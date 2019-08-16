package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.storage.AvailableContests
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

fun redirectToLogin(http: HttpApi) = http.chain {
  clearSession()
  redirect("/login")
}

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
    AvailableContests.select { (AvailableContests.contest_code eq argValues.contest_code) and (AvailableContests.user_id eq user.id) }
        .map {
          val variantChoice = it[AvailableContests.variant_choice]
          val selectedVariant = argValues.variant_id
          if (selectedVariant != "" && variantChoice == Contests.VariantChoice.ANY) {
            user.assignVariant(argValues.contest_code, selectedVariant.toInt())
            return@map http.ok()
          }
          if (selectedVariant != "" && variantChoice != Contests.VariantChoice.ANY) {
            return@map http.error(400, "Variant can't be chosen by client")
          }

          val assignedVariant = it[AvailableContests.assigned_variant_id]
          if (assignedVariant != null) {
            return@map http.ok()
          }

          when (variantChoice) {
            Contests.VariantChoice.ANY -> http.error(400, "No variant chosen")

            Contests.VariantChoice.RANDOM -> http.ok().also { user.assignRandomVariant(argValues.contest_code) }
          }
        }.firstOrNull() ?: http.error(404, "No available contest with code ${argValues.contest_code}")
  }
}

data class ContestAttemptsArgs(var contest_code: String) : RequestArgs()

class ContestAttemptsHandler : DashboardHandler<ContestAttemptsArgs>() {
  override fun args() = ContestAttemptsArgs("")

  override fun handle(http: HttpApi, argValues: ContestAttemptsArgs) = withUser(http) { user ->
    println("Searching for attempts of user ${user.id} in contest ${argValues.contest_code}")
    AvailableContests.select {(AvailableContests.contest_code eq argValues.contest_code) and (AvailableContests.user_id eq user.id) }
        .map {
          val assignedVariant = it[AvailableContests.assigned_variant_id]
          println("User is assigned variant $assignedVariant")
          if (assignedVariant != null) {
            return@map http.json(user.getVariantAttempts(assignedVariant))
          }
          http.error(400, "No variant chosen")
        }.firstOrNull() ?: http.error(404, "No available contest with code ${argValues.contest_code}")
  }
}

class LogoutHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = redirectToLogin(http)
}
