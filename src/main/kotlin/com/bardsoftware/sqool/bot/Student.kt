package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.math.BigDecimal

fun studentRegister(tg: ChainBuilder, json: ObjectNode) {
    val answer = json["a"]?.asInt() ?: 0
    if (answer == 1) {
        insertStudent(tg.userName, tg.fromUser?.displayName() ?: tg.userName, tg.userId)
        tg.reply("Окей, мы теперь с вами знакомы.", isMarkdown = false, stop = true)
    } else {
        tg.reply("Ну штош, бывает.", isMarkdown = false, stop = true)
    }
}

fun getSumScore(tgUsername: String): Pair<BigDecimal, BigDecimal> =
    db {
        select(field("sum_score", BigDecimal::class.java), field("scored_sprints", Long::class.java))
            .from(table("ScoreSummary"))
            .where(field("tg_username", String::class.java).eq(tgUsername))
            .firstOrNull()?.let {
                it.component1() to BigDecimal.valueOf(it.component2())
            } ?: (BigDecimal.ZERO to BigDecimal.ZERO)
    }

fun getScoreList(tgUsername: String): List<BigDecimal> =
    db {
        select(field("score", BigDecimal::class.java))
            .from(table("Score").join(table("Student")).on("id=student_id"))
            .where(field("tg_username", String::class.java).eq(tgUsername))
            .orderBy(field("sprint_num"))
            .map {
                it.component1()
            }
    }
