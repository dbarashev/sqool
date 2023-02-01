package com.bardsoftware.sqool.bot

import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.onSuccess
import org.jooq.impl.DSL.*
import java.math.BigDecimal
import kotlin.math.max

class ScoringReportFlow(tg: ChainBuilder) {
    init {
        if (tg.isTeacher) {
            tg.onCallback { json ->
                when (json["p"]?.asInt() ?: 0) {
                    ACTION_PRINT_PEER_REVIEW_SCORES -> {
                        tg.withFlow(json, ACTION_PRINT_PEER_REVIEW_SCORES).withUniversity().andThen {
                            it.withSprint()
                        }.onSuccess {
                            it.execute { tg, json, uni, sprintNum ->
                                if (sprintNum == -1) {
                                    printAllScores(tg, uni)
                                } else {
                                    printSprintScores(tg, uni, sprintNum, false)
                                    tg.reply(
                                        "Зафиксировать оценки? Это сделает их видимыми для студентов", buttons = listOf(
                                            BtnData(
                                                "Да",
                                                """{"p": $ACTION_FIX_REVIEW_SCORES, "u": $uni, "s": $sprintNum}"""
                                            ),
                                            BtnData("Нет", """{"p": $ACTION_TEACHER_LANDING, "u": $uni}""")
                                        )
                                    )
                                }
                            }
                        }
                    }
                    ACTION_FIX_REVIEW_SCORES -> {
                        tg.withFlow(json, ACTION_FIX_REVIEW_SCORES).withUniversity().andThen {
                            it.withSprint()
                        }.onSuccess {
                            it.execute { tg, json, uni, sprintNum ->
                                printSprintScores(tg, uni, sprintNum, true)
                            }
                            tg.reply("Done")
                            teacherLanding(tg)
                        }
                    }
                }
            }
        }
    }
}


private data class Scores(var ord: Int = 0, val scores: MutableList<BigDecimal> = mutableListOf()) {
    private val coderSummary: Double
        get() {
            var sum = 0.0
            when (scores[0].toLong()) {
                0L -> return 0.0
                1L -> sum += 0.75
                2L -> sum += 0.9
            }
            when (scores[1].toLong()) {
                0L -> sum += 0.1
                1L -> sum = sum
                2L -> sum -= 0.1
                3L -> sum -= 0.25
                4L -> sum -= 0.4
            }
            when (scores[2].toLong()) {
                0L -> sum -= scores[1].toDouble() * 0.5
                1L -> sum += 0.025 * scores[1].toDouble()
            }
            if (sum > 1.0) {
                println(scores)
            }
            sum = max(sum, 1.0)
            return (10*sum + scores[3].toDouble())/2
        }
    private val reviewerSummary: Double
        get() {
            var sum = 0.0
            if (scores[0].toLong() == 0L) {
                return -1.0
            }
            when (scores[1].toLong()) {
                0L -> return 0.0
                1L -> sum += 0.6
                2L -> sum += 0.75
            }
            when (scores[2].toLong()) {
                0L -> sum -= 0.05
                1L -> sum -= 0.0
                2L -> sum += 0.05
                3L -> sum += 0.25
            }
            return (sum*10 + scores[3].toDouble())/2
        }
    val summary: Double
        get() =
            when (ord) {
                1 -> reviewerSummary
                2,3 -> coderSummary
                else -> -1.0
            }

}

fun printAllScores(tg: ChainBuilder, uni: Int) {
    val scoredSprints = mutableMapOf<String, Int>()
    val nameSprintScore  = db {
        select(
            field("Student.name", String::class.java),
            field("sprint_num", Int::class.java),
            field("score", BigDecimal::class.java),
            field("scored_sprints", Int::class.java)
        ).from(table("Student").leftJoin(table("Score")).on("id=student_id").leftJoin("ScoreSummary").using(field("id")))
            .orderBy(
                field("Student.name", String::class.java),
                field("sprint_num", Int::class.java),

            )
            .map {
                scoredSprints[it.component1()] = it.component4()
                it
            }
            .associate { (it.component1() to it.component2()) to it.component3()  }
    }

    val lastSprint = lastSprint(uni) ?: 0
    val names = nameSprintScore.keys.map { it.first }.toSet()
    val result = StringBuilder()
    names.forEach {name ->
        result.append(name.escapeMarkdown()).append(',').append(scoredSprints[name]).append(',')
        (1..lastSprint).forEach { sprint ->
            result.append(nameSprintScore[name to sprint]?.toString() ?: "").append(',')
        }
        result.append('\n')
    }
    tg.reply("""
        ```
        $result
        ```
    """.trimIndent())

}
fun printSprintScores(tg: ChainBuilder, uni: Int, sprintNum: Int, writeSummaryScores: Boolean = false) {
    val idToResultScore = mutableMapOf<Int, Double>()

    db {
        val rawRecords = select(
            field("name", String::class.java),
            field("ord", Int::class.java),
            field("scoring_pos", Int::class.java),
            field("score", BigDecimal::class.java),
            field("id", Int::class.java)
        ).from(table("TeamAndPeerScores")).where(
            field("sprint_num", Int::class.java).eq(sprintNum)
        ).orderBy(
            field("team_num"), field("ord"), field("scoring_pos")
        ).toList()

        val mapUserIdScores = mutableMapOf<Int, Scores>()
        var currentUserId = -1
        var currentScores = Scores()

        rawRecords.forEach {
            if (it.component5() != currentUserId) {
                mapUserIdScores[currentUserId] = currentScores
                currentUserId = it.component5()
                currentScores = Scores()
                currentScores.scores.addAll(List(4) { BigDecimal.valueOf(0) })
            }
            currentScores.ord = it.component2()
            currentScores.scores[it.component3() - 1] = it.component4()
            currentUserId = it.component5()
        }
        mapUserIdScores[currentUserId] = currentScores

        val reply = StringBuilder()

        select(
            field("student_id", Int::class.java),
            coalesce(field("score", BigDecimal::class.java), BigDecimal.valueOf(-1.0)),
            field("name", String::class.java)
        ).from(table("Student").leftJoin(table("TeacherScores")).on("id = student_id"))
            .where(field("sprint_num", Int::class.java).eq(sprintNum))
            .forEach { teacherRecord ->
                val sumScores = mutableListOf<Double>()

                print("${teacherRecord.component3()}: ")
                mapUserIdScores[teacherRecord.component1()]?.let {
                    print("peers: ${it.summary}, ")
                    sumScores.add(it.summary)
                }
                if (teacherRecord.component2().toDouble() != -1.0) {
                    print("teachers: ${teacherRecord.component2()}, ")
                    sumScores.add(teacherRecord.component2().toDouble())
                }

                val result = sumScores.average().round(2)
                idToResultScore[teacherRecord.component1()] = result
                println("result=$result")
                reply.append("${teacherRecord.component3()}\t\t${result}\n".escapeMarkdown())
            }

        tg.reply("""
            ```
            ${reply}
            ```
        """.trimIndent(), isMarkdown = true, stop = false)

    }

    if (writeSummaryScores) {
        txn {
            idToResultScore.forEach { (id, score) ->
                if (!score.isNaN()) {
                    insertInto(
                        table("Score"),
                        field("student_id", Int::class.java),
                        field("sprint_num", Int::class.java),
                        field("score", BigDecimal::class.java)
                    ).values(id, sprintNum, BigDecimal.valueOf(score))
                        .onConflict(field("student_id"), field("sprint_num"))
                        .doUpdate().set(field("score", BigDecimal::class.java), BigDecimal.valueOf(score))
                        .execute()
                }
            }
        }
    }
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}