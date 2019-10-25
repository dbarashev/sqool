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
import com.bardsoftware.sqool.grader.AssessmentPubSubResp
import javax.servlet.http.HttpServletResponse

/**
 * @author dbarashev@bardsoftware.com
 */

class ChallengeHandler {
  fun handleMaybeTry(http: HttpApi): HttpResponse {
    val userName = http.session("name") ?: return http.redirect("/login")
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec http.error(HttpServletResponse.SC_FORBIDDEN)
      val difficulty = http.formValue("difficulty")?.toInt()
          ?: return@exec http.error(HttpServletResponse.SC_BAD_REQUEST)
      val authorId = http.formValue("author")?.toInt()
      http.json(user.createChallengeOffer(difficulty, authorId))
    }
  }

  fun handleDoTry(http: HttpApi): HttpResponse {
    val userName = http.session("name") ?: return http.redirect("/login")
    val taskId = http.formValue("id")?.toInt() ?: return http.error(HttpServletResponse.SC_BAD_REQUEST)
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec http.error(HttpServletResponse.SC_FORBIDDEN)
      if (user.acceptChallenge(taskId)) {
        http.error(HttpServletResponse.SC_NO_CONTENT)
      } else {
        http.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }

  fun handleAttemptStatus(http: HttpApi): HttpResponse {
    val attemptId = http.formValue("attempt-id") ?: return http.error(HttpServletResponse.SC_BAD_REQUEST)
    return UserStorage.exec {
      val attempt = findAttempt(attemptId)
      println("attemptid=$attemptId attempt=$attempt")
      if (attempt == null) {
        http.error(404)
      } else {
        return@exec http.json(attempt.entity)
      }
    }
  }

  fun handleAssessmentResponse(response: AssessmentPubSubResp) {
    println("Got response!\n$response")
    UserStorage.exec {
      val attempt = findAttempt(response.requestId) ?: return@exec
      attempt.assessorResponse = response
    }
  }
}
