/*
 * Copyright (c) BarD Software s.r.o 2019
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

package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.RequestArgs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*

private val JSON_MAPPER = ObjectMapper()

object Attempts : Table("Contest.Attempt") {
  val attempt_id = text("attempt_id").nullable()
  val task_id = integer("task_id")
  val variant_id = integer("variant_id")
  val contest_code = text("contest_code")
  val user_id = integer("user_id")
  val attempt_text = text("attempt_text")
}

data class SubmissionGetArgs(var attempt_id: String) : RequestArgs()

class SubmissionGetHandler : AdminHandler<SubmissionGetArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionGetArgs) = withAdminUser(http) {
    val attempts = Attempts.select { Attempts.attempt_id eq argValues.attempt_id }.toList()
    when {
      attempts.size > 1 -> http.error(500, "get more than one attempt by user_id, task_id, variant_id and contest_code")
      attempts.isNotEmpty() -> http.json(hashMapOf("attempt_text" to attempts.last()[Attempts.attempt_text]))
      else -> http.json(hashMapOf("attempt_text" to "[comment]: # (there was no attempt)"))
    }
  }

  override fun args(): SubmissionGetArgs = SubmissionGetArgs("")
}

object MyAttempts : Table("Contest.MyAttempts") {
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
  var attemptUserUni = text("user_uni")
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
      it.put("user_name", row[attemptUserName])
      it.put("status", row[status])
      it.put("testing_start_ts", row[testingStartTs]?.millis)
      it.put("count", row[count])
      it.put("error_msg", row[errorMsg])
      it.put("result_set", row[resultSet])
    }
  }
}

data class SubmissionListArgs(var task_id: String) : RequestArgs()

class SubmissionListHandler : AdminHandler<SubmissionListArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionListArgs) = withAdminUser(http) {
    http.json(MyAttempts.select {
      ((MyAttempts.taskId eq argValues.task_id.toInt()))
    }.map(MyAttempts::asJson).toList())
  }

  override fun args(): SubmissionListArgs = SubmissionListArgs("")
}

data class UserSubmissionsByContestArgs(var contestCode: String, var userId: String) : RequestArgs()

class UserSubmissionsByContestHandler : AdminHandler<UserSubmissionsByContestArgs>() {
  override fun args() = UserSubmissionsByContestArgs("", "")

  override fun handle(http: HttpApi, argValues: UserSubmissionsByContestArgs) = withAdminUser(http) {
    val attempts = MyAttempts.select {
      (MyAttempts.contestCode eq argValues.contestCode) and (MyAttempts.attemptUserId eq argValues.userId.toInt())
    }.map(MyAttempts::asJson).toList()
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
    val users = MyAttempts.slice(MyAttempts.contestCode, MyAttempts.attemptUserId, MyAttempts.attemptUserName, MyAttempts.attemptUserUni)
        .select { MyAttempts.contestCode eq argValues.contestCode }.andWhere { MyAttempts.status neq "virgin" }
        .withDistinct()
        .orderBy(MyAttempts.attemptUserUni to SortOrder.ASC, MyAttempts.attemptUserName to SortOrder.ASC)
        .map {
          mapOf(
              "user_id" to it[MyAttempts.attemptUserId],
              "user_name" to it[MyAttempts.attemptUserName],
              "uni" to it[MyAttempts.attemptUserUni]
          )
        }.toList()
    http.json(users)
  }
}
