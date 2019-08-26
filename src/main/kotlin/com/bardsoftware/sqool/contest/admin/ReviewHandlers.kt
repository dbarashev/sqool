package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.RequestArgs
import org.jetbrains.exposed.sql.*

object SolutionReview : Table("Contest.SolutionReview") {
  val task_id = integer("task_id")
  val variant_id = integer("variant_id")
  val user_id = integer("user_id")
  val reviewer_id = integer("reviewer_id")
  val solution_review = text("solution_review")
}

data class ReviewGetArgs(var user_id: String, var task_id: String, var variant_id: String) : RequestArgs()

class ReviewGetHandler : AdminHandler<ReviewGetArgs>() {
  override fun handle(http: HttpApi, argValues: ReviewGetArgs) = withAdminUser(http) {
    val solutionReview = SolutionReview.select {
      (SolutionReview.user_id eq argValues.user_id.toInt()) and
          (SolutionReview.task_id eq argValues.task_id.toInt()) and
          (SolutionReview.reviewer_id eq reviewerId.toInt()) and
          (SolutionReview.variant_id eq argValues.variant_id.toInt())
    }.toList()
    when {
      solutionReview.size > 1 -> http.error(500, "get more than one solution by user_id, task_id, variant_id and reviewer_id")
      solutionReview.isNotEmpty() -> http.json(hashMapOf("review_text" to solutionReview.last()[SolutionReview.solution_review]))
      else -> http.json(hashMapOf("review_text" to "[comment]: # (there was no review)"))
    }
  }

  override fun args(): ReviewGetArgs = ReviewGetArgs("", "", "")
}

data class ReviewSaveArgs(var user_id: String, var task_id: String, var variant_id: String, var solution_review: String) : RequestArgs()

class ReviewSaveHandler : AdminHandler<ReviewSaveArgs>() {
  override fun handle(http: HttpApi, argValues: ReviewSaveArgs) = withAdminUser(http) {
    val solutionReview = SolutionReview.select {
      (SolutionReview.user_id eq argValues.user_id.toInt()) and
      (SolutionReview.task_id eq argValues.task_id.toInt()) and
      (SolutionReview.reviewer_id eq reviewerId.toInt()) and
      (SolutionReview.variant_id eq argValues.variant_id.toInt())
    }.toList()
    if (solutionReview.isEmpty()) {
      SolutionReview.insert {
        it[task_id] = argValues.task_id.toInt()
        it[variant_id] = argValues.variant_id.toInt()
        it[user_id] = argValues.user_id.toInt()
        it[reviewer_id] = reviewerId.toInt()
        it[solution_review] = argValues.solution_review
      }
    } else {
      SolutionReview.update({
        (SolutionReview.user_id eq argValues.user_id.toInt()) and
        (SolutionReview.task_id eq argValues.task_id.toInt()) and
        (SolutionReview.reviewer_id eq reviewerId.toInt()) and
        (SolutionReview.variant_id eq argValues.variant_id.toInt())
      }) {
        it[solution_review] = argValues.solution_review
      }
    }
    http.ok()
  }

  override fun args(): ReviewSaveArgs = ReviewSaveArgs("", "", "", "")
}
