package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.storage.AvailableContests
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

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

data class VariantAttemptsArgs(var id: String) : RequestArgs()

class VariantAttemptsHandler : DashboardHandler<VariantAttemptsArgs>() {
  override fun args() = VariantAttemptsArgs("")

  override fun handle(http: HttpApi, argValues: VariantAttemptsArgs) = withUser(http) {
    val id = argValues.id.toInt()
    acceptVariant(id)
    http.json(getVariantAttempts(id))
  }
}

data class ContestAttemptsArgs(var code: String) : RequestArgs()

class ContestAttemptsHandler : DashboardHandler<ContestAttemptsArgs>() {
  override fun args() = ContestAttemptsArgs("")

  override fun handle(http: HttpApi, argValues: ContestAttemptsArgs) = withUser(http) {
    Contests.join(AvailableContests, JoinType.INNER, onColumn = Contests.code, otherColumn = AvailableContests.contest_code)
        .select { Contests.code eq argValues.code }
        .map {
          val variantId = it[AvailableContests.variant_id]
          val variantChoice = it[Contests.variant_choice]
          if (variantId != null) {
            acceptVariant(variantId)
            return@map http.json(getVariantAttempts(variantId))
          }

          when (variantChoice) {
            Contests.VariantChoice.ANY -> http.error(400, "No variant chosen")

            Contests.VariantChoice.ALL -> {
              acceptAllVariants(argValues.code)
              http.json(getAllVariantsAttempts(argValues.code))
            }

            Contests.VariantChoice.RANDOM -> {
              val randomVariantId = acceptRandomVariant(argValues.code)
              http.json(getVariantAttempts(randomVariantId))
            }
          }
        }.first()
  }
}

class LogoutHandler : DashboardHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = redirectToLogin(http)
}