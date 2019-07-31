package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.storage.UserStorage

class DashboardHandler {
    fun handle(http: HttpApi): HttpResponse {
        val userName = http.session("name") ?: return http.redirect("/login")
        return UserStorage.exec {
            val user = findUser(userName)
            if (user == null) {
                http.chain {
                    clearSession()
                    redirect("/login")
                }
            } else {
                http.render("me2.ftl", mapOf("userId" to user.id))
            }
        }
    }
}

data class AvailableContestAllArgs(var userId: String) : RequestArgs()

class AvailableContestAllHandler : RequestHandler<AvailableContestAllArgs>() {
    override fun args() = AvailableContestAllArgs("")

    override fun handle(http: HttpApi, argValues: AvailableContestAllArgs): HttpResponse {
        val user = UserStorage.exec { findUserById(argValues.userId.toInt()) } ?: return http.redirect("/login")
        val contests = user.availableContests()
        return http.json(contests)
    }
}