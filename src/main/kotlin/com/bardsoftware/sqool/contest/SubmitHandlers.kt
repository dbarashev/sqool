package com.bardsoftware.sqool.contest

import com.bardsoftware.sqool.contest.storage.UserStorage

data class SubmitDoArgs(var contestCode: String = "",
                        var taskId: String = "",
                        var taskName: String = "",
                        var variantId: String = "",
                        var variantName: String = "",
                        var submissionText: String = "") : RequestArgs()
/**
 * @author dbarashev@bardsoftware.com
 */
class SubmitDoHandler(private val assessor: AssessorApi) : DashboardHandler<SubmitDoArgs>() {
  override fun args(): SubmitDoArgs  = SubmitDoArgs()

  override fun handle(http: HttpApi, argValues: SubmitDoArgs): HttpResponse {
    return withUser(http) {user ->
      assessor.submit(argValues.contestCode, argValues.variantName, argValues.taskName, argValues.submissionText) {attemptId ->
              println("Submitted task $attemptId")
        UserStorage.exec {
          user.recordAttempt(argValues.taskId.toInt(), argValues.variantId.toInt(), attemptId)
        }
      }
      http.ok()
    }
  }
}
