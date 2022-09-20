package com.bardsoftware.sqool.bot

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.escape.CharEscaper
import com.google.common.escape.Escapers
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
//import org.jooq.impl.DSL.field
//import org.spbelect.blacklist.shared.db
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendLocation
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.io.Serializable
import java.math.BigDecimal


data class Response(val text: String)
data class BtnData(val label: String, val callbackData: String = "")


typealias CallbackHandler = (ObjectNode) -> Unit
typealias MessageHandler = (String) -> Unit
typealias MatchHandler = (MatchResult) -> Unit

data class Document(val docId: String, val caption: String = "")
data class DocumentList(val docs : List<Document>, val container: Message)

typealias DocumentHandler = (DocumentList) -> Unit

interface MessageSender {
  fun <T: BotApiMethod<Serializable>> send(msg: T)
  fun forward(msg: Message, toChat: String)
  fun sendDoc(doc: SendDocument)
}

private var ourSender: MessageSender = object : MessageSender {
  override fun <T : BotApiMethod<Serializable>> send(msg: T) {
    TODO("Not yet implemented")
  }
  override fun sendDoc(doc: SendDocument) {
    TODO("Not yet implemented")
  }

  override fun forward(msg: Message, toChat: String) {
    TODO("Not yet implemented")
  }
}

fun getMessageSender() = ourSender
fun setMessageSender(sender: MessageSender) {
  ourSender = sender
}

enum class MessageSource {
  DIRECT
}

open class ChainBuilder(internal val update: Update, internal val sendMessage: MessageSender) {
  val escaper = Escapers.builder().let {
    for (c in charArrayOf('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!')) {
      it.addEscape(c, "\\$c")
    }
    it.build()
  }
  val messageText = (update.message?.text ?: "").trim()
  val fromUser = update.message?.from ?: update.callbackQuery?.from
  val messageId = update.callbackQuery?.message?.messageId ?: update.message?.messageId

  val userId = (this.fromUser?.id ?: -1).toLong()
  val userName = this.fromUser?.userName ?: ""
  val chatId = update.message?.chatId

  val dialogState: DialogState? by lazy {
    this.fromUser?.getDialogState()
  }

  private var replyChatId = update.message?.chatId ?: -1

  private val callbackHandlers = mutableListOf<CallbackHandler>()
  private val documentHandlers = mutableListOf<DocumentHandler>()
  private val handlers = mutableListOf<MessageHandler>()
  private var stopped = false
  private val replies = mutableListOf<BotApiMethod<Serializable>>()
  internal val docReplies = mutableListOf<SendDocument>()

  fun parseJson(code: (ObjectNode) -> Unit) {
    try {
      println(this.update.callbackQuery.data)
      val jsonNode = OBJECT_MAPPER.readTree(this.update.callbackQuery.data)
      if (jsonNode.isObject) {
        code(jsonNode as ObjectNode)
      } else {
        println("Malformed callback json: $jsonNode")
      }
    } catch (ex: JsonProcessingException) {
      ex.printStackTrace()
    }
  }

  fun onCallback(code: CallbackHandler) {
    this.callbackHandlers += code
  }

  fun onDocument(whenState: Int? = null, code: DocumentHandler) {
    this.documentHandlers += {docs ->
      if (whenState == null || this.dialogState?.state == whenState) {
        code(docs)
      }
    }
  }

  fun onCommand(vararg commands: String, messageSource: MessageSource = MessageSource.DIRECT, code: MessageHandler) {
    if (messageSource == MessageSource.DIRECT && update.message?.chatId != update.message?.from?.id) {
      return
    }
    this.handlers += { msg ->
      commands.forEach { command ->
        val slashedCommand = "/$command"
        if (msg.lowercase().startsWith(slashedCommand)) {
          code(msg.substring(slashedCommand.length).trim())
        }
      }
    }
  }

  fun onText(text: String, whenState: Int? = null, code: MessageHandler) {
    this.handlers += { msg ->
      if (msg == text) {
        if (whenState == null || this.dialogState?.state == whenState) {
          code(msg)
        }
      }
    }
  }
  fun onRegexp(pattern: String, options: Set<RegexOption> = setOf(RegexOption.MULTILINE), whenState: Int? = null,
               messageSource: MessageSource = MessageSource.DIRECT, code: MatchHandler) {
    if (messageSource == MessageSource.DIRECT && update.message?.chatId != update.message?.from?.id) {
      return
    }
    val regexp = pattern.toRegex(options)
    this.handlers += { msg ->
      regexp.matchEntire(msg.trim().replace("\n", ""))?.let {
        if (whenState == null || this.dialogState?.state == whenState) {
          code(it)
        } else {
          println("whenState=$whenState does not match the dialog state=${this.dialogState}")
        }

      }
    }
  }

  fun replyDocument(doc: String) {
    docReplies.add(SendDocument().also {
      it.document = InputFile(doc.byteInputStream(), "data.csv")
      it.chatId = this.update.message.chatId.toString()
    })
  }

