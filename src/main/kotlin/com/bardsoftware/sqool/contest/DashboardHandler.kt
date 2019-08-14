package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.storage.AvailableContests
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.jetbrains.exposed.sql.select

abstract class DashboardHandler<T : RequestArgs> : RequestHandler<T>() {
  protected fun withUser(http: HttpApi, handle: User.() -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: return redirectToLogin(http)
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec redirectToLogin(http)
      user.handle()
    }
  }

  protected fun redirectToLogin(http: HttpApi) = http.chain {
    clearSession()
    redirect("/login")
  }
}

class DashboardPageHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withUser(http) {
    http.render("me2.ftl", mapOf("userName" to name))
  }
}

class AvailableContestAllHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withUser(http) {
    http.json(availableContests())
  }
}

data class VariantAcceptArgs(var contest_code: String, var variant_id: String) : RequestArgs()

class VariantAcceptHandler : DashboardHandler<VariantAcceptArgs>() {
  override fun args() = VariantAcceptArgs("", "")

  override fun handle(http: HttpApi, argValues: VariantAcceptArgs) = withUser(http) {
    AvailableContests.select { AvailableContests.contest_code eq argValues.contest_code }
        .map {
          val variantChoice = it[AvailableContests.variant_choice]
          if (variantChoice == Contests.VariantChoice.ANY) {
            assignVariant(argValues.contest_code, argValues.variant_id.toInt())
            http.ok()
          } else {
            http.error(400, "Variant can't be chosen by client")
          }
        }.firstOrNull() ?: http.error(404, "No contest with code ${argValues.contest_code}")
  }
}

data class ContestAcceptArgs(var contest_code: String) : RequestArgs()

class ContestAcceptHandler : DashboardHandler<ContestAcceptArgs>() {
  override fun args() = ContestAcceptArgs("")

  override fun handle(http: HttpApi, argValues: ContestAcceptArgs) = withUser(http) {
    AvailableContests.select { AvailableContests.contest_code eq argValues.contest_code }
        .map {
          val variantId = it[AvailableContests.variant_id]
          val variantChoice = it[AvailableContests.variant_choice]
          if (variantId != null) {
            acceptVariant(variantId)
            return@map http.ok()
          }

          when (variantChoice) {
            Contests.VariantChoice.ANY -> http.error(400, "No variant chosen")

            Contests.VariantChoice.ALL -> http.ok().also { acceptAllVariants(argValues.contest_code) }

            Contests.VariantChoice.RANDOM -> http.ok().also { acceptRandomVariant(argValues.contest_code) }
          }
        }.firstOrNull() ?: http.error(404, "No contest with code ${argValues.contest_code}")
  }
}

data class ContestAttemptsArgs(var contest_code: String) : RequestArgs()

class ContestAttemptsHandler : DashboardHandler<ContestAttemptsArgs>() {
  override fun args() = ContestAttemptsArgs("")

  override fun handle(http: HttpApi, argValues: ContestAttemptsArgs) = withUser(http) {
    println("Searching for attempts of user $id in contest ${argValues.contest_code}")
    AvailableContests.select { AvailableContests.contest_code eq argValues.contest_code }
        .map {
          val assignedVariant = it[AvailableContests.variant_id]
          println("User is assigned variant $assignedVariant")
          if (assignedVariant != null) {
            return@map http.json(getVariantAttempts(assignedVariant))
          }
          if (it[AvailableContests.variant_choice] == Contests.VariantChoice.ALL) {
            return@map http.json(getAllVariantsAttempts(argValues.contest_code))
          }
          http.error(400, "No variant chosen")
        }.firstOrNull() ?: http.error(404, "No contest with code ${argValues.contest_code}")
  }
}

class LogoutHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = redirectToLogin(http)
}
