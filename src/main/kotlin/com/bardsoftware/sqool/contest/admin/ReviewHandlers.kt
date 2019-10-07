package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.RequestArgs
import org.jetbrains.exposed.sql.*

object SolutionReview : Table("Contest.SolutionReview") {
  val attempt_id = text("attempt_id")
  val reviewer_id = integer("reviewer_id")
  val solution_review = text("solution_review")
}

data class ReviewGetArgs(var attempt_id: String) : RequestArgs()

class ReviewGetHandler : AdminHandler<ReviewGetArgs>() {
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
              ```
              $attempt
              ```
            """.trimIndent()))
          }
          else -> http.json(mapOf(
              "review_text" to "-- Студенческое решение ${argValues.attempt_id} не найдено. Возможно, это ошибка сервера"
          ))
        }
      }
    }
  }

  override fun args(): ReviewGetArgs = ReviewGetArgs("")
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
