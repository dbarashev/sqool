package com.bardsoftware.sqool.bot

import UniContext
import com.bardsoftware.libbotanique.*
import com.bardsoftware.sqool.bot.db.tables.references.TEACHERSCORES
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.*
import execute
import initContext
import whenLanding
import whenTeacher
import withSprint
import withUniversity
import java.math.BigDecimal

class TeacherCommands(tg: ChainBuilder) {
  init {
    tg.initContext().whenTeacher().andThen {
      println("TEACHER: ${tg.userName}")

      it.whenLanding {
        tg.onCommand("/start", "/help") {
          teacherLanding(tg)
        }
      }
      it.withUniversity().map { ctx ->
        println("UNI=${ctx.value}")
        ctx.flow.json.getAction().map { action ->
          println("action=$action")
          when (action) {
            ACTION_TEACHER_LANDING -> teacherPageChooseAction(tg, ctx.flow.json)
            ACTION_ROTATE_TEAMS -> teacherPageRotateProjects(tg, ctx.flow.json)
            //ACTION_GREET_STUDENT -> studentRegister(tg, json)
            ACTION_FINISH_ITERATION -> teacherPageFinishIteration(tg, ctx.flow.json)
            ACTION_PRINT_TEAMS -> getAllCurrentTeamRecords(ctx.value).print(tg)
            else -> actionsWithSprint(ctx, action)
          }
        }
        tg.stop()
      }
      tg.onInput(whenState = 2) {
        val score = it.toDoubleOrNull()
        when {
          score == null -> {
            tg.reply("Оценка должна быть вещественным числом")
          }

          score < 0 || score > 10 -> {
            tg.reply("Оценка должна быть в диапазоне [0..10]")
          }

          else -> {
            tg.userSession.state?.asJson()?.let { json ->
              try {
                val studentId = json["m"]?.asInt() ?: return@let
                val sprintNum = json["s"]?.asInt() ?: return@let
                writeTeacherScore(studentId, sprintNum, score)
                teacherLanding(tg)
              } finally {
                txn {
                  tg.userSession.reset()
                }
              }
            }
          }
        }
      }
      Ok(it)
    }
  }

  private fun writeTeacherScore(studentId: Int, sprintNum: Int, score: Double) {
    txn {
      insertInto(
        TEACHERSCORES,
        TEACHERSCORES.STUDENT_ID,
        TEACHERSCORES.SPRINT_NUM,
        TEACHERSCORES.SCORE
      ).values(
        studentId, sprintNum, BigDecimal.valueOf(score)
      ).onConflict(
        TEACHERSCORES.STUDENT_ID, TEACHERSCORES.SPRINT_NUM
      ).doUpdate().set(TEACHERSCORES.SCORE, BigDecimal.valueOf(score))
        .execute();
    }
  }
}

internal fun teacherLanding(tg: ChainBuilder) {
  tg.reply("Выберите вуз", buttons = listOf(
    BtnData("CUB", """ {"u": 0, "p": $ACTION_TEACHER_LANDING} """)
  ))
  tg.stop()
}

private fun teacherPageChooseAction(tg: ChainBuilder, json: ObjectNode) {
  val uni = json.getUniversity().onFailure {
    tg.reply(it, isMarkdown = false, stop = true)
  }.unwrap()

  tg.reply(
    "Чего изволите?", isMarkdown = false, stop = true, buttons = listOf(
      BtnData("Напечатать команды", """ {"u": $uni, "p": $ACTION_PRINT_TEAMS} """),
      BtnData("Поставить оценки", """{"u": $uni, "p": $ACTION_SCORE_STUDENTS}"""),
      BtnData("Посмотреть ведомость", """ {"u": $uni, "p": $ACTION_PRINT_PEER_REVIEW_SCORES } """),
      BtnData("Завершить итерацию", """ {"u": $uni, "p": $ACTION_FINISH_ITERATION} """),
      BtnData("Сделать ротацию в проектах", """ {"u": $uni, "p": $ACTION_ROTATE_TEAMS } """),
    ), maxCols = 1
  )
}

private fun actionsWithSprint(uniCtx: UniContext, action: Int) {
  uniCtx.withSprint().execute { tg, json, uni, sprintNum ->
    when (action) {
      ACTION_SCORE_STUDENTS -> {
        studentList(tg, uni, sprintNum, json)
        return@execute
      }

      ACTION_PRINT_PEER_REVIEW_SCORES -> {
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

      ACTION_FIX_REVIEW_SCORES -> {
        printSprintScores(tg, uni, sprintNum, true)
        tg.reply("Done")
        teacherLanding(tg)
      }
    }
  }
}

private fun studentList(tg: ChainBuilder, uni: Int, sprintNum: Int, json: ObjectNode) {
  val teamRecords = getAllSprintTeamRecords(uni, sprintNum).sortedBy { it.displayName.lastNameFirst() }
  json.getStudent().andThen { studentId ->
    teamRecords.firstOrNull { it.id == studentId }?.let { Ok(it) }
      ?: Err("Student with id=$studentId not found in the team records")
  }.mapError {
    val btns = teamRecords.map {
      println(it)
      BtnData(
        it.displayName,
        """{"p": $ACTION_SCORE_STUDENTS, "m": ${it.id}, "u": $uni, "s": $sprintNum } """
      )
    }
    tg.reply("Выберите студента", buttons = btns, maxCols = 1)
  }.map { student ->
    tg.reply("Поставьте оценку для ${student.displayName} по вещественной шкале 0..10")
    db {
      tg.userSession.save( 2, """ {"m": ${student.id}, "s": $sprintNum } """)
    }
  }
}

private fun teacherPageFinishIteration(tg: ChainBuilder, json: ObjectNode) {
  val uni = json.getUniversity().onFailure {
    tg.reply(it, isMarkdown = false, stop = true)
  }.unwrap()
  val newIteration = finishIteration(uni)
  if (newIteration == -1) {
    tg.reply("Done")
  } else {
    tg.reply("Итерация завершена, новая итерация №$newIteration. Не забудь сделать ротацию.", isMarkdown = false, stop=true)
  }
}


private fun teacherPageRotateProjects(tg: ChainBuilder, json: ObjectNode) {
  val uni = json.getUniversity().onFailure {
    tg.reply(it, isMarkdown = false, stop = true)
  }.unwrap()

  tg.reply("Произведём ротацию в университете $uni", isMarkdown = false, stop = true)
  val actualMembers = lastSprint(uni)?.let {
    getAllSprintTeamRecords(uni, it)
  } ?: listOf()
  val newTeamRecords = if (actualMembers.isEmpty()) {
    initialTeams().also {
      println("Initial teams: $it")
    }
  } else {
    rotateTeams(actualMembers).also {
      println("Rotated teams: $it")
    }
  }
  updateRepositoryPermissions(newTeamRecords, actualMembers)
  insertNewRotation(newTeamRecords)
  newTeamRecords.print(tg)
}

private fun String.lastNameFirst(): String {
  val (first, last) = this.split("\\s+".toRegex(), limit = 2)
  return "$last $first"
}