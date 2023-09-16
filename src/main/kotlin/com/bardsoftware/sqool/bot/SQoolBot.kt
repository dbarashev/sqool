/*
 * Copyright (c) BarD Software s.r.o 2021
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
import com.github.michaelbull.result.*
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.bots.TelegramWebhookBot
import org.telegram.telegrambots.meta.ApiConstants
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.telegram.telegrambots.updatesreceivers.DefaultWebhook
import java.io.Serializable
import java.math.BigDecimal
import com.github.michaelbull.result.Result as Res

fun main(args: Array<String>) {
  if (args.isNotEmpty() && args[0] == "poll") {
    TelegramBotsApi(DefaultBotSession::class.java).registerBot(SQoolBot())
  } else {
    TelegramBotsApi(DefaultBotSession::class.java, DefaultWebhook().also {
      it.setInternalUrl("http://0.0.0.0:8080")
    }).registerBot(SQooLWebhookBot(), SetWebhook(System.getenv("TG_BOT_URL")))
  }
}

/**
 * @author dbarashev@bardsoftware.com
 */
class SQoolBot : TelegramLongPollingBot(
    DefaultBotOptions().apply {
      baseUrl = System.getenv("TG_BASE_URL") ?: ApiConstants.BASE_URL
    }),
    MessageSender {
  init {
    println("Started SQooL 2023 under name $botUsername")
    setMessageSender(this)
  }
  override fun getBotUsername() = System.getenv("TG_BOT_USERNAME") ?: "sqool_bot"
  override fun getBotToken(): String = System.getenv("TG_BOT_TOKEN") ?: ""
  override fun <T : BotApiMethod<Serializable>> send(msg: T) {
    execute(msg)
  }

  override fun sendDoc(doc: SendDocument) {
    execute(doc)
  }

  override fun forward(msg: Message, toChat: String) {
    execute(ForwardMessage().also {
      it.chatId = toChat
      it.fromChatId = msg.chatId.toString()
      it.messageId = msg.messageId
    })
  }

  override fun onUpdateReceived(update: Update) {
    process(update, this)
  }
}

class SQooLWebhookBot : TelegramWebhookBot(), MessageSender {
  override fun getBotUsername() = System.getenv("TG_BOT_USERNAME") ?: ""
  override fun getBotToken(): String = System.getenv("TG_BOT_TOKEN") ?: ""
  override fun getBotPath(): String = System.getenv("TG_BOT_PATH") ?: ""

  init {
    println("Started SQooL 2022 under name $botUsername")
    setMessageSender(this)
  }

  override fun onWebhookUpdateReceived(update: Update): BotApiMethod<*>? {
    return try {
      LOGGER.info("Received update: $update")
      process(update, this)
      null
    } catch (ex: Exception) {
      LOGGER.error("Failed to process update", ex)
      SendMessage().apply {
        chatId = update.message?.chatId?.toString() ?: ""
        text = "ERROR"

      }
    }
  }

  override fun <T : BotApiMethod<Serializable>> send(msg: T) {
    execute(msg)
  }

  override fun sendDoc(doc: SendDocument) {
    execute(doc)
  }

  override fun forward(msg: Message, toChat: String) {
    execute(ForwardMessage().also {
      it.chatId = toChat
      it.fromChatId = msg.chatId.toString()
      it.messageId = msg.messageId
    })
  }
}

private fun process(update: Update, sender: MessageSender) = chain(update, sender) {
  val tg = this
  onCallback { json ->
    val dialogPage = json["p"]?.asInt() ?: 0
    when (dialogPage) {
      ACTION_TEACHER_LANDING -> teacherPageChooseAction(tg, json)
      ACTION_ROTATE_TEAMS -> teacherPageRotateProjects(tg, json)
      ACTION_GREET_STUDENT -> studentRegister(tg, json)
      ACTION_FINISH_ITERATION -> teacherPageFinishIteration(tg, json)
      ACTION_PRINT_TEAMS -> {
        json.getUniversity().map {
          getAllCurrentTeamRecords(it).print(tg)
        }.onFailure {msg ->
          tg.reply(msg, isMarkdown = false)
        }

      }
    }
  }
  TeammateScoringFlow(tg)
  TeacherScoringFlow(tg)
  if (update.message?.chatId == update.message?.from?.id) {
    onCommand("start", "help") {
      onStart()
    }

    ScoringReportFlow(tg)
    StudentCommands(tg)
    onText("pingme") {
      getStudent(this.userName)?.let {student ->
        sender.send(SendMessage().also {
          it.chatId = student.tgUserid.toLong().toString()
          it.text = "Ping you!"
        } as BotApiMethod<Serializable>)
        stop()
      }
    }
    onText("git") {
      getStudent(this.userName)?.let { student ->
        updateRepositoryPermissions(
          listOf(TeamMember(teamNum = 2, tgUsername = "dbarashev", ord = 1, githubUsername = "dbarashev")),
          listOf(TeamMember(teamNum = 1, tgUsername = "dbarashev", ord = 1, githubUsername = "dbarashev"))
        )
        sender.send(SendMessage().also {
          it.chatId = student.tgUserid.toLong().toString()
          it.text = "yo, trying..."
        } as BotApiMethod<Serializable>)
        stop()
      }
    }
    onRegexp(".*") {
      onStart()
    }
  }
}

