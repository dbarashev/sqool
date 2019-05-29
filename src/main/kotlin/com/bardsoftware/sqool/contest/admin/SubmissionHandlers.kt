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

  fun asJsonUserId(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("user_id", row[Attempts.user_id])
    }
  }
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

data class SubmissionGetByTaskArgs(var task_id: String) : RequestArgs()

class SubmissionGetByTaskHandler : RequestHandler<SubmissionGetByTaskArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionGetByTaskArgs): HttpResponse {
    return transaction {
      http.json(Attempts.select {
        ((Attempts.task_id eq argValues.task_id.toInt()))
      }.map(Attempts::asJsonUserId).toList())
    }
  }

  override fun args(): SubmissionGetByTaskArgs = SubmissionGetByTaskArgs("")
}
