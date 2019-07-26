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
                http.render("me2.ftl", mapOf(
                        "name" to user.name,
                        "contests" to user.availableContests()
                ))
            }
        }
    }
}