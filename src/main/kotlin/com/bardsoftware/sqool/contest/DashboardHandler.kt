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

package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.admin.Variants
import com.bardsoftware.sqool.contest.storage.AvailableContests
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

fun withUser(http: HttpApi, userName: String? = http.session("name"), userEmail: String? = http.session("email"), handle: (User) -> HttpResponse): HttpResponse {
  //if (userName == null) return redirectToLogin(http)
  return UserStorage.exec {
    val user = findUser(userName ?: "", userEmail ?: "") ?: return@exec redirectToLogin(http)
    handle(user)
  }
}

data class UserGetArgs(
    var id: String, var forceCreate: String, var email: String, var displayName: String
) : RequestArgs()

class UserGetHandler() : RequestHandler<UserGetArgs>() {
  override fun args() = UserGetArgs("", "", "false", "")

  override fun handle(http: HttpApi, argValues: UserGetArgs): HttpResponse {
    val user = UserStorage.exec {
      findUser("", argValues.email)
    }
    return securityHeaders(http) {
      val http = this
      if (user != null) {
        http.session("name", argValues.id)
        http.session("email", argValues.email)
        http.ok()
      } else {
        if (argValues.forceCreate.toBoolean()) {
          UserStorage.exec {
            createUser(argValues.displayName, "", argValues.email).let {
              http.session("name", argValues.id)
              http.session("email", argValues.email)
              http.ok()
            }
          }
        } else {
          http.error(404)
        }
      }
    }
  }
}



abstract class DashboardHandler<T : RequestArgs> : RequestHandler<T>() {
  protected fun withUserContest(http: HttpApi, contestCode: String, handle: (User, ResultRow) -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: ""
    val email = http.session("email") ?: ""
    return UserStorage.exec {
      val user = findUser(userName, email) ?: return@exec redirectToLogin(http)
      val rowUserContest = AvailableContests.select {
        (AvailableContests.contest_code eq contestCode) and (AvailableContests.user_id eq user.id)
      }.toList()
      if (rowUserContest.size > 1) {
        return@exec http.error(500, """Too many records for ($contestCode, ${user.id}) 
          |returned from AvailableContests (${rowUserContest.size} records in the result""".trimMargin())
      }
      rowUserContest.firstOrNull()?.let { handle(user, it) }
          ?: http.error(404, "No available contest with code $contestCode")
    }
  }
}

class DashboardPageHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withUser(http) {
    http.render("me2.ftl", mapOf("userName" to it.name))
  }
}

class AvailableContestAllHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withUser(http) {
    http.json(it.availableContests())
  }
}

class ContestRecentHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withUser(http) { user ->
    user.recentContest()?.let { http.json(it) } ?: http.ok()
  }
}

data class ContestAcceptResponse(val id: Int, val name: String)
data class ContestAcceptArgs(var contest_code: String, var variant_id: String) : RequestArgs()

class ContestAcceptHandler : DashboardHandler<ContestAcceptArgs>() {
  override fun args() = ContestAcceptArgs("", "")

  override fun handle(http: HttpApi, argValues: ContestAcceptArgs) = withUserContest(http, argValues.contest_code) { user, rowUserContest ->
    val variantChoice = rowUserContest[AvailableContests.variant_choice]
    val selectedVariant = argValues.variant_id
    if (selectedVariant != "") {
      return@withUserContest if (variantChoice == Contests.VariantChoice.ANY) {
        user.assignVariant(argValues.contest_code, selectedVariant.toInt())
        getResponse(selectedVariant.toInt())?.let {
          http.json(it)
        } ?: http.error(400, "Variant $selectedVariant not found")
      } else {
        http.error(400, "Variant can't be chosen by client in contest ${argValues.contest_code}")
      }
    }

    val assignedVariant = rowUserContest[AvailableContests.assigned_variant_id]
    if (assignedVariant != null) {
      return@withUserContest getResponse(assignedVariant)?.let {
        http.json(it)
      } ?: http.error(500, "Can't locate assigned variant $assignedVariant")
    }

    when (variantChoice) {
      Contests.VariantChoice.ANY -> http.error(400, "No variant chosen in contest ${argValues.contest_code}")

      Contests.VariantChoice.RANDOM -> {
        val variantId = user.assignRandomVariant(argValues.contest_code)
        getResponse(variantId)?.let {
          http.json(it)
        } ?: http.error(500, "Can't assign random varian")
      }
    }
  }
}
private fun getResponse(variantId: Int): ContestAcceptResponse? {
  return Variants.select { Variants.id eq variantId }
      .map { ContestAcceptResponse(it[Variants.id], it[Variants.name]) }
      .firstOrNull()
}

data class ContestAttemptsArgs(var contest_code: String) : RequestArgs()

class ContestAttemptsHandler : DashboardHandler<ContestAttemptsArgs>() {
  override fun args() = ContestAttemptsArgs("")

  override fun handle(http: HttpApi, argValues: ContestAttemptsArgs) = withUserContest(http, argValues.contest_code) { user, rowUserContest ->
    val assignedVariant = rowUserContest[AvailableContests.assigned_variant_id]
    if (assignedVariant != null) {
      return@withUserContest http.json(user.getVariantAttempts(assignedVariant, argValues.contest_code))
    }
    http.error(400, "No variant chosen")
  }
}

object ReviewByUser : Table("Contest.ReviewByUser") {
  val attempt_id = text("attempt_id")
  val user_id = integer("user_id")
  val solution_review = text("solution_review")
  val contest_code = text("contest_code")
  val variant_id = integer("variant_id")
  val task_id = integer("task_id")
  val task_name = text("task_name")
  val reviewer_name = text("reviewer_name")
}

data class ReviewGetArgs(var attempt_id: String) : RequestArgs()

class ReviewGetHandler : DashboardHandler<ReviewGetArgs>() {
  override fun args() = ReviewGetArgs("")

  override fun handle(http: HttpApi, argValues: ReviewGetArgs) = withUser(http) { user ->
    val reviewRow = ReviewByUser.select {
      (ReviewByUser.user_id eq user.id) and (ReviewByUser.attempt_id eq argValues.attempt_id)
    }.toList()
    val reviews = reviewRow.joinToString("\n\n") { row -> row[ReviewByUser.solution_review] }
    http.json(reviews)
  }
}
