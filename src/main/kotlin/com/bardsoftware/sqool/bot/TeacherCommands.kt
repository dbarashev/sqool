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
        it.json.setAction(ACTION_TEACHER_LANDING)
        it.action = ACTION_TEACHER_LANDING
        it.withUniversity()
      }
      else if (it.action >= ACTION_LAST) {
        STOP
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
        teacherStudentInput(action, it, tg, studentId, sprint)
      } else {
        teacherStudentMenu(uni, sprint, studentId, action, tg, json)
      }
    }
    return Ok(it)
  }

  private fun teacherStudentMenu(uni: Int, sprint: Int, studentId: Int, action: Int, tg: ChainBuilder, json: ObjectNode): Err<Int> {
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
              | 
              | Ткните кнопку "Назад" если попали сюда по ошибке.
              | """.trimMargin(),
          buttons = listOf(
            BtnData("Назад", json.deepCopy().apply {
              setAction(ACTION_STUDENT_LIST)
              setStudent(null)
            }.toString())
          )
        )
        tg.userSession.save(ACTION_REVIEW_STUDENTS, json.toString())
      }
    }
    return STOP
  }

  private fun teacherStudentInput(action: Int, text: String, tg: ChainBuilder, studentId: Int, sprint: Int) =
    when (action) {
      ACTION_SCORE_STUDENTS -> {
        text.toDoubleOrNull()?.let {
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
        } ?: run {
          tg.reply("Оценка должна быть вещественным числом")
        }
        STOP
      }

      ACTION_REVIEW_STUDENTS -> {
        try {
          getStudent(studentId)?.let {
            writeTeacherReview(tg.userName, it.tgUsername, sprint, text)
            tg.reply("Отзыв записан!")
            teacherLanding(tg)
          }
        } finally {
          txn {
            tg.userSession.reset()
          }
        }
        STOP
      }

      else -> STOP
    }

  private fun teacherWithSprint(it: SprintContext, tg: ChainBuilder): Result<StudentContext, Any> {
    println("Sprint: ${it.value}")
    val sprint = it.value
    val uni = it.uni.value
    val action = it.uni.flow.action
    return when (action) {
      ACTION_STUDENT_LIST -> {
        studentList(tg, it.uni.value, it.value, it.uni.flow.json)
        STOP
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
        STOP
      }

      ACTION_FIX_REVIEW_SCORES -> {
        printSprintScores(tg, uni, sprint, true)
        tg.reply("Done")
        teacherLanding(tg)
        STOP
      }

      else -> it.withStudent()
    }
  }

  private fun teacherWithUniversity(it: UniContext, tg: ChainBuilder): Result<SprintContext, Any> {
    val uni = it.value
    val action = it.flow.action
    println("UNI: $uni, action: $action")
    return when (action) {
      ACTION_TEACHER_LANDING -> {
        teacherPageChooseAction(tg, it)
        STOP
      }

      ACTION_ROTATE_TEAMS -> {
        teacherPageRotateProjects(tg, uni)
        STOP
      }
      //ACTION_GREET_STUDENT -> studentRegister(tg, json)
      ACTION_FINISH_ITERATION -> {
        teacherPageFinishIteration(tg, uni)
        STOP
      }

      ACTION_PRINT_TEAMS -> {
        getAllCurrentTeamRecords(it.value).print(tg)
        STOP
      }

      ACTION_SEND_REMINDERS -> {
        teacherPageReminderList(tg, it)
        STOP
      }

      ACTION_PRINT_PEER_REVIEW_SCORES -> {
        if (it.flow.json.getSprint() == -1) {
          printAllScores(tg, uni)
          STOP
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
  tg.reply("Привет ${tg.fromUser?.displayName()}! Вы ПРЕПОДАВАТЕЛЬ.")
}

private fun teacherPageChooseAction(tg: ChainBuilder, ctx: UniContext) {
  val btns =
    if (tg.userName == "dbarashev") {
      listOf(
        BtnData("Напечатать команды", ctx.flow.json.setAction(ACTION_PRINT_TEAMS).toString()),
        BtnData("Ввести рецензии и оценки", ctx.flow.json.setAction(ACTION_STUDENT_LIST).toString()),
        BtnData("Посмотреть ведомость", ctx.flow.json.setAction(ACTION_PRINT_PEER_REVIEW_SCORES).toString()),
        BtnData("Завершить итерацию", ctx.flow.json.setAction(ACTION_FINISH_ITERATION).toString()),
        BtnData("Сделать ротацию в проектах", ctx.flow.json.setAction(ACTION_ROTATE_TEAMS).toString()),
        BtnData("Пнуть студентов", ctx.flow.json.setAction(ACTION_SEND_REMINDERS).toString())
      )
    } else {
      listOf(
        BtnData("Ввести рецензии и оценки", ctx.flow.json.setAction(ACTION_STUDENT_LIST).toString()),
      )
    }
  tg.reply(
    "Чего изволите?", isMarkdown = false, stop = true, buttons = btns, maxCols = 1
  )
}

private fun studentList(tg: ChainBuilder, uni: Int, sprintNum_: Int, json: ObjectNode) {
  var sprintNum = sprintNum_
  if (sprintNum != 0) {
    tg.reply("Сорян, оценки и отзывы можно ставить только для последнего спринта. Выбрал его автоматически.")
    sprintNum = 0
  }
  json.getStudent()?.let(::getStudent)?.let { student ->
    getReviews(student.tgUsername, sprintNum).let {
      if (it.isNotEmpty()) {
        tg.reply("Существующие рецензии:")
      }
      it.forEach {tg.reply("""Рецензент: ${it.first}
            |
            |${it.second}
          """.trimMargin())}
    }
    tg.reply("""*${student.name.escapeMarkdown()}*""", buttons = listOf(
        BtnData("Отзыв", json.apply {
          setAction(ACTION_REVIEW_STUDENTS)
          setStudent(student.id)
          setSprint(sprintNum)
        }.toString()),
        BtnData("Оценка", json.apply {
          setAction(ACTION_SCORE_STUDENTS)
          setStudent(student.id)
          setSprint(sprintNum)
        }.toString()),
    ))
  } ?: run {
    val btns = getAllSprintTeamRecords(uni, sprintNum).map {
      BtnData("${it.displayName} (проект ${it.teamNum})", json.apply {
        setStudent(it.id)
        setSprint(sprintNum)
      }.toString())
    }
    tg.reply("Выберите студента", buttons = btns, maxCols = 1)
  }
//  getAllSprintTeamRecords(uni, sprintNum).forEach {
//    tg.reply("""*${it.displayName.escapeMarkdown()}* \(проект ${it.teamNum}\)""", maxCols = 2, isMarkdown = true,
//      buttons = listOf(
//        BtnData("Отзыв", json.apply {
//          setAction(ACTION_REVIEW_STUDENTS)
//          setStudent(it.id)
//          setSprint(sprintNum)
//        }.toString()),
//        BtnData("Оценка", json.apply {
//          setAction(ACTION_SCORE_STUDENTS)
//          setStudent(it.id)
//          setSprint(sprintNum)
//        }.toString()),
//      )
//    )
//  }
//  tg.reply("Выберите студента")
  return
}

private fun teacherPageFinishIteration(tg: ChainBuilder, uni: Int) {
  val newIteration = finishIteration(uni)
  if (newIteration == -1) {
    tg.reply("Done")
  } else {
    tg.reply("Итерация завершена. Новая итерация №$newIteration. Не забудь сделать ротацию.")
  }
}


private fun teacherPageRotateProjects(tg: ChainBuilder, uni: Int) {
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

private fun writeTeacherReview(teacherUsername: String, studentUsername: String, sprintNum: Int, review: String) {
  txn {
    insertInto(TEACHERREVIEW,
      TEACHERREVIEW.STUDENT_USERNAME, TEACHERREVIEW.SPRINT_NUM, TEACHERREVIEW.TEACHER_USERNAME, TEACHERREVIEW.REVIEW
    ).values(
      studentUsername, sprintNum, teacherUsername, review
    ).onConflict(
      TEACHERREVIEW.STUDENT_USERNAME, TEACHERREVIEW.SPRINT_NUM, TEACHERREVIEW.TEACHER_USERNAME
    ).doUpdate().set(TEACHERREVIEW.REVIEW, review)
      .execute()
  }
}

private fun getReviews(studentUsername: String, sprintNum: Int) =
  db {
    selectFrom(TEACHERREVIEW).where(TEACHERREVIEW.STUDENT_USERNAME.eq(studentUsername)).and(TEACHERREVIEW.SPRINT_NUM.eq(sprintNum))
      .mapNotNull {
        it.teacherUsername to it.review
      }
  }

private fun String.lastNameFirst(): String {
  val (first, last) = this.split("\\s+".toRegex(), limit = 2)
  return "$last $first"
}

private fun ObjectNode.getReminder(): Int = this[REMINDER_FIELD]?.asInt() ?: 0
private const val REMINDER_FIELD = "r"
private val STOP = Err(0)