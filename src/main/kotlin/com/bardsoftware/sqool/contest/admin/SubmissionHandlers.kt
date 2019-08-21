package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.format.DateTimeFormatterBuilder

private val JSON_MAPPER = ObjectMapper()

object Attempts : Table("Contest.Attempt") {
  val task_id = integer("task_id")
  val user_id = integer("user_id")
  val attempt_text = text("attempt_text")
}

data class SubmissionGetArgs(var user_id: String, var task_id: String, var reviewer_id: String) : RequestArgs()

class SubmissionGetHandler : RequestHandler<SubmissionGetArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionGetArgs): HttpResponse {
    return transaction {
      val attempts = Attempts.select {
        ((Attempts.user_id eq argValues.user_id.toInt())
            and
            (Attempts.task_id eq argValues.task_id.toInt()))
      }.toList()
      when {
        attempts.size > 1 -> http.error(500, "get more than one attempt by user_id and task_id")
        attempts.isNotEmpty() -> http.json(hashMapOf("attempt_text" to attempts.last()[Attempts.attempt_text]))
        else -> http.json(hashMapOf("attempt_text" to "[comment]: # (there was no attempt)"))
      }
    }
  }

  override fun args(): SubmissionGetArgs = SubmissionGetArgs("", "", "")
}

object MyAttempts : Table("Contest.MyAttempts") {
  val task_id = integer("task_id")
  val user_id = integer("user_id")
  val user_name = text("user_name")
  val user_nick = text("user_nick")
  val status = text("status")
  val count = integer("count")
  val error_msg = text("error_msg")
  val result_set = text("result_set")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("user_id", row[user_id])
      it.put("user_name", row[user_name])
      it.put("user_nick", row[user_nick])
      it.put("status", row[status])
      it.put("count", row[count])
      it.put("error_msg", row[error_msg])
      it.put("result_set", row[result_set])
    }
  }
}

data class SubmissionListArgs(var task_id: String) : RequestArgs()

class SubmissionListHandler : RequestHandler<SubmissionListArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionListArgs): HttpResponse {
    return transaction {
      http.json(MyAttempts.select {
        ((MyAttempts.task_id eq argValues.task_id.toInt()))
      }.map(MyAttempts::asJson).toList())
    }
  }

  override fun args(): SubmissionListArgs = SubmissionListArgs("")
}

private val DATE_FORMATTER = DateTimeFormatterBuilder()
    .appendYear(2, 4).appendLiteral('-')
    .appendMonthOfYear(2).appendLiteral('-')
    .appendDayOfMonth(2).appendLiteral(' ')
    .appendHourOfDay(2).appendLiteral(':')
    .appendMinuteOfHour(2).toFormatter()

object AttemptsByContest : Table("Contest.AttemptsByContest") {
  var contestCode = text("contest_code")
  var taskId = integer("task_id")
  var name = text("name")
  var description = text("description")
  var signature = text("signature")
  var difficulty = integer("difficulty")
  var score = integer("score")
  var variantId = integer("variant_id")
  var author = text("author_nick")
  var authorId = integer("author_id")
  var attemptId = text("attempt_id").nullable()
  var attemptUserId = integer("user_id")
  var status = text("status").nullable()
  var testingStartTs = date("testing_start_ts").nullable()
  var count = integer("count")
  var errorMsg = text("error_msg").nullable()
  var resultSet = text("result_set").nullable()

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("task_id", row[taskId])
      it.put("name", row[name])
      it.put("description", row[description])
      it.put("signature", row[signature])
      it.put("difficulty", row[difficulty])
      it.put("score", row[score])
      it.put("variant_id", row[variantId])
      it.put("author", row[author])
      it.put("author_id", row[authorId])
      it.put("attempt_id", row[attemptId])
      it.put("user_id", row[attemptUserId])
      it.put("status", row[status])
      it.put("testing_start_ts", row[testingStartTs]?.toString(DATE_FORMATTER))
      it.put("count", row[count])
      it.put("error_msg", row[errorMsg])
      it.put("result_set", row[resultSet])
    }
  }
}

data class SubmissionsByContestArgs(var contestCode: String) : RequestArgs()

class SubmissionsByContestHandler : RequestHandler<SubmissionsByContestArgs>() {
  override fun args() = SubmissionsByContestArgs("")

  override fun handle(http: HttpApi, argValues: SubmissionsByContestArgs): HttpResponse {
    val attempts = transaction {
      AttemptsByContest.select { AttemptsByContest.contestCode eq argValues.contestCode }
          .map(AttemptsByContest::asJson)
          .toList()
    }
    return http.json(attempts)
  }
}