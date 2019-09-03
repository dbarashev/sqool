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

  fun handleSubmissionPage(http: HttpApi, contestId: String): HttpResponse {
    val userName = http.session("name") ?: return http.redirect("/login")
    val taskId = http.formValue("id")?.toInt() ?: return http.error(HttpServletResponse.SC_BAD_REQUEST)
    return UserStorage.exec {
      val user = findUser(userName) ?: return@exec http.error(HttpServletResponse.SC_FORBIDDEN)
      val task = findTask(taskId) ?: return@exec http.error(HttpServletResponse.SC_NOT_FOUND)
      task.entity.name = "${task.entity.name} ${user.id}"
      http.render("task-submit.ftl", mapOf(
          "task" to task.entity,
          "user" to user.entity,
          "contestId" to contestId
      ))
    }
  }

  fun handleSubmit(http: Http, assessor: AssessorApi): HttpResponse {
    val userName = http.session("name") ?: return http.redirect("/login")
    val taskId = http.formValue("task-id")?.toInt() ?: return http.error(HttpServletResponse.SC_BAD_REQUEST)
    val solution = http.formValue("solution") ?: return http.error(HttpServletResponse.SC_BAD_REQUEST)
    val contestId = http.formValue("contest-id") ?: "test-contest"
    println("User: $userName Contest:$contestId Task: $taskId Solution:\n$solution")

    val task = UserStorage.exec {
      findTask(taskId)
    } ?: return http.error(400, "Task $taskId not found")

    assessor.submit(contestId, task.entity.name, solution) {
      println("Submitted task $it")
      UserStorage.exec {
        val user = findUser(userName) ?: return@exec
        user.recordAttempt(taskId, it)
      }
    }
    return http.redirect("/me?awaitTesting=true")
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