fun ChainBuilder.onStart() {
  if (isTeacher(this.userName)) {
    return teacherLanding(this)
  }
  val student = getStudent(this.userName)
  if (student == null) {
    reply("Вы студент CUB и посещаете курс Database Internals '23?", buttons = listOf(
      BtnData("Да!", """ {"tg": "${this.userName}", "p": $ACTION_GREET_STUDENT, "a": 1} """),
      BtnData("Нет :(", """ {"tg": "${this.userName}", "p": $ACTION_GREET_STUDENT, "a": 0} """),
    ), isMarkdown = false, stop = true)
  } else {
    if (student.tgUserid == BigDecimal.ZERO) {
      student.updateTgUserId(update.message.from.id)
    }
    studentLandingMenu(this, student)
  }
  stop()
}
fun teacherLanding(tg: ChainBuilder) {
  tg.reply("Выберите вуз", isMarkdown = false, stop = true, buttons = listOf(
    BtnData("CUB", """ {"u": 0, "p": 1} """)
  ))
  tg.stop()
}

private fun teacherPageChooseAction(tg: ChainBuilder, json: ObjectNode) {
  val uni = json.getUniversity().onFailure {
    tg.reply(it, isMarkdown = false, stop = true)
  }.unwrap()

  tg.reply("Чего изволите?", isMarkdown = false, stop = true, buttons = listOf(
    BtnData("Напечатать команды", """ {"u": $uni, "p": $ACTION_PRINT_TEAMS} """),
    BtnData("Поставить оценки", """{"u": $uni, "p": $ACTION_SCORE_STUDENTS}"""),
    BtnData("Посмотреть ведомость", """ {"u": $uni, "p": $ACTION_PRINT_PEER_REVIEW_SCORES } """),
    BtnData("Завершить итерацию", """ {"u": $uni, "p": $ACTION_FINISH_ITERATION} """),
    BtnData("Сделать ротацию в проектах", """ {"u": $uni, "p": $ACTION_ROTATE_TEAMS } """),
  ), maxCols = 1)
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

private fun teacherPageGetPeerScores(tg: ChainBuilder, json: ObjectNode) {
  val uni = json.getUniversity().onFailure {
    tg.reply(it, isMarkdown = false, stop = true)
  }.unwrap()
  val table = getAllScores(uni).joinToString("\n") {
    """${it.name.escapeMarkdown()}:${it.scoreSumFormula.escapeMarkdown()}:${it.scoreSources.entries.toString().escapeMarkdown()}"""
  }
  tg.reply("""|
      ```
      |${table.ifBlank { "МНЕ НЕЧЕГО ВАМ СКАЗАТЬ" }}
      ```
    |""".trimMargin(), isMarkdown = true, stop = true)
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

internal fun isTeacher(username: String) = (System.getenv("SQOOL_TEACHERS") ?: "").split(",").contains(username)

data class ArgFlow(val tg: ChainBuilder, val json: ObjectNode, val action: Int)
data class ArgUni(val value: Int, val flow: ArgFlow)
data class ArgSprint(val value: Int, val uni: ArgUni)

fun ChainBuilder.withFlow(json: ObjectNode, action: Int) = Ok(ArgFlow(this, json, action))

internal fun Ok<ArgFlow>.withUniversity(): Res<ArgUni, String> =
  this.component1().json.getUniversity().mapError {
    this.component1().tg.reply("Выберите вуз", isMarkdown = false, stop = true, buttons = listOf(
      BtnData("CUB", """ {"u": 0, "p": 1} """)
    ))
    ""
  }.map { ArgUni(it, this.component1())  }


internal fun ArgUni.withSprint(): Res<ArgSprint, Int> {
  val sprint = this.flow.json["s"]?.asInt()
  if (sprint != null) {
    return Ok(ArgSprint(sprint, this))
  }
  val uni = this.value
  val buttons = sprintNumbers(uni).map {
    BtnData(
      "№${it.component1()}",
      """{"p": ${this.flow.action}, "s": ${it.component1()}, "u": $uni } """
    )
  } + listOf(BtnData("Весь курс", """{"p": ${this.flow.action}, "s": -1, "u": $uni } """))
  this.flow.tg.reply("Выберите итерацию", buttons = buttons, maxCols = 4, isMarkdown = false, stop = true)
  return Err(0)
}

internal fun ArgSprint.execute(code: (tg: ChainBuilder, json: ObjectNode, uni: Int, sprintNum: Int) -> Unit) {
  code(this.uni.flow.tg, this.uni.flow.json, this.uni.value, this.value)
}

private const val INBOX_CHAT_ID = "-585161267"
private val LOGGER = LoggerFactory.getLogger("Bot")
internal const val ACTION_ROTATE_TEAMS = 2
internal const val ACTION_PRINT_PEER_REVIEW_SCORES = 3
internal const val ACTION_GREET_STUDENT = 4
internal const val ACTION_FINISH_ITERATION = 5
internal const val ACTION_SCORE_TEAMMATE = 6
internal const val ACTION_PRINT_TEAMS = 7
internal const val ACTION_SCORE_STUDENTS = 8
internal const val ACTION_FIX_REVIEW_SCORES = 9
internal const val ACTION_TEACHER_LANDING = 1