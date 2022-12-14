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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
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
    println("Started SQooL 2022 under name $botUsername")
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
    try {
      LOGGER.info("Received update: $update")
      process(update, this)
      return null
    } catch (ex: Exception) {
      LOGGER.error("Failed to process update", ex)
      return SendMessage().apply {
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
      ACTION_PRINT_TEAMS -> teacherPagePrintTeams(tg, json)
    }
  }
  TeammateScoringFlow(tg)
  TeacherScoringFlow(tg)
  if (update.message?.chatId == update.message?.from?.id) {
    onCommand("start", "help") {
      if (isTeacher(this.userName)) {
        return@onCommand teacherLanding(tg)
      }
      val student = getStudent(this.userName)
      if (student == null) {
        reply("???? ?????????????? JUB ?? ?????????????????? ???????? Database Internals 2022?", buttons = listOf(
            BtnData("????!", """ {"tg": "${this.userName}", "p": $ACTION_GREET_STUDENT, "a": 1} """),
            BtnData("?????? :(", """ {"tg": "${this.userName}", "p": $ACTION_GREET_STUDENT, "a": 0} """),
        ), isMarkdown = false, stop = true)
      } else {
        val curTeammates = getCurrentTeammates(this.userName)
        reply("????????????, ${student.name}!", isMarkdown = false, stop = true)
        studentLandingMenu(this)
      }
      stop()
    }

    ScoringReportFlow(tg)
    StudentCommands(tg)
//    onRegexp(".*") {
//      sender.forward(update.message, INBOX_CHAT_ID)
//    }
  }
}

fun teacherLanding(tg: ChainBuilder) {
  tg.reply("???????????????? ??????", isMarkdown = false, stop = true, buttons = listOf(
    BtnData("JUB", """ {"u": 0, "p": 1} """)
  ))
  tg.stop()
}

private fun teacherPageChooseAction(tg: ChainBuilder, json: ObjectNode) {
  val uni = json["u"]?.asInt() ?: run {
    tg.reply("???????????? ??????????????????: ???? ???????????? ??????????????????????", isMarkdown = false, stop = true)
    return
  }
  tg.reply("???????? ?????????????????", isMarkdown = false, stop = true, buttons = listOf(
    BtnData("???????????????????? ??????????????", """ {"u": $uni, "p": $ACTION_PRINT_TEAMS} """),
    BtnData("?????????????????? ????????????", """{"u": $uni, "p": $ACTION_SCORE_STUDENTS}"""),
    BtnData("???????????????????? ??????????????????", """ {"u": $uni, "p": $ACTION_PRINT_PEER_REVIEW_SCORES } """),
    BtnData("?????????????????? ????????????????", """ {"u": $uni, "p": $ACTION_FINISH_ITERATION} """),
    BtnData("?????????????? ?????????????? ?? ????????????????", """ {"u": $uni, "p": $ACTION_ROTATE_TEAMS } """),
  ), maxCols = 1)
}

private fun teacherPageRotateProjects(tg: ChainBuilder, json: ObjectNode) {
  val uni = json["u"]?.asInt() ?: run {
    tg.reply("???????????? ??????????????????: ???? ???????????? ??????????????????????", isMarkdown = false, stop = true)
    return
  }

  tg.reply("???????????????????? ?????????????? ?? ???????????????????????? $uni", isMarkdown = false, stop = true)
  val newTeamRecords = rotateTeams(uni)
  println(newTeamRecords)
  insertNewRotation(newTeamRecords)
  teacherPagePrintTeams(tg, json)
}

private fun teacherPageGetPeerScores(tg: ChainBuilder, json: ObjectNode) {
  val uni = json["u"]?.asInt() ?: run {
    tg.reply("???????????? ??????????????????: ???? ???????????? ??????????????????????", isMarkdown = false, stop = true)
    return
  }
  val table = getAllScores(uni).joinToString("\n") {
    """${it.name.escapeMarkdown()}:${it.scoreSumFormula.escapeMarkdown()}:${it.scoreSources.entries.toString().escapeMarkdown()}"""
  }
  tg.reply("""|
      ```
      |${table.ifBlank { "?????? ???????????? ?????? ??????????????" }}
      ```
    |""".trimMargin(), isMarkdown = true, stop = true)
}

private fun teacherPageFinishIteration(tg: ChainBuilder, json: ObjectNode) {
  val uni = json["u"]?.asInt() ?: run {
    tg.reply("???????????? ??????????????????: ???? ???????????? ??????????????????????", isMarkdown = false, stop = true)
    return
  }
  val newIteration = finishIteration(uni)
  if (newIteration == -1) {
    tg.reply("Done")
  } else {
    tg.reply("???????????????? ??????????????????, ?????????? ???????????????? ???$newIteration. ???? ???????????? ?????????????? ??????????????.", isMarkdown = false, stop=true)
  }
}

private fun teacherPagePrintTeams(tg: ChainBuilder, json: ObjectNode) {
  val uni = json["u"]?.asInt() ?: run {
    tg.reply("???????????? ??????????????????: ???? ???????????? ??????????????????????", isMarkdown = false, stop = true)
    return
  }
  var teamNum = -1
  val buf = StringBuffer()
  getAllCurrentTeamRecords(uni).forEach {
    if (it.teamNum > teamNum) {
      buf.append("\n\n")
      teamNum = it.teamNum
      buf.append("__team ${teamNum}__\n")
    }
    buf.append(it.displayName.escapeMarkdown()).append("\n")
  }
  tg.reply(buf.toString(), isMarkdown = true)
}

private fun studentRegister(tg: ChainBuilder, json: ObjectNode) {
  val answer = json["a"]?.asInt() ?: 0
  if (answer == 1) {
    insertStudent(tg.userName, tg.fromUser?.displayName() ?: tg.userName)
    tg.reply("????????, ???? ???????????? ?? ???????? ??????????????.", isMarkdown = false, stop = true)
  } else {
    tg.reply("???? ????????, ????????????.", isMarkdown = false, stop = true)
  }
}
internal fun isTeacher(username: String) = (System.getenv("SQOOL_TEACHERS") ?: "").split(",").contains(username)

data class ArgFlow(val tg: ChainBuilder, val json: ObjectNode, val action: Int)
data class ArgUni(val value: Int, val flow: ArgFlow)
data class ArgSprint(val value: Int, val uni: ArgUni)

fun ChainBuilder.withFlow(json: ObjectNode, action: Int) = Ok(ArgFlow(this, json, action))

internal fun Ok<ArgFlow>.withUniversity(): Res<ArgUni, String> {
  val uni = this.component1().json["u"]?.asInt() ?: run {
    this.component1().tg.reply("???????????????? ??????", isMarkdown = false, stop = true, buttons = listOf(
      BtnData("JUB", """ {"u": 0, "p": 1} """)
    ))
    return Err("")
  }
  return Ok(ArgUni(uni, this.component1()))
}

internal fun ArgUni.withSprint(): Res<ArgSprint, Int> {
  val sprint = this.flow.json["s"]?.asInt()
  if (sprint != null) {
    return Ok(ArgSprint(sprint, this))
  }
  val uni = this.value
  val buttons = sprintNumbers(uni).map {
    BtnData(
      "???${it.component1()}",
      """{"p": ${this.flow.action}, "s": ${it.component1()}, "u": $uni } """
    )
  } + listOf(BtnData("???????? ????????", """{"p": ${this.flow.action}, "s": -1, "u": $uni } """))
  this.flow.tg.reply("???????????????? ????????????????", buttons = buttons, maxCols = 4, isMarkdown = false, stop = true)
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