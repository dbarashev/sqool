package com.bardsoftware.sqool.bot

import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.math.BigDecimal
import kotlin.math.max

class ScoringReportFlow(tg: ChainBuilder) {
    init {
        if (tg.isTeacher) {
            tg.onCommand("scores") {
                printLastSprintScores(tg, 0)
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

fun printLastSprintScores(tg: ChainBuilder, uni: Int) {
    db {
        val lastSprint = lastSprint(uni)
        val rawRecords = select(
            field("name", String::class.java),
            field("ord", Int::class.java),
            field("scoring_pos", Int::class.java),
            field("score", BigDecimal::class.java)
        ).from(table("TeamAndPeerScores")).where(
            field("sprint_num", Int::class.java).eq(lastSprint)
        ).orderBy(
            field("team_num"), field("ord"), field("scoring_pos")
        ).toList()

        val mapUsernameScores = mutableMapOf<String, Scores>()
        var currentUsername = ""
        var currentScores = Scores()

        rawRecords.forEach {
            if (it.component1() != currentUsername) {
                mapUsernameScores[currentUsername] = currentScores
                currentUsername = it.component1()
                currentScores = Scores()
                currentScores.scores.addAll(List(4) { BigDecimal.valueOf(0) })
            }
            currentScores.ord = it.component2()
            currentScores.scores[it.component3() - 1] = it.component4()
            currentUsername = it.component1()
        }
        mapUsernameScores[currentUsername] = currentScores

        val reply = StringBuilder()
        mapUsernameScores.forEach { username, scores ->
            reply.append("$username\t\t${scores.summary}\n".escapeMarkdown())
        }
        tg.reply("""
            ```
            ${reply}
            ```
        """.trimIndent(), isMarkdown = true, stop = false)

    }
}