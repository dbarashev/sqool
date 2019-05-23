package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object SolutionReview : Table("Contest.SolutionReview") {
  val task_id = integer("task_id")
  val user_id = integer("user_id")
  val reviewer_id = integer("reviewer_id")
  val solution_review = text("solution_review")
}

data class ReviewGetArgs(var user_id: String, var task_id: String) : RequestArgs()

class ReviewGetHandler : RequestHandler<ReviewGetArgs>() {
  override fun handle(http: HttpApi, argValues: ReviewGetArgs): HttpResponse {
    return transaction {
      val reviewerId = http.session("user_id");
      if (reviewerId == null) {
        http.error(401, "no authorization");
      } else {
        val solutionReview = SolutionReview.select {
          ((SolutionReview.user_id eq argValues.user_id.toInt())
                  and
                  (SolutionReview.task_id eq argValues.task_id.toInt())
                  and
                  (SolutionReview.reviewer_id eq reviewerId.toInt()))
        }.toList()
        when {
          solutionReview.size > 1 -> http.error(500, "get more than one solution by user_id, task_id and reviewer_id")
          solutionReview.isNotEmpty() -> http.json(hashMapOf("review_text" to solutionReview.last()[SolutionReview.solution_review]))
          else -> http.json(hashMapOf("review_text" to "[comment]: # (there was no review)"))
        }
      }
    }
  }

  override fun args(): ReviewGetArgs = ReviewGetArgs("", "")
}

data class ReviewSaveArgs(var user_id: String, var task_id: String, var solution_review: String) : RequestArgs()

class ReviewSaveHandler : RequestHandler<ReviewSaveArgs>() {
  override fun handle(http: HttpApi, argValues: ReviewSaveArgs): HttpResponse {
    return transaction {
      val reviewerId = http.session("user_id");
      if (reviewerId == null) {
        http.error(401, "no authorization");
      } else {
        val solutionReview = SolutionReview.select {
          ((SolutionReview.user_id eq argValues.user_id.toInt())
          and
          (SolutionReview.task_id eq argValues.task_id.toInt())
          and
          (SolutionReview.reviewer_id eq reviewerId.toInt()))
        }.toList()
        if (solutionReview.isEmpty()) {
          SolutionReview.insert {
            it[task_id] = argValues.task_id.toInt()
            it[user_id] = argValues.user_id.toInt()
            it[reviewer_id] = reviewerId.toInt()
            it[solution_review] = argValues.solution_review
          }
        } else {
          SolutionReview.update({
            ((SolutionReview.user_id eq argValues.user_id.toInt())
                    and
                    (SolutionReview.task_id eq argValues.task_id.toInt())
                    and
                    (SolutionReview.reviewer_id eq reviewerId.toInt()))
          }) {
            it[solution_review] = argValues.solution_review
          }
        }
      }
      http.ok();
    }
  }

  override fun args(): ReviewSaveArgs = ReviewSaveArgs("", "", "")
}
