/*
 * Copyright (c) BarD Software s.r.o 2019
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
