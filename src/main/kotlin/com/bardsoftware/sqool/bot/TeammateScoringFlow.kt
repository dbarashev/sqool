/*
 * Copyright (c) BarD Software s.r.o 2022
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.math.BigDecimal

const val DIALOG_STARTED = 0

object ReviewerDialog {
    const val DIALOG_STARTED = 0
    const val DIALOG_EXPECT_REPLY_HAS_PR = 1
    const val DIALOG_EXPECT_REPLY_CODE_QUALITY = 2
    const val DIALOG_EXPECT_REPLY_REVIEW_PROGRESS = 3
    val ANSWERS_HAS_PR = listOf(
        "Нет, пулреквеста не было",
        "Пулреквест был, но с серьезным нарушением дедлайна",
        "Да. пулреквест пришел вовремя"
    )
    val ANSWERS_CODE_QUALITY = listOf(
        "Всё отлично, никаких замечаний",
        "Были замечания, не относившиеся к сути решавшейся задачи",
        "Небольшое количество замечаний по сути решавшейся задачи",
        "Было существенное количество косяков, но всё рашаемо",
        "Этот код нужно было переписать заново"
    )
}
object CoderDialog {
    const val DIALOG_STARTED = 0
    const val EXPECT_REPLY_HAS_PR = 1
    const val EXPECT_REPLY_HAS_REVIEW = 2
    val ANSWERS_HAS_REVIEW = listOf(
        "Нет, рецензии не было",
        "Рецензия была, но очень поздно, и отреагировать не успел",
        "Рецензия была вовремя, всё успели исправить"
    )
    const val EXPECT_REPLY_REVIEW_QUALITY = 3
    val ANSWERS_REVIEW_QUALITY = listOf(
        "Формальная рецензия без комментариев",
        "Мелкие замечания к форматированию",
        "Небольшие замечания по делу, несущественно улучшившие код",
        "Хорошее ревью, очень сильно улучшило код",
    )
}
private val TEAMMMATE_CODER = setOf(2, 3)
private val TEAMMMATE_REVIEWER = setOf(1)


internal data class TeammateScoringState(
    val tgUsernameFrom: String,
    val idTo: Int,
    val sprintNum: Int,
    var dialogStep: Int,
    val currentScore: Int = 0
) {
    lateinit var studentFrom: TeamMember
    lateinit var studentTo: TeamMember

    fun load() {
        db {
            val info = select(
                field("tg_username", String::class.java),
                field("id", Int::class.java),
                field("name", String::class.java),
                field("ord", Int::class.java)
            ).from(table("TeamDetails"))
                .where(field("sprint_num").eq(sprintNum))
                .and(field("tg_username", String::class.java).eq(tgUsernameFrom).or(field("id", Int::class.java).eq(idTo)))
                .toList()
            studentFrom = info.first { it.component1() == tgUsernameFrom }.let {
                TeamMember(-1, tgUsernameFrom, it.component4(), it.component3())
            }
            studentTo = info.first { it.component2() == idTo }.let {
                TeamMember(-1, it.component1(), it.component4(), it.component3())
            }
        }
    }
    val name: String get() = studentTo.displayName

    fun clearScores() {
        db {
            deleteFrom(table("ScoreDetails"))
                .where(field("tg_username_from").eq(studentFrom.tgUsername))
                .and(field("tg_username_to").eq(studentTo.tgUsername))
                .and(field("sprint_num").eq(sprintNum))
                .execute()
        }
    }

    fun buttonsYesNo() =
        listOf(
            BtnData("Да", """ ${TeammateScoringState(tgUsernameFrom, idTo, sprintNum, dialogStep+1, 1).toJsonString()} """),
            BtnData("Нет", """ ${TeammateScoringState(tgUsernameFrom, idTo, sprintNum, dialogStep+1, 0).toJsonString()} """)
        )

    fun buttonsScore(questions: List<String>) =
        questions.mapIndexed { idx, q ->
            BtnData(q, """ ${TeammateScoringState(tgUsernameFrom, idTo, sprintNum, dialogStep+1, idx).toJsonString()}} """)
        }.toList()

    fun toJsonString() =
        """{"p":$ACTION_SCORE_TEAMMATE,"s":$dialogStep,"m":$idTo,"i":$sprintNum,"a":$currentScore }""".also { println(it)}

    fun writeScores() {
        writeScore(dialogStep, BigDecimal.valueOf(currentScore.toLong()))
    }

    fun writeScore(idx: Int, score: BigDecimal) {
        txn {
            insertInto(table("ScoreDetails"),
                field("tg_username_from", String::class.java),
                field("tg_username_to", String::class.java),
                field("sprint_num", Int::class.java),
                field("scoring_pos", Int::class.java),
                field("score", BigDecimal::class.java)).values(tgUsernameFrom, studentTo.tgUsername, sprintNum, idx, score)
                .onConflict(field("tg_username_from", String::class.java),
                    field("tg_username_to", String::class.java),
                    field("sprint_num", Int::class.java),
                    field("scoring_pos", Int::class.java))
                .doUpdate()
                .set(field("score", BigDecimal::class.java), score).execute()
        }
    }

    companion object {
        fun fromJsonString(tgUsernameFrom: String, json: ObjectNode): TeammateScoringState {
            if (json["p"].asInt() != ACTION_SCORE_TEAMMATE) {
                throw IllegalStateException("Unexpected callback object")
            }
            val sprintNum = json["i"]?.asInt() ?: throw IllegalStateException("Sprint number not found in 's' field: $json")
            val mateId = json["m"]?.asInt() ?: throw IllegalStateException("Team mate id not found in 'm' field: $json")
            val step = json["s"]?.asInt() ?: 0
            val currentScore = json["a"]?.asInt() ?: 0
            return TeammateScoringState(tgUsernameFrom, mateId, sprintNum, step, currentScore).also {
                it.load()
            }
        }

    }
}

class TeammateScoringFlow(tg: ChainBuilder) {
    init {
        tg.onCallback { json ->
            when (json["p"]?.asInt() ?: 0) {
                ACTION_SCORE_TEAMMATE -> {
                    val state = TeammateScoringState.fromJsonString(tg.userName, json)
                    if (state.studentTo.ord in TEAMMMATE_CODER) {
                        return@onCallback reviewerFlow(tg, state)
                    }
                    if (state.studentTo.ord in TEAMMMATE_REVIEWER) {
                        return@onCallback coderFlow(tg, state)
                    }
                }
                else -> return@onCallback
            }
        }
        tg.onRegexp(""".*""", whenState = 1) {
            val score = it.value.toDoubleOrNull()
            when {
                score == null -> {
                    tg.reply("Оценка должна быть вещественным числом", isMarkdown = false, stop = true)
                }
                score < 0 || score > 10 -> {
                    tg.reply("Оценка должна быть в диапазоне [0..10]", isMarkdown = false, stop = true)
                }
                else -> {
                    tg.fromUser?.getDialogState()?.data?.let { data ->
                        val state = TeammateScoringState.fromJsonString(tg.userName, OBJECT_MAPPER.readTree(data) as ObjectNode)
                        state.writeScore(state.dialogStep, BigDecimal.valueOf(score))
                        txn {
                            dialogState(tg.userId, null)
                        }
                        tg.reply("Вы поставили  $score товарищу ${state.studentTo.displayName}", isMarkdown = false)
                        studentLandingMenu(tg, getStudent(tg.userName)!!)
                    }
                }
            }
        }
    }

    private fun coderFlow(tg: ChainBuilder, state: TeammateScoringState) {
        when (state.dialogStep) {
            CoderDialog.DIALOG_STARTED -> {
                state.clearScores()
                tg.reply(
                    "Вы послали pull request ${state.name}?", buttons = state.buttonsYesNo(),
                    isMarkdown = false, stop = true
                )
                return
            }
            CoderDialog.EXPECT_REPLY_HAS_PR -> {
                state.writeScores()
                if (state.currentScore == 0) {
                    tg.reply("Жаль! Больше вопросов нет.", isMarkdown = false, stop = true)
                } else {
                    tg.reply("Вы получили рецензию от  ${state.name}?", buttons = state.buttonsScore(CoderDialog.ANSWERS_HAS_REVIEW),
                        isMarkdown = false, stop = true, maxCols = 1)
                }
                return
            }
            CoderDialog.EXPECT_REPLY_HAS_REVIEW -> {
                state.writeScores()
                if (state.currentScore == 0) {
                    tg.reply("Жаль! Больше вопросов нет.", isMarkdown = false, stop = true)
                } else {
                    tg.reply("Что скажете о качестве рецензии?", buttons = state.buttonsScore(CoderDialog.ANSWERS_REVIEW_QUALITY),
                        maxCols = 1, isMarkdown = false, stop = true)
                }
                return
            }
            CoderDialog.EXPECT_REPLY_REVIEW_QUALITY -> {
                state.writeScores()
                tg.reply("Ok. Общее впечатление от работы ${state.studentTo.displayName} по вещественной шкале 0..10?", isMarkdown = false)
                db {
                    state.dialogStep += 1
                    dialogState(tg.userId, 1, "${state.toJsonString()}")
                }
            }
        }
    }
    private fun reviewerFlow(tg: ChainBuilder, state: TeammateScoringState) {
        when (state.dialogStep) {
            DIALOG_STARTED -> {
                state.clearScores()
                tg.reply("Вы получили pull request от ${state.name}?", buttons = state.buttonsScore(ReviewerDialog.ANSWERS_HAS_PR),
                    isMarkdown = false, stop = true, maxCols = 1)
                return
            }
            ReviewerDialog.DIALOG_EXPECT_REPLY_HAS_PR -> {
                state.writeScores()
                when (state.currentScore) {
                    0 -> {
                        tg.reply("Жаль! Больше вопросов нет.", isMarkdown = false, stop = true)
                        return
                    }
                    1 -> {
                        tg.reply("Ай-ай! Надеюсь, вы успели сделать code review", isMarkdown = false, stop = true)
                    }
                }
                tg.reply("Что скажете о коде?", buttons = state.buttonsScore(ReviewerDialog.ANSWERS_CODE_QUALITY), isMarkdown = false, stop = true, maxCols = 1)
                return
            }
            ReviewerDialog.DIALOG_EXPECT_REPLY_CODE_QUALITY -> {
                state.writeScores()
                tg.reply("Была ли реакция на комментарии от ${state.studentTo.displayName}, вносились ли исправления?",
                    buttons = state.buttonsYesNo(), isMarkdown = false, stop = true)
                return
            }
            ReviewerDialog.DIALOG_EXPECT_REPLY_REVIEW_PROGRESS -> {
                state.writeScores()
                tg.reply("Ok. Общее впечатление от работы ${state.studentTo.displayName} по вещественной шкале 0..10?", isMarkdown = false)
                db {
                    state.dialogStep += 1
                    dialogState(tg.userId, 1, "${state.toJsonString()}")
                }
            }
        }
    }
}

