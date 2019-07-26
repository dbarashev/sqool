package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.storage.UserStorage
import com.fasterxml.jackson.databind.ObjectMapper

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
                //for testing purposes only
                user.addAvailableContests()
                http.render("me2.ftl", mapOf(
                        "name" to user.name,
                        "contests" to ObjectMapper().writeValueAsString(user.availableContests())
                ))
            }
        }
    }
}