package com.bardsoftware.sqool.bot

import SprintContext
import StudentContext
import UniContext
import com.bardsoftware.libbotanique.*
import com.bardsoftware.sqool.bot.db.tables.references.TEACHERREVIEW
import com.bardsoftware.sqool.bot.db.tables.references.TEACHERSCORES
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.*
import initContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import whenTeacher
import withSprint
import withStudent
import withUniversity
import java.math.BigDecimal

class TeacherCommands(tg: ChainBuilder) {
  init {
    tg.initContext().whenTeacher().andThen {
      println("TEACHER: ${tg.userName}")
      if (it.action == -1) {
        teacherLanding(tg)
        Err(0)
      }
      else {
        it.withUniversity()
      }
    }.andThen {
      teacherWithUniversity(it, tg)
    }.andThen {
      teacherWithSprint(it, tg)
    }.andThen {
      teacherWithStudent(it, tg)
    }
  }

  private fun teacherWithStudent(it: StudentContext, tg: ChainBuilder): Result<StudentContext, Any> {
    val studentId = it.value
    val sprint = it.sprint.value
    val uni = it.sprint.uni.value
    val action = it.sprint.uni.flow.action
    val json = it.sprint.uni.flow.json
    println("Student: $studentId")

    tg.messageText.let {
      if (it.isNotBlank()) {
        when (action) {
          ACTION_SCORE_STUDENTS -> {
            it.toDoubleOrNull()?.let {
              if (it < 0 || it > 10.0) {
                tg.reply("Оценка должна быть в диапазоне [0..10]")
              } else {
                try {
                  writeTeacherScore(studentId, sprint, it)
                  tg.reply("Оценка записана!")
                  teacherLanding(tg)
                } finally {
                  txn {
                    tg.userSession.reset()
                  }
                }
              }
              Err(0)
            } ?: run {
              tg.reply("Оценка должна быть вещественным числом")
              Err(0)
            }
          }

          ACTION_REVIEW_STUDENTS -> {
            try {
              writeTeacherReview(tg.userId, studentId, sprint, it)
              tg.reply("Отзыв записан!")
              teacherLanding(tg)
            } finally {
              txn {
                tg.userSession.reset()
              }
            }
            Err(0)
          }

          else -> Err(0)
        }
      } else {
        val teamRecords = getAllSprintTeamRecords(uni, sprint)
        val student = teamRecords.first { it.id == studentId }
        when (action) {
          ACTION_SCORE_STUDENTS -> {
            tg.reply(
              """Поставьте оценку для ${student.displayName} по вещественной шкале 0..10
            | Оценка 8..10 означает, что недочёты были мелкие и легкоустранимые.
            | Оценка 6..8 означает, что были серьезные недочёты, устранение которых потребует существенных изменений.
            | Оценка 4..6 означает, что задание в принципе было выполнено неполностью, выполнено не то, что требовалось, и так далее.
            """.trimMargin()
            )
            tg.userSession.save(ACTION_SCORE_STUDENTS, json.toString())
          }

          ACTION_REVIEW_STUDENTS -> {
            tg.reply(
              """Напишите текстовый отзыв для ${student.displayName}.  
            | В отзыве хочется видеть: 
            | - какие именно недостатки по существу решаемой задачи были обнаружены
            | - имелась ли рецензия от товарища по команде и была ли она полезной с точки зрения решаемой задачи
            | - реагировал ли студент на рецензию товарища
            | """.trimMargin()
            )
            getReviews(studentId, sprint).forEach {
              tg.reply(it)
            }
            tg.userSession.save(ACTION_REVIEW_STUDENTS, json.toString())
          }
        }
        Err(0)
      }
    }
    return Ok(it)
  }

  private fun teacherWithSprint(it: SprintContext, tg: ChainBuilder): Result<StudentContext, Any> {
    println("Sprint: ${it.value}")
    val sprint = it.value
    val uni = it.uni.value
    val action = it.uni.flow.action
    return when (action) {
      ACTION_STUDENT_LIST -> {
        studentList(tg, it.uni.value, it.value, it.uni.flow.json)
        Err(0)
      }

      ACTION_PRINT_PEER_REVIEW_SCORES -> {
        printSprintScores(tg, uni, sprint, false)
        tg.reply(
          "Зафиксировать оценки? Это сделает их видимыми для студентов", buttons = listOf(
            BtnData(
              "Да",
              it.uni.flow.json.apply {
                put("p", ACTION_FIX_REVIEW_SCORES)
              }.toString()
            ),
            BtnData(
              "Нет", it.uni.flow.json.apply {
                removeAll()
              }.toString()
            )
          )
        )
        Err(0)
      }

      ACTION_FIX_REVIEW_SCORES -> {
        printSprintScores(tg, uni, sprint, true)
        tg.reply("Done")
        teacherLanding(tg)
        Err(0)
      }

      else -> it.withStudent()
    }
  }