  fun sendLocation(lat: BigDecimal, lon: BigDecimal) {
    replies.add(SendLocation().apply {
      chatId = this@ChainBuilder.replyChatId.toString()
      latitude = lat.toDouble()
      longitude = lon.toDouble()
    } as BotApiMethod<Serializable>)
  }
  fun reply(msg: String, stop: Boolean = true,
            buttons: List<BtnData> = listOf(),
            maxCols: Int = Int.MAX_VALUE,
            isMarkdown: Boolean = true,
            editMessageId: Int? = null,
            isInlineKeyboard: Boolean = true) {
    if (editMessageId == null) {
      replies.add(SendMessage().apply {
        chatId = this@ChainBuilder.replyChatId.toString()
        enableMarkdownV2(isMarkdown)
        text = msg.ifBlank { "ПУСТОЙ ОТВЕТ" }
        if (buttons.isNotEmpty()) {
          if (isInlineKeyboard) {
            replyMarkup = InlineKeyboardMarkup(
              buttons.map {
                InlineKeyboardButton(it.label).also { btn ->
                  if (it.callbackData.isNotBlank()) btn.callbackData = it.callbackData
                }
              }.chunked(maxCols)
            )
          } else {
            replyMarkup = ReplyKeyboardMarkup().also {
              it.keyboard = buttons.map { KeyboardButton(it.label) }.chunked(maxCols).map { KeyboardRow(it) }
              it.resizeKeyboard = true
            }
          }
        }
      } as BotApiMethod<Serializable>)
      this.stopped = this.stopped || stop
    } else {
      replies.add(EditMessageText().apply {
        messageId = editMessageId
        chatId = this@ChainBuilder.replyChatId.toString()
        enableMarkdown(isMarkdown)
        text = msg.ifBlank { "ПУСТОЙ ОТВЕТ" }
        if (buttons.isNotEmpty()) {
          replyMarkup = InlineKeyboardMarkup(
              buttons.map {
                InlineKeyboardButton(it.label).also { btn ->
                  if (it.callbackData.isNotBlank()) btn.callbackData = it.callbackData
                }
              }.chunked(maxCols)
          )
        }
      })
    }
  }

  fun stop() {
    this.stopped = true
  }

  fun handle(): List<out BotApiMethod<Serializable>> {
    try {
      when {
        !(this.update.message?.photo?.isNullOrEmpty() ?: true) -> {
          val docs = DocumentList(this.update.message.photo.map {
            Document(it.fileId, this.update.message.caption ?: "")
          }.toList(), this.update.message)

          for (h in documentHandlers) {
            h(docs)
            if (this.stopped) {
              break
            }
          }
        }
        this.update.message?.document != null -> {
          val docs = DocumentList(listOf(Document(this.update.message.document.fileId, this.update.message.caption ?: "")), this.update.message)
          for (h in documentHandlers) {
            h(docs)
            if (this.stopped) {
              break
            }
          }
        }
        this.update.callbackQuery != null -> {
          this.replyChatId = this.update.callbackQuery.message.chatId
          this.update.callbackQuery.let { q ->
            AnswerCallbackQuery().apply {
              callbackQueryId = q.id
            }.also { sendMessage.send(it as BotApiMethod<Serializable>) }

            parseJson {json ->
              for (h in callbackHandlers) {
                h(json)
                if (this.stopped) {
                  break
                }
              }
            }
          }
        }

        this.messageText.isNotBlank() -> {
          for (h in handlers) {
            h(this.messageText)
            if (this.stopped) {
              break
            }
          }
        }

        else -> {}
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
      reply("Что-то сломалось внутри бота", isMarkdown = false)
    }
    return replies
  }
}

fun chain(update: Update, sender: MessageSender, handlers: (ChainBuilder.() -> Unit)) {
  ChainBuilder(update, sender).apply(handlers).also{
    val replies = it.handle()
    replies.forEach { reply ->
      try {
        sender.send(reply)
      } catch (ex: TelegramApiRequestException) {
        ex.printStackTrace()
        when (reply) {
          is SendMessage -> {
            sender.send(SendMessage(reply.chatId, "Что-то сломалось при отправке ответа.") as BotApiMethod<Serializable>)
          }
          else -> println(reply)
        }
      }
    }
    it.docReplies.forEach {doc ->
      try {
        sender.sendDoc(doc)
      } catch (ex: TelegramApiRequestException) {
        ex.printStackTrace()
      }
    }
  }
}


private val escapedChars = charArrayOf('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!')
private val ESCAPER = object : CharEscaper() {
  override fun escape(c: Char): CharArray {
    return if (escapedChars.contains(c)) charArrayOf('\\', c) else charArrayOf(c)
  }
}

fun (String).escapeMarkdown() = ESCAPER.escape(this)

fun (ArrayNode).item(builder: ObjectNode.() -> Unit) {
  this.add(OBJECT_MAPPER.createObjectNode().also(builder))
}

val OBJECT_MAPPER = ObjectMapper()

data class DialogState(val state: Int, val data: String?) {
  fun asJson(): ObjectNode {
    try {
      return jacksonObjectMapper().readTree(data) as ObjectNode
    } catch (ex: JsonParseException) {
      println("""Failed to parse:
        |$data
      """.trimMargin())
      ex.printStackTrace()
      throw RuntimeException(ex)
    }
  }
}

fun User.displayName(): String = "${this.firstName} ${this.lastName}"

fun User.getDialogState(): DialogState? {
  val userId = this.id
  return db {
    select(field("state_id", Int::class.java), field("data", String::class.java))
        .from("DialogState")
        .where(field("tg_id").eq(userId))
        .firstOrNull()?.let {
          if (it.component1() == null) null else DialogState(it.component1(), it.component2())
        }
  }
}

fun jsonCallback(builder: ObjectNode.() -> Unit) =
    OBJECT_MAPPER.createObjectNode().also(builder).toString()

internal fun DSLContext.dialogState(userId: Long, stateId: Int?, data: String? = null) {
  insertInto(DSL.table("DialogState"))
      .columns(
          DSL.field("tg_id", Long::class.java),
          DSL.field("state_id", Int::class.java),
          DSL.field("data", String::class.java)
      )
      .values(userId.toLong(), stateId, data)
      .onConflict(DSL.field("tg_id", Long::class.java)).doUpdate()
      .set(DSL.field("state_id", Int::class.java), stateId)
      .set(DSL.field("data", String::class.java), data)
      .execute()
}
