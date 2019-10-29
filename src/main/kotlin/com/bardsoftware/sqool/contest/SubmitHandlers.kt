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

import com.bardsoftware.sqool.contest.admin.Tasks
import org.jetbrains.exposed.sql.select

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
      val hasResult = Tasks.select { Tasks.id eq argValues.taskId.toInt() }.map { it[Tasks.hasResult] }.firstOrNull() ?: false

      assessor.submit(argValues.contestCode, argValues.variantName, argValues.taskName, hasResult, argValues.submissionText) {attemptId ->
        println("User $user submitted assessor task $attemptId")
        withUser(http) {
          it.recordAttempt(argValues.taskId.toInt(), argValues.variantId.toInt(), argValues.contestCode, attemptId, argValues.submissionText)
          http.ok()
        }
      }
      http.ok()
    }
  }
}
