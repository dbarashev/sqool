package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.databind.node.ObjectNode
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.math.BigDecimal

class TeacherScoringFlow(tg: ChainBuilder) {
    init {
        if (tg.isTeacher) {
            tg.onCallback { json ->
                when (json["p"]?.asInt() ?: 0) {
                    ACTION_SCORE_STUDENTS -> {
                        studentList(tg, json)
                    }
                }
            }
            tg.onRegexp(""".*""", whenState = 2) {
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
                            try {
                                val json = OBJECT_MAPPER.readTree(data)
                                val studentId = json["m"]?.asInt() ?: return@let
                                val sprintNum = json["s"]?.asInt() ?: return@let
                                writeTeacherScore(studentId, sprintNum, score)
                                teacherLanding(tg)
                            } finally {
                                txn {
                                    dialogState(tg.userId, null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun writeTeacherScore(studentId: Int, sprintNum: Int, score: Double) {
        txn {
            insertInto(table("TeacherScores"),
                field("student_id", Int::class.java),
                field("sprint_num", Int::class.java),
                field("score", BigDecimal::class.java)
            ).values(
                studentId, sprintNum, BigDecimal.valueOf(score)
            ).onConflict(
                field("student_id"), field("sprint_num")
            ).doUpdate().set(field("score", BigDecimal::class.java), BigDecimal.valueOf(score)).execute();
        }
    }

    private fun studentList(tg: ChainBuilder, json: ObjectNode) {
        val uni = json["u"]?.asInt() ?: run {
            tg.reply("Ошибка состояния: не найден университет", isMarkdown = false, stop = true)
            return
        }

        val lastSprint = lastSprint(uni) ?: run {
            tg.reply("Не получилось узнать номер последней итерации", isMarkdown = false, stop = true)
            return
        }
        val teamRecords = getAllSprintTeamRecords(uni, lastSprint).sortedBy { it.displayName.lastNameFirst() }
        val student = json["m"]?.asInt()?.let { studentId -> teamRecords.first { it.id == studentId } }
        if (student != null) {
            tg.reply("Поставьте оценку для ${student.displayName} по вещественной шкале 0..10", isMarkdown = false)
            db {
                dialogState(tg.userId, 2, """ {"m": ${student.id}, "s": $lastSprint } """)
            }
        } else {
            val btns = teamRecords.map {
                println(it)
                BtnData(
                    it.displayName,
                    """{"p": $ACTION_SCORE_STUDENTS, "m": ${it.id}, "u": $uni } """
                )
            }
            tg.reply("Выберите студента", buttons = btns, isMarkdown = false, stop = false, maxCols = 1)
        }
    }
}

private fun String.lastNameFirst(): String {
    val (first, last) = this.split("\\s+".toRegex(), limit = 2)
    return "$last $first"
}