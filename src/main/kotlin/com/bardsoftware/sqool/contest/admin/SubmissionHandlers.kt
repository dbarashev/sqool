package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object Attempts : Table("Contest.Attempt") {
  val task_id = integer("task_id")
  val user_id = integer("user_id")
  val attempt_text = text("attempt_text")
}

data class SubmissionGetArgs(var user_id: String, var task_id: String, var reviewer_id: String) : RequestArgs()

class SubmissionGetHandler : RequestHandler<SubmissionGetArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionGetArgs): HttpResponse {
    return transaction {
      val solutionReview = SolutionReview.select {
        ((SolutionReview.user_id eq argValues.user_id.toInt())
                and
                (SolutionReview.task_id eq argValues.task_id.toInt())
                and
                (SolutionReview.reviewer_id eq argValues.reviewer_id.toInt()))
      }.toList()
      val attempts = Attempts.select {
        ((Attempts.user_id eq argValues.user_id.toInt())
        and
        (Attempts.task_id eq argValues.task_id.toInt()))
      }.toList()
      when {
          solutionReview.size > 1 -> http.error(500, "get more than one solution by user_id, task_id and reviewer_id")
          attempts.size > 1 -> http.error(500, "get more than one attempt by user_id and task_id")
          solutionReview.isNotEmpty() -> http.json(hashMapOf("attempt_text" to solutionReview.last()[SolutionReview.solution_review]))
          attempts.isNotEmpty() -> http.json(hashMapOf("attempt_text" to attempts.last()[Attempts.attempt_text]))
          else -> http.json(hashMapOf("attempt_text" to "[comment]: # (there was no attempt)"))
      }
    }
  }

  override fun args(): SubmissionGetArgs = SubmissionGetArgs("", "", "")
}

object SolutionReview : Table("Contest.SolutionReview") {
  val task_id = integer("task_id")
  val user_id = integer("user_id")
  val reviewer_id = integer("reviewer_id")
  val solution_review = text("solution_review")
}

data class SubmissionSaveArgs(var user_id: String, var task_id: String, var reviewer_id: String, var solution_review: String) : RequestArgs()

class SubmissionSaveHandler : RequestHandler<SubmissionSaveArgs>() {
  override fun handle(http: HttpApi, argValues: SubmissionSaveArgs): HttpResponse {
    return transaction {
      val solutionReview = SolutionReview.select {
        ((SolutionReview.user_id eq argValues.user_id.toInt())
        and
        (SolutionReview.task_id eq argValues.task_id.toInt())
        and
        (SolutionReview.reviewer_id eq argValues.reviewer_id.toInt()))
      }.toList()
      if (solutionReview.isEmpty()) {
        SolutionReview.insert {
          it[task_id] = argValues.task_id.toInt()
          it[user_id] = argValues.user_id.toInt()
          it[reviewer_id] = argValues.reviewer_id.toInt()
          it[solution_review] = argValues.solution_review
        }
      } else {
        SolutionReview.update ({
          ((SolutionReview.user_id eq argValues.user_id.toInt())
          and
          (SolutionReview.task_id eq argValues.task_id.toInt())
          and
          (SolutionReview.reviewer_id eq argValues.reviewer_id.toInt()))
        }) {
          it[solution_review] = argValues.solution_review
        }
      }
      http.ok();
    }
  }

  override fun args(): SubmissionSaveArgs = SubmissionSaveArgs("", "", "", "")
}

