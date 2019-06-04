package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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
      it.put("user_id", row[MyAttempts.user_id])
      it.put("user_name", row[MyAttempts.user_name])
      it.put("user_nick", row[MyAttempts.user_nick])
      it.put("status", row[MyAttempts.status])
      it.put("count", row[MyAttempts.count])
      it.put("error_msg", row[MyAttempts.error_msg])
      it.put("result_set", row[MyAttempts.result_set])
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
