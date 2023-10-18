package com.bardsoftware.sqool.bot

import com.bardsoftware.libbotanique.*
import com.bardsoftware.sqool.bot.db.tables.records.HatplayerRecord
import com.bardsoftware.sqool.bot.db.tables.records.HatplayerviewRecord
import com.bardsoftware.sqool.bot.db.tables.records.HatroundRecord
import com.bardsoftware.sqool.bot.db.tables.references.*
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.onSuccess
import initContext
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.time.LocalDateTime

private const val ACTION_LEAVE_HAT = 100
private const val ACTION_ROUND_INIT = 101
private const val ACTION_ROUND_START = 102
private const val ACTION_ROUND_STOP = 103
private const val ACTION_WORD_GUESSED = 104
private const val ACTION_WORD_SKIPPED = 105
private const val ACTION_WORD_NEXT = 106

fun hatCommands(tg: ChainBuilder) {
  tg.onCommand("hat", executeImmediately = true) {
    hatPlayerLanding(tg)
  }
  tg.onCommand("join_hat", executeImmediately = true) {
    addHatPlayer(tg.userName)
    tg.reply("Welcome to The Hat, ${tg.fromUser?.displayName()}!")
    tg.stop()
  }
  tg.initContext().onSuccess { ctx ->
    when (ctx.action) {
      ACTION_LEAVE_HAT -> {
        removeHatPlayer(tg.userName)
        tg.reply("See you, ${tg.fromUser?.displayName()}!")
      }
      ACTION_ROUND_INIT -> {
        val allPlayers = getHatPlayers()
        val rounds = getHatRounds()
        var notPlayed: MutableList<HatplayerviewRecord> = allPlayers.filter { player ->
          rounds.find { it.leader == player.tgUsername || it.follower == player.tgUsername } == null
        }.toMutableList()
        if (notPlayed.size < 2) {
          notPlayed = allPlayers.toMutableList()
        }
        if (notPlayed.size < 2) {
          tg.reply("We have less than 2 players :(")
        } else {
          val player1 = notPlayed.random().also { notPlayed.remove(it) }
          val player2 = notPlayed.random()
          tg.reply(
            "A new round: ${player1.name} and ${player2.name}", buttons = listOf(
              BtnData("Go Ahead!", callbackData = ctx.json.apply {
                setAction(ACTION_ROUND_START)
                put("p1", player1.tgUsername)
                put("p2", player2.tgUsername)
              }.toString()),
              BtnData("Oh wait...", callbackData = ctx.json.apply {
                setAction(ACTION_ROUND_STOP)
              }.toString())
            )
          )
          getMessageSender().send(SendMessage(player1.tgUserid.toString(),
            """Hello ${player1.name}! You are the Describer. 
              | - Your task is to explain the words that I send to you to ${player2.name}. Their task is
              |   to guess the word. 
              | - You will be given 5 words, and you need to spend as little time as possible.
              | - Once the word is guessed, we advance to the next one.
              | - You can skip a word, but there is a time penalty for skipping, that grows exponentially.   
              | - Please don't use the same root words or phonetically similar words. 
              |
            """.trimMargin()))
          getMessageSender().send(SendMessage(player2.tgUserid.toString(),
            """Hello ${player2.name}! You are the Guesser. 
              | - Your task is to guess the words that I ${player1.name} will try to explain to you.
              | - You will be given 5 words, and you need to spend as little time as possible.
              | - Once the word is guessed, we advance to the next one.
              |
            """.trimMargin()))
        }
      }
      ACTION_ROUND_START -> {
        val leader = ctx.json["p1"].asText()
        val follower = ctx.json["p2"].asText()
        val leaderName = getHatPlayers().find { it.tgUsername == leader }?.name ?: leader
        val followerName = getHatPlayers().find { it.tgUsername == follower }?.name ?: follower
        insertHatRound(leader, follower)?.let {roundId ->
          tg.reply("""Round ${roundId} started. 
            |Now I will challenge the Describer, $leaderName to explain words to $followerName. 
            |You are the moderator and you need to click "OK" if a word has been guessed correctly or "Skip" if it was skipped.
          """.trimMargin())
          sendWord(tg, roundId)
        }
      }
      ACTION_WORD_GUESSED -> {
        val roundId = ctx.json["r"].asInt()
        val wordId = ctx.json["w"].asInt()
        recordWordGuess(wordId, roundId)
        stopOrContinue(tg, roundId, ctx.json)
      }
      ACTION_WORD_SKIPPED -> {
        val roundId = ctx.json["r"].asInt()
        stopOrContinue(tg, roundId, ctx.json)
      }
      ACTION_WORD_NEXT -> {
        val roundId = ctx.json["r"].asInt()
        sendWord(tg, roundId)
      }
      ACTION_ROUND_STOP -> {
        val roundId = ctx.json["r"].asInt()
        stopHatRound(roundId)?.let {
          tg.reply("The round was completed in ${it.resultSec} seconds")
        }
        hatPlayerLanding(tg)
      }
    }
  }

}

fun stopOrContinue(tg: ChainBuilder, roundId: Int, json: ObjectNode) {
  val wordCount = db {
    selectFrom(HATROUNDRESULTS).where(HATROUNDRESULTS.ID.eq(roundId)).fetchOne()?.wordCount ?: 0
  }
  if (wordCount < 5) {
    tg.reply(
      "Continue?", buttons = listOf(
        BtnData("Yes!", callbackData = json.apply {
          setAction(ACTION_WORD_NEXT)
          remove("w")
        }.toString()),
        BtnData("STOP", callbackData = json.apply {
          setAction(ACTION_ROUND_STOP)
          remove("w")
        }.toString())
      )
    )
  } else {
    tg.reply("This round is over!")
    stopHatRound(roundId)?.let {
      tg.reply("The round was completed in ${it.resultSec} seconds")
    }
    hatPlayerLanding(tg)
  }
}

fun sendWord(tg: ChainBuilder, roundId: Int) {
  val words = db {
    selectFrom(HATWORD).where(HATWORD.ID.notIn(select(WORDGUESS.WORD_ID).from(WORDGUESS).where(WORDGUESS.ROUND_ID.eq(roundId)))).toList()
  }
  val leaderId = db {
    select(STUDENT.TG_USERID)
      .from(HATROUND.join(STUDENT).on(HATROUND.LEADER.eq(STUDENT.TG_USERNAME)))
      .where(HATROUND.ID.eq(roundId))
  }

  if (words.isEmpty()) {
    tg.reply("No more words, sir! Click STOP above")
    return
  }
  words.random().let {
    val wordId = it.id!!
    insertChallenge(wordId, roundId)
    tg.reply("Next word: ${it.value}", buttons = listOf(
      BtnData("Guessed!", callbackData = OBJECT_MAPPER.createObjectNode().apply {
        setAction(ACTION_WORD_GUESSED)
        put("r", roundId)
        put("w", wordId)
      }.toString()),
      BtnData("Skipped...", callbackData = OBJECT_MAPPER.createObjectNode().apply {
        setAction(ACTION_WORD_SKIPPED)
        put("r", roundId)
        put("w", wordId)
      }.toString()),
    ))
    getMessageSender().send(SendMessage(leaderId.toString(), "Next word: ${it.value}"))
  }


}

fun insertHatRound(tgUsername1: String, tgUsername2: String) =
  txn {
    insertInto(HATROUND, HATROUND.LEADER, HATROUND.FOLLOWER, HATROUND.START_TS).values(tgUsername1, tgUsername2, LocalDateTime.now())
      .returning(HATROUND.ID).fetchOne()?.component1()
  }

fun stopHatRound(roundId: Int) =
  txn {
    update(HATROUND).set(HATROUND.STOP_TS, LocalDateTime.now()).where(HATROUND.ID.eq(roundId)).execute()
    selectFrom(HATROUNDRESULTS).where(HATROUNDRESULTS.ID.eq(roundId)).fetchOne()
  }

fun insertChallenge(wordId: Int, roundId: Int) =
  txn {
    insertInto(WORDGUESS, WORDGUESS.WORD_ID, WORDGUESS.ROUND_ID).values(wordId, roundId).execute()
  }

fun recordWordGuess(wordId: Int, roundId: Int) =
  txn {
    update(WORDGUESS).set(WORDGUESS.IS_OK, true).where(WORDGUESS.ROUND_ID.eq(roundId)).and(WORDGUESS.WORD_ID.eq(wordId)).execute()
  }


// -------------------------------------------------
fun getHatPlayers(): List<HatplayerviewRecord> =
  db {
    selectFrom(HATPLAYERVIEW).toList()
  }

fun getHatRounds(): List<HatroundRecord> =
  db {
    selectFrom(HATROUND).toList()
  }

fun hatPlayerLanding(tg: ChainBuilder) {
  getHatPlayers().find { it.tgUsername == tg.userName }?.let {
    val btns = listOf(
      BtnData("Leave Tournament", callbackData = OBJECT_MAPPER.createObjectNode().apply {
        setAction(ACTION_LEAVE_HAT)
      }.toString()),
    ) + if (isTeacher(tg.userName)) {
      listOf(
        BtnData("Start a new round!", callbackData = OBJECT_MAPPER.createObjectNode().apply {
          setAction(ACTION_ROUND_INIT)
        }.toString())
      )
    } else emptyList()
    tg.reply("You are playing The Hat!", buttons = btns)
  } ?: run {
    tg.reply("Do you want to join The Hat tournament?", buttons = listOf(BtnData("/join_hat")), isInlineKeyboard = false)
  }
  tg.stop()

}
fun addHatPlayer(tgUsername: String) =
  txn {
    insertInto(HATPLAYER, HATPLAYER.TG_USERNAME).values(tgUsername).onConflictDoNothing().execute()
  }

fun removeHatPlayer(tgUsername: String) =
  txn {
    deleteFrom(HATPLAYER).where(HATPLAYER.TG_USERNAME.eq(tgUsername)).execute()
  }

