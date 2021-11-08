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

import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.ApiConstants
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.io.Serializable

fun main() {
  TelegramBotsApi(DefaultBotSession::class.java).registerBot(SQoolBot())
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
    println("Started SQooL 2021 under name $botUsername")
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
    chain(update, this) {
      val tg = this
      onCallback { json ->
        val teammate = json["tg"]?.asText(null) ?: run {
          reply("Внутренняя ошибка. Кажется, вам надо послать команду /t снова", isMarkdown = false, stop = true)
          return@onCallback
        }
        val sprintNum = json["sprint"]?.asInt(0) ?: run {
          reply("Внутренняя ошибка. Кажется, вам надо послать команду /t снова", isMarkdown = false, stop = true)
          return@onCallback
        }
        db {
          dialogState(tg.userId, 1, "$teammate:$sprintNum")
          reply("Поставьте оценку $teammate. Вещественное число в диапазоне [0..1]", isMarkdown = false, stop = true)
        }
      }
      if (update.message?.chatId == update.message?.from?.id) {
        onRegexp(""".*""", whenState = 1) {
          val score = it.value.toDoubleOrNull()
          if (score == null) {
            reply("Оценка должна быть вещественным числом", isMarkdown = false, stop = true)
          } else {
            if (score < 0 || score > 1) {
              reply("Оценка должна быть в диапазоне [0..1]", isMarkdown = false, stop = true)
            } else {
              this.fromUser?.getDialogState()?.data?.let { data ->
                val (tgUsernameTo, sprintNum) = data.split(':', limit = 2)
                setScore(this.userName, tgUsernameTo, score, sprintNum.toInt())
                reply("Вы поставили  ${score} товарищу ${tgUsernameTo}. Чтобы изменить оценку, пошлите команду /t", isMarkdown = false, stop = true)
                db {
                  dialogState(tg.userId, null)
                }
              }
            }
          }
        }
        onCommand("start") {
          reply("Команда /t позволит поставить оценки товарищам. Всё остальное, что вы мне пишете, я пересылаю преподавателю.", isMarkdown = false)
          stop()
        }
        onCommand("t") {
          val curTeammates = getCurrentTeammates(this.userName)
          reply("Ваша нынешняя команда: ${curTeammates.map { it.first }.joinToString()}", isMarkdown = false)

          val teammates = getPrevTeammates(this.userName)
          val btns = teammates.members.map { BtnData(it.first, """{"tg": "${it.second}", "sprint": ${teammates.sprintNum}} """) }
          reply("Ваша команда на прошлой итерации. Если ткнуть в кнопку, можно поставить оценку", stop = true, buttons = btns, isMarkdown = false)
        }
        onRegexp(".*") {
          forward(update.message, INBOX_CHAT_ID)
        }
      }
    }
  }
}

private const val INBOX_CHAT_ID = "-585161267"