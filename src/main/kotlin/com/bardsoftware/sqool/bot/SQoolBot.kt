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

import com.bardsoftware.libbotanique.*
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.result.*
import initContext
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.telegram.telegrambots.updatesreceivers.DefaultWebhook
import whenLanding
import whenTeacher
import java.io.Serializable
import java.math.BigDecimal

class BotLauncher: CliktCommand() {
  val poll by option().flag(default = false)
  val testReplyChatId by option(help="ID of a chat where bot will send all messages to, for testing purposes")
  val testBecome by option(help="Comma-separated map of CHAT_ID:BECOME_CHAT_ID identifiers so that messages from the CHAT_ID were handled as if they come from BECOME_CHAT_ID")

  override fun run() {
    if (poll) {
      TelegramBotsApi(DefaultBotSession::class.java).registerBot(LongPollingConnector(::processMessage, testReplyChatId, testBecome))
    } else {
      TelegramBotsApi(DefaultBotSession::class.java, DefaultWebhook().also {
        it.setInternalUrl("http://0.0.0.0:8080")
      }).registerBot(WebHookConnector(::processMessage), SetWebhook(System.getenv("TG_BOT_URL")))
    }
  }

}
fun main(args: Array<String>) = BotLauncher().main(args)


private fun processMessage(update: Update, sender: MessageSender) = chain(update, sender) {
  val tg = this
  hatCommands(tg)
  TeammateScoringFlow(tg)
  TeacherCommands(tg)
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
//    onText("git") {
//      getStudent(this.userName)?.let { student ->
//        updateRepositoryPermissions(
//          listOf(TeamMember(teamNum = 2, tgUsername = "dbarashev", ord = 1, githubUsername = "dbarashev")),
//          listOf(TeamMember(teamNum = 1, tgUsername = "dbarashev", ord = 1, githubUsername = "dbarashev"))
//        )
//        sender.send(SendMessage().also {
//          it.chatId = student.tgUserid.toLong().toString()
//          it.text = "yo, trying..."
//        } as BotApiMethod<Serializable>)
//        stop()
//      }
//    }
    onRegexp(".*") {
      onStart()
    }
  }
}

fun ChainBuilder.onStart() {
  withUser { user ->
    if (isTeacher(this.userName)) {
      return@withUser
    }
    val student = getStudent(this.userName)
    if (student == null) {
      reply("Вы студент CUB и посещаете курс Database Internals '23?", buttons = listOf(
        BtnData("Да!", """ {"tg": "${this.userName}", "p": $ACTION_GREET_STUDENT, "a": 1} """),
        BtnData("Нет :(", """ {"tg": "${this.userName}", "p": $ACTION_GREET_STUDENT, "a": 0} """),
      ), isMarkdown = false, stop = true)
    } else {
      if (student.tgUserid == BigDecimal.ZERO) {
        student.updateTgUserId(user.id)
      }
      studentLandingMenu(this, student)
    }
    stop()
  }
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

internal fun isTeacher(username: String) = (System.getenv("SQOOL_TEACHERS") ?: "").split(",").contains(username)


private val LOGGER = LoggerFactory.getLogger("Bot")
internal const val ACTION_TEACHER_LANDING = 1
internal const val ACTION_ROTATE_TEAMS = 2
internal const val ACTION_PRINT_PEER_REVIEW_SCORES = 3
internal const val ACTION_GREET_STUDENT = 4
internal const val ACTION_FINISH_ITERATION = 5
internal const val ACTION_SCORE_TEAMMATE = 6
internal const val ACTION_PRINT_TEAMS = 7
internal const val ACTION_SCORE_STUDENTS = 8
internal const val ACTION_FIX_REVIEW_SCORES = 9
internal const val ACTION_SEND_REMINDERS = 10
internal const val ACTION_REVIEW_STUDENTS = 11
internal const val ACTION_STUDENT_LIST = 12
internal const val ACTION_LAST = 100