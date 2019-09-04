package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.admin.SolutionReview
import com.bardsoftware.sqool.contest.storage.AvailableContests
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.jetbrains.exposed.sql.ResultRow
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

  protected fun withUserContest(http: HttpApi, contestCode: String, handle: (User, ResultRow) -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: return redirectToLogin(http)
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec redirectToLogin(http)
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

data class ContestAcceptArgs(var contest_code: String, var variant_id: String) : RequestArgs()

class ContestAcceptHandler : DashboardHandler<ContestAcceptArgs>() {
  override fun args() = ContestAcceptArgs("", "")

  override fun handle(http: HttpApi, argValues: ContestAcceptArgs) = withUserContest(http, argValues.contest_code) { user, rowUserContest ->
    val variantChoice = rowUserContest[AvailableContests.variant_choice]
    val selectedVariant = argValues.variant_id
    if (selectedVariant != "") {
      return@withUserContest if (variantChoice == Contests.VariantChoice.ANY) {
        user.assignVariant(argValues.contest_code, selectedVariant.toInt())
        http.ok()
      } else {
        http.error(400, "Variant can't be chosen by client in contest ${argValues.contest_code}")
      }
    }

    val assignedVariant = rowUserContest[AvailableContests.assigned_variant_id]
    if (assignedVariant != null) {
      return@withUserContest http.ok()
    }

    when (variantChoice) {
      Contests.VariantChoice.ANY -> http.error(400, "No variant chosen in contest ${argValues.contest_code}")

      Contests.VariantChoice.RANDOM -> http.ok().also { user.assignRandomVariant(argValues.contest_code) }
    }
  }
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

data class ReviewGetArgs(var contest_code: String, var task_id: String) : RequestArgs()

class ReviewGetHandler : DashboardHandler<ReviewGetArgs>() {
  override fun args() = ReviewGetArgs("", "")

  override fun handle(http: HttpApi, argValues: ReviewGetArgs) = withUserContest(http, argValues.contest_code) { user, rowUserContest ->
    val assignedVariant = rowUserContest[AvailableContests.assigned_variant_id]
        ?: return@withUserContest http.error(400, "No variant chosen")
    val reviewRow = SolutionReview.select {
      (SolutionReview.user_id eq user.id) and
      (SolutionReview.task_id eq argValues.task_id.toInt()) and
      (SolutionReview.variant_id eq assignedVariant) and
      (SolutionReview.contest_code eq argValues.contest_code)
    }.toList()
    val reviews = reviewRow.joinToString("\n\n") { row -> row[SolutionReview.solution_review] }
    http.json(reviews)
  }
}