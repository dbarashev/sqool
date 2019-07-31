package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.storage.UserStorage
import javax.servlet.http.HttpServletResponse

class UserDashboardHandler {
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
        http.render("me.ftl", mapOf(
            "name" to user.name,
            "tasks" to user.availableTasks,
            "awaitTesting" to ("true" == http.formValue("awaitTesting"))))
      }
    }
  }

  fun handleAttempts(http: HttpApi): HttpResponse {
    val userName = http.session("name") ?: return http.error(HttpServletResponse.SC_FORBIDDEN)
    return UserStorage.exec {
      val user = findUser(userName)
      if (user == null) {
        http.chain {
          clearSession()
          error(HttpServletResponse.SC_NOT_FOUND)
        }
      } else {
        http.json(user.attempts().map { taskAttempt ->
          taskAttempt.entity.taskEntity.name = "${taskAttempt.entity.taskEntity.name} ${user.id}"
          return@map taskAttempt.entity
        })
      }
    }
  }
}
