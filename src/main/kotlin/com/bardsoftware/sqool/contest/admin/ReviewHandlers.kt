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

import com.bardsoftware.sqool.contest.*
import com.bardsoftware.sqool.contest.storage.UserStorage
import okhttp3.*
import okhttp3.Route
import org.jetbrains.exposed.sql.*
import java.io.IOException


object SolutionReview : Table("Contest.SolutionReview") {
  val attempt_id = text("attempt_id")
  val reviewer_id = integer("reviewer_id")
  val solution_review = text("solution_review")
}

data class ReviewGetArgs(var attempt_id: String) : RequestArgs()

class ReviewListHandler : AdminHandler<ReviewGetArgs>() {
  override fun args(): ReviewGetArgs = ReviewGetArgs("")

  override fun handle(http: HttpApi, argValues: ReviewGetArgs): HttpResponse {
    return withAdminUser(http) {
      val allReviews = ReviewByUser.select {
        ReviewByUser.attempt_id eq argValues.attempt_id
      }.toList()
      println("We have ${allReviews.size} reviews for attempt=${argValues.attempt_id}")
      when {
        allReviews.isEmpty() -> http.json(listOf<String>())
        else -> http.json(allReviews.map {
          review -> mapOf(
            "review_text" to review[ReviewByUser.solution_review],
            "reviewer_name" to review[ReviewByUser.reviewer_name]
        )}.toList())
      }
    }
  }
}

class ReviewGetHandler : AdminHandler<ReviewGetArgs>() {
  override fun args(): ReviewGetArgs = ReviewGetArgs("")

  override fun handle(http: HttpApi, argValues: ReviewGetArgs) = withAdminUser(http) {
    val solutionReview = SolutionReview.select {
      (SolutionReview.reviewer_id eq it.id) and (SolutionReview.attempt_id eq argValues.attempt_id)
    }.toList()
    when {
      solutionReview.size > 1 -> http.json(mapOf(
          "review_text" to "-- Найдено несколько ваших рецензий решения ${argValues.attempt_id}. Возможно, это ошибка сервера"
      ))
      solutionReview.isNotEmpty() -> http.json(mapOf("review_text" to solutionReview.last()[SolutionReview.solution_review]))
      else -> {
        val attempts = Attempts.select { Attempts.attempt_id eq argValues.attempt_id }.toList()
        when {
          attempts.size > 1 -> http.json(mapOf(
              "review_text" to "-- Найдено несколько решений  ${argValues.attempt_id}. Возможно, это ошибка сервера"
          ))
          attempts.isNotEmpty() -> {
            val attempt = attempts.last()[Attempts.attempt_text]
            http.json(mapOf("review_text" to """
              |```
              |$attempt
              |```
            """.trimMargin()))
          }
          else -> http.json(mapOf(
              "review_text" to "-- Студенческое решение ${argValues.attempt_id} не найдено. Возможно, это ошибка сервера"
          ))
        }
      }
    }
  }
}

data class ReviewSaveArgs(var attempt_id: String, var solution_review: String) : RequestArgs()

class ReviewSaveHandler : AdminHandler<ReviewSaveArgs>() {
  override fun handle(http: HttpApi, argValues: ReviewSaveArgs) = withAdminUser(http) { admin ->
    val solutionReview = SolutionReview.select {
      (SolutionReview.reviewer_id eq admin.id) and (SolutionReview.attempt_id eq argValues.attempt_id)
    }.toList()
    if (solutionReview.isEmpty()) {
      SolutionReview.insert {
        it[attempt_id] = argValues.attempt_id
        it[reviewer_id] = admin.id
        it[solution_review] = argValues.solution_review
      }
    } else {
      SolutionReview.update({
        (SolutionReview.reviewer_id eq admin.id) and (SolutionReview.attempt_id eq argValues.attempt_id)
      }) {
        it[solution_review] = argValues.solution_review
      }
    }
    http.ok()
  }

  override fun args(): ReviewSaveArgs = ReviewSaveArgs("", "")
}

data class ReviewEmailArgs(var contest_code: String, var user_id: String, var attempt_id: String) : RequestArgs()
class ReviewEmailHandler(val apiKey: String) : AdminHandler<ReviewEmailArgs>() {
  private val httpClient = OkHttpClient.Builder().authenticator(object : Authenticator {
    @Throws(IOException::class)
    override fun authenticate(route: Route?, response: Response): Request? {
      if (response.request.header("Authorization") != null) {
        return null // Give up, we've already attempted to authenticate.
      }
      return response.request.newBuilder()
          .header("Authorization", Credentials.basic("api", apiKey))
          .build()
    }
  }).build()

  override fun args(): ReviewEmailArgs = ReviewEmailArgs("", "", "")

  override fun handle(http: HttpApi, argValues: ReviewEmailArgs): HttpResponse {
    return withAdminUser(http) {
      val email = UserStorage.exec<String?> {
        val recipient = this.findUserById(argValues.user_id.toInt()) ?: return@exec null
        recipient.email
      } ?: return@withAdminUser http.error(404, "User ${argValues.user_id} not found")
      if (email == "") {
        return@withAdminUser http.error(404, "Email is unknown for user ${argValues.user_id}")
      }

      val query = if (argValues.attempt_id == "") ReviewByUser.select {
        (ReviewByUser.user_id eq argValues.user_id.toInt()) and (ReviewByUser.contest_code eq argValues.contest_code)
      } else ReviewByUser.select {
        ReviewByUser.attempt_id eq argValues.attempt_id
      }

      val contestName = Contests.select { Contests.code eq argValues.contest_code }.firstOrNull()?.let {
        it[Contests.name]
      } ?: ""
      val markdown = query.orderBy(ReviewByUser.task_id).map {
        """
### Задача ${it[ReviewByUser.task_name]}

_рецензировал ${it[ReviewByUser.reviewer_name]}_

${it[ReviewByUser.solution_review]} 

        """
      }.joinToString(
        separator = """

----

"""
      )

      println("Sending email to $email")
      sendEmail(markdown, mapOf(
          "from" to "DBMS Class <dbms@mg.barashev.net>",
          "to" to email,
          "bcc" to "dbms+review@barashev.net",
          "subject" to "Рецензии на задачи контеста $contestName",
          "h:Reply-To" to "dbms@barashev.net"
      ))
      http.ok()
    }
  }
}