  private fun teacherWithUniversity(it: UniContext, tg: ChainBuilder): Result<SprintContext, Any> {
    println("UNI: ${it.value}")
    val uni = it.value
    val action = it.flow.action
    return when (action) {
      ACTION_TEACHER_LANDING -> {
        teacherPageChooseAction(tg, it)
        Err(0)
      }

      ACTION_ROTATE_TEAMS -> {
        teacherPageRotateProjects(tg, it.flow.json)
        Err(0)
      }
      //ACTION_GREET_STUDENT -> studentRegister(tg, json)
      ACTION_FINISH_ITERATION -> {
        teacherPageFinishIteration(tg, it.flow.json)
        Err(0)
      }

      ACTION_PRINT_TEAMS -> {
        getAllCurrentTeamRecords(it.value).print(tg)
        Err(0)
      }

      ACTION_SEND_REMINDERS -> {
        teacherPageReminderList(tg, it)
        Err(0)
      }

      ACTION_PRINT_PEER_REVIEW_SCORES -> {
        if (it.flow.json.getSprint() == -1) {
          printAllScores(tg, uni)
          Err(0)
        } else {
          it.withSprint()
        }
      }
      else -> it.withSprint()
    }
  }

  private fun teacherPageReminderList(tg: ChainBuilder, ctx: UniContext) {
    when (ctx.flow.json.getReminder()) {
      0 -> {
        tg.reply("О чем напомнить?", buttons = listOf(
          BtnData("Провести peer assessment", callbackData = ctx.flow.json.put(REMINDER_FIELD, 1).toString())
        ))
      }
      1 -> {
        findMissingPeerAssessments(ctx.value).forEach {rec ->
          getMessageSender().send(SendMessage().also {
            it.chatId = rec.tgUserid.toString()
            it.text = "Hello! Please complete peer assessment for your team mate on the last sprint."
          })
          tg.reply("Пнули пользователя ${rec.tgUsername}")
        }
      }
    }
  }
}

internal fun teacherLanding(tg: ChainBuilder) {
  tg.reply("Выберите вуз", buttons = listOf(
    BtnData("CUB", """ {"u": 0, "p": $ACTION_TEACHER_LANDING} """)
  ))
}

private fun teacherPageChooseAction(tg: ChainBuilder, ctx: UniContext) {
  tg.reply(
    "Чего изволите?", isMarkdown = false, stop = true, buttons = listOf(
      BtnData("Напечатать команды", ctx.flow.json.put("p", ACTION_PRINT_TEAMS).toString()),
      BtnData("Рецензии и оценки", ctx.flow.json.put("p", ACTION_STUDENT_LIST).toString()),
      BtnData("Посмотреть ведомость", ctx.flow.json.put("p", ACTION_PRINT_PEER_REVIEW_SCORES).toString()),
      BtnData("Завершить итерацию", ctx.flow.json.put("p", ACTION_FINISH_ITERATION).toString()),
      BtnData("Сделать ротацию в проектах", ctx.flow.json.put("p", ACTION_ROTATE_TEAMS).toString()),
      BtnData("Пнуть студентов", ctx.flow.json.put("p", ACTION_SEND_REMINDERS).toString())
    ), maxCols = 1
  )
}

private fun studentList(tg: ChainBuilder, uni: Int, sprintNum: Int, json: ObjectNode) {
  val teamRecords = getAllSprintTeamRecords(uni, sprintNum)
  val student = json.getStudent()?.let { studentId ->
    teamRecords.firstOrNull { it.id == studentId }
  } ?: run {
    teamRecords.forEach {
      tg.reply("${it.displayName} (проект ${it.teamNum})", maxCols = 2, buttons = listOf(
        BtnData("Отзыв", json.apply {
          setAction(ACTION_REVIEW_STUDENTS)
          setStudent(it.id)
          setSprint(sprintNum)
        }.toString()),
        BtnData("Оценка", json.apply {
          setAction(ACTION_SCORE_STUDENTS)
          setStudent(it.id)
          setSprint(sprintNum)
        }.toString()),
      ))
    }
    tg.reply("Выберите студента")
    return
  }
  json.getAction().onSuccess { action ->
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

private fun writeTeacherReview(teacherId: Long, studentId: Int, sprintNum: Int, review: String) {
  txn {
    insertInto(TEACHERREVIEW,
      TEACHERREVIEW.STUDENT_ID, TEACHERREVIEW.SPRINT_NUM, TEACHERREVIEW.TEACHER_TGID, TEACHERREVIEW.REVIEW
    ).values(
      studentId, sprintNum, teacherId, review
    ).onConflict(
      TEACHERREVIEW.STUDENT_ID, TEACHERREVIEW.SPRINT_NUM, TEACHERREVIEW.TEACHER_TGID
    ).doUpdate().set(TEACHERREVIEW.REVIEW, review)
      .execute()
  }
}

private fun getReviews(studentId: Int, sprintNum: Int) =
  db {
    selectFrom(TEACHERREVIEW).where(TEACHERREVIEW.STUDENT_ID.eq(studentId)).and(TEACHERREVIEW.SPRINT_NUM.eq(sprintNum))
      .mapNotNull {
        it.review
      }
  }

private fun String.lastNameFirst(): String {
  val (first, last) = this.split("\\s+".toRegex(), limit = 2)
  return "$last $first"
}

private fun ObjectNode.getReminder(): Int = this[REMINDER_FIELD]?.asInt() ?: 0
private const val REMINDER_FIELD = "r"
