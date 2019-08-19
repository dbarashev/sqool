package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.admin.SolutionReview
import com.bardsoftware.sqool.contest.storage.AvailableContests
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

abstract class DashboardHandler<T : RequestArgs> : RequestHandler<T>() {
  protected fun withUser(http: HttpApi, handle: (User) -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: return redirectToLogin(http)
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec redirectToLogin(http)
      handle(user)
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

data class ContestAcceptArgs(var contest_code: String, var variant_id: String) : RequestArgs()

class ContestAcceptHandler : DashboardHandler<ContestAcceptArgs>() {
  override fun args() = ContestAcceptArgs("", "")

  override fun handle(http: HttpApi, argValues: ContestAcceptArgs) = withUser(http) { user ->
    val rowUserContest = AvailableContests.select {
      (AvailableContests.contest_code eq argValues.contest_code) and (AvailableContests.user_id eq user.id)
    }.toList()

    if (rowUserContest.size > 1) {
      http.error(500, """Too many records for (${argValues.contest_code}, ${user.id}) 
        |returned from AvailableContests (${rowUserContest.size} records in the result""".trimMargin())
    } else {
      rowUserContest.firstOrNull()?.let {
        val variantChoice = it[AvailableContests.variant_choice]
        val selectedVariant = argValues.variant_id
        if (selectedVariant != "") {
          if (variantChoice == Contests.VariantChoice.ANY) {
            user.assignVariant(argValues.contest_code, selectedVariant.toInt())
            return@let http.ok()
          } else {
            return@let http.error(400, "Variant can't be chosen by client in contest ${argValues.contest_code}")
          }
        }

        val assignedVariant = it[AvailableContests.assigned_variant_id]
        if (assignedVariant != null) {
          return@let http.ok()
        }

        when (variantChoice) {
          Contests.VariantChoice.ANY -> http.error(400, "No variant chosen in contest ${argValues.contest_code}")

          Contests.VariantChoice.RANDOM -> http.ok().also { user.assignRandomVariant(argValues.contest_code) }
        }
      } ?: http.error(404, "No available contest with code ${argValues.contest_code}")
    }
  }
}

data class ContestAttemptsArgs(var contest_code: String) : RequestArgs()

class ContestAttemptsHandler : DashboardHandler<ContestAttemptsArgs>() {
  override fun args() = ContestAttemptsArgs("")

  override fun handle(http: HttpApi, argValues: ContestAttemptsArgs) = withUser(http) { user ->
    val rowUserContest = AvailableContests.select {
      (AvailableContests.contest_code eq argValues.contest_code) and (AvailableContests.user_id eq user.id)
    }.toList()

    if (rowUserContest.size > 1) {
      http.error(500, """Too many records for (${argValues.contest_code}, ${user.id}) 
        |returned from AvailableContests (${rowUserContest.size} records in the result""".trimMargin())
    } else {
      rowUserContest.firstOrNull()?.let {
        val assignedVariant = it[AvailableContests.assigned_variant_id]
        if (assignedVariant != null) {
          return@let http.json(user.getVariantAttempts(assignedVariant))
        }
        http.error(400, "No variant chosen")
      } ?: http.error(404, "No available contest with code ${argValues.contest_code}")
    }
  }
}

data class ReviewGetArgs(var contest_code: String, var task_id: String) : RequestArgs()

class ReviewGetHandler : DashboardHandler<ReviewGetArgs>() {
  override fun args() = ReviewGetArgs("", "")

  override fun handle(http: HttpApi, argValues: ReviewGetArgs) = withUser(http) { user ->
    val rowUserContest = AvailableContests.select {
      (AvailableContests.contest_code eq argValues.contest_code) and (AvailableContests.user_id eq user.id)
    }.toList()

    if (rowUserContest.size > 1) {
      http.error(500, """Too many records for (${argValues.contest_code}, ${user.id}) 
        |returned from AvailableContests (${rowUserContest.size} records in the result""".trimMargin())
    } else {
      rowUserContest.firstOrNull()?.let {
        val assignedVariant = it[AvailableContests.assigned_variant_id] ?: return@withUser http.error(400, "No variant chosen")
        val reviewRow = SolutionReview.select {
          (SolutionReview.user_id eq user.id) and
          (SolutionReview.task_id eq argValues.task_id.toInt()) and
          (SolutionReview.variant_id eq assignedVariant)
        }.toList()
        val reviews = reviewRow.joinToString("\n\n") { row -> row[SolutionReview.solution_review] }
        http.json(reviews)
      } ?: http.error(404, "No available contest with code ${argValues.contest_code}")
    }
  }
}