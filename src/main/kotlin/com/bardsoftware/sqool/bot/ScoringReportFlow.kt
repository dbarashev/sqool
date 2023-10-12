package com.bardsoftware.sqool.bot

import com.bardsoftware.sqool.bot.db.tables.references.STUDENT
import com.bardsoftware.sqool.bot.db.tables.references.TEACHERSCORES
import com.bardsoftware.sqool.bot.db.tables.references.TEAMANDPEERSCORES
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

enum class ScoringPos {
    CODER_SENT_PR, // 0
    CODE_QUALITY,  // 1
    RESPONSE_TO_REVIEW_NO_YES, //2
    CODER_RATING,  // 3
    UNUSED, // 4
    YOU_SENT_PR, //5
    YOU_RECEIVED_REVIEW, // 6
    REVIEW_QUALITY, // 7
    REVIEWER_RATING // 8
}
private data class Scores(var ord: Int = 0, val scores: MutableList<BigDecimal> = mutableListOf()) {
    // Here we score the coder's work
    private val coderSummary: Double
        get() {
            var sum = 0.0
            // If there was a PR
            when (scores[ScoringPos.CODER_SENT_PR.ordinal].toLong()) {
                0L -> return 0.0
                1L -> sum += 0.75
                2L -> sum += 0.9
            }
            // Code quality
            when (scores[ScoringPos.CODE_QUALITY.ordinal].toLong()) {
                0L -> sum += 0.1
                1L -> sum = sum
                2L -> sum -= 0.1
                3L -> sum -= 0.25
                4L -> sum -= 0.4
            }
            // Response to review
            when (scores[ScoringPos.RESPONSE_TO_REVIEW_NO_YES.ordinal].toLong()) {
                0L -> sum -= scores[1].toDouble() * 0.5 // no response
                1L -> sum += 0.025 * scores[1].toDouble() // yes, responded and fixed
            }
            if (sum > 1.0) {
                println(scores)
            }
            sum = max(sum, 1.0)
            return (10*sum + scores[ScoringPos.CODER_RATING.ordinal].toDouble())/2
        }
    // Here we score the reviewer work
    private val reviewerSummary: Double
        get() {
            var sum = 0.0
            if (scores[ScoringPos.YOU_SENT_PR.ordinal].toLong() == 0L) {
                return -1.0
            }
            when (scores[ScoringPos.YOU_RECEIVED_REVIEW.ordinal].toLong()) {
                0L -> return 0.0
                1L -> sum += 0.6
                2L -> sum += 0.75
            }
            when (scores[ScoringPos.REVIEW_QUALITY.ordinal].toLong()) {
                0L -> sum -= 0.05
                1L -> sum -= 0.0
                2L -> sum += 0.05
                3L -> sum += 0.25
            }
            sum = max(sum, 1.0)
            return (sum*10 + scores[ScoringPos.REVIEWER_RATING.ordinal].toDouble())/2
        }
    val summary: Double
        get() =
            when (reviewerSummary) {
                -1.0 -> coderSummary
                else -> (coderSummary + reviewerSummary)/2.0
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
            TEAMANDPEERSCORES.NAME,
            TEAMANDPEERSCORES.ORD,
            TEAMANDPEERSCORES.SCORING_POS,
            TEAMANDPEERSCORES.SCORE,
            TEAMANDPEERSCORES.ID
        ).from(TEAMANDPEERSCORES).where(
            TEAMANDPEERSCORES.SPRINT_NUM.eq(sprintNum)
        ).orderBy(
            TEAMANDPEERSCORES.TEAM_NUM, TEAMANDPEERSCORES.ORD, TEAMANDPEERSCORES.SCORING_POS
        ).toList()

        val mapUserIdScores = mutableMapOf<Int, Scores>()
        var currentUserId = -1
        var currentScores = Scores()

        rawRecords.forEach {
            if (it[TEAMANDPEERSCORES.ID] != currentUserId) {
                mapUserIdScores[currentUserId] = currentScores
                currentUserId = it[TEAMANDPEERSCORES.ID]!!
                currentScores = Scores()
                currentScores.scores.addAll(List(9) { BigDecimal.valueOf(0) })
            }
            currentScores.ord = it[TEAMANDPEERSCORES.ORD]!!
            currentScores.scores[it[TEAMANDPEERSCORES.SCORING_POS]!! - 1] = it[TEAMANDPEERSCORES.SCORE]!!.toBigDecimal()
            currentUserId = it[TEAMANDPEERSCORES.ID]!!
        }
        mapUserIdScores[currentUserId] = currentScores

        val reply = StringBuilder()

        select(
            STUDENT.ID,
            //field("student_id", Int::class.java),
            coalesce(TEACHERSCORES.SCORE, BigDecimal.valueOf(-1.0)),
            STUDENT.NAME
        ).from(STUDENT.leftJoin(TEACHERSCORES).on(STUDENT.ID.eq(TEACHERSCORES.STUDENT_ID)))
            .where(TEACHERSCORES.SPRINT_NUM.eq(sprintNum))
            .forEach { teacherRecord ->
                val sumScores = mutableListOf<Double>()

                print("${teacherRecord[STUDENT.NAME]}: ")
                mapUserIdScores[teacherRecord[STUDENT.ID]!!]?.let {
                    print("peers: ${it.summary}, ")
                    sumScores.add(it.summary)
                }
                if (teacherRecord.component2()!!.toDouble() != -1.0) {
                    print("teachers: ${teacherRecord.component2()}, ")
                    sumScores.add(teacherRecord.component2()!!.toDouble())
                }

                val result = sumScores.average().round(2)
                idToResultScore[teacherRecord[STUDENT.ID]!!] = result
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