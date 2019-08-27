package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.RequestArgs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

private val JSON_MAPPER = ObjectMapper()

object Attempts : Table("Contest.Attempt") {
  val task_id = integer("task_id")
  val variant_id = integer("variant_id")
  val user_id = integer("user_id")
  val attempt_text = text("attempt_text")
}

data class SubmissionGetArgs(var user_id: String, var task_id: String, var variant_id: String, var reviewer_id: String) : RequestArgs()

class SubmissionGetHandler : AdminHandler<SubmissionGetArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionGetArgs) = withAdminUser(http) {
    val attempts = Attempts.select {
      (Attempts.user_id eq argValues.user_id.toInt()) and
          (Attempts.task_id eq argValues.task_id.toInt()) and
          (Attempts.variant_id eq argValues.variant_id.toInt())
    }.toList()
    when {
      attempts.size > 1 -> http.error(500, "get more than one attempt by user_id, task_id and variant_id")
      attempts.isNotEmpty() -> http.json(hashMapOf("attempt_text" to attempts.last()[Attempts.attempt_text]))
      else -> http.json(hashMapOf("attempt_text" to "[comment]: # (there was no attempt)"))
    }
  }

  override fun args(): SubmissionGetArgs = SubmissionGetArgs("", "", "", "")
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

class SubmissionListHandler : AdminHandler<SubmissionListArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionListArgs) = withAdminUser(http) {
    http.json(MyAttempts.select {
      ((MyAttempts.task_id eq argValues.task_id.toInt()))
    }.map(MyAttempts::asJson).toList())
  }

  override fun args(): SubmissionListArgs = SubmissionListArgs("")
}

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
  var attemptUserName = text("user_name")
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
      it.put("attempt_id", row[attemptId])
      it.put("user_id", row[attemptUserId])
      it.put("status", row[status])
      it.put("testing_start_ts", row[testingStartTs]?.let { dateTime ->
        val time = PrettyTime(dateTime.toDate())
        time.format(Date(0))
      })
      it.put("count", row[count])
      it.put("error_msg", row[errorMsg])
      it.put("result_set", row[resultSet])
    }
  }
}

data class UserSubmissionsByContestArgs(var contestCode: String, var userId: String) : RequestArgs()

class UserSubmissionsByContestHandler : AdminHandler<UserSubmissionsByContestArgs>() {
  override fun args() = UserSubmissionsByContestArgs("", "")

  override fun handle(http: HttpApi, argValues: UserSubmissionsByContestArgs) = withAdminUser(http) {
    val attempts = AttemptsByContest.select {
      (AttemptsByContest.contestCode eq argValues.contestCode) and (AttemptsByContest.attemptUserId eq argValues.userId.toInt())
    }.map(AttemptsByContest::asJson).toList()
    http.json(attempts)
  }
}

object TaskSubmissionsStats : Table("Contest.TaskSubmissionsStats") {
  var taskId = integer("task_id")
  var taskName = text("task_name")
  var contestCode = text("contest_code")
  var solved = integer("solved")
  var attempted = integer("attempted")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("task_id", row[taskId])
      it.put("task_name", row[taskName])
      it.put("contest_code", row[contestCode])
      it.put("solved", row[solved])
      it.put("attempted", row[attempted])
    }
  }
}

data class TaskSubmissionsStatsByContestArgs(var contestCode: String) : RequestArgs()

class TaskSubmissionsStatsByContestHandler : AdminHandler<TaskSubmissionsStatsByContestArgs>() {
  override fun args() = TaskSubmissionsStatsByContestArgs("")

  override fun handle(http: HttpApi, argValues: TaskSubmissionsStatsByContestArgs) = withAdminUser(http) {
    val attempts = TaskSubmissionsStats.select { TaskSubmissionsStats.contestCode eq argValues.contestCode }
        .map(TaskSubmissionsStats::asJson)
        .toList()
    http.json(attempts)
  }
}

data class ContestUsersArgs(var contestCode: String) : RequestArgs()

class ContestUsersHandler : AdminHandler<ContestUsersArgs>() {
  override fun args() = ContestUsersArgs("")

  override fun handle(http: HttpApi, argValues: ContestUsersArgs) = withAdminUser(http) {
    val users = AttemptsByContest.slice(AttemptsByContest.contestCode, AttemptsByContest.attemptUserId, AttemptsByContest.attemptUserName)
        .select { AttemptsByContest.contestCode eq argValues.contestCode }
        .withDistinct()
        .map {
          mapOf(
              "user_id" to it[AttemptsByContest.attemptUserId],
              "user_name" to it[AttemptsByContest.attemptUserName]
          )
        }.toList()
    http.json(users)
  }
}