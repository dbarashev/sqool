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

package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.*
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage

typealias CodeExecutor = (code: UserStorage.() -> HttpResponse) -> HttpResponse
private val PROD_CODE_EXECUTOR: CodeExecutor = UserStorage.Companion::exec

abstract class AdminHandler<T : RequestArgs>(private val codeExecutor: CodeExecutor = PROD_CODE_EXECUTOR) : RequestHandler<T>()  {
  protected fun withAdminUser(http: HttpApi, redirectUrl: String = "/dashboard", handle: (User) -> HttpResponse): HttpResponse {
    val userName = http.session("name") ?: return redirectToLogin(http, redirectUrl)
    return codeExecutor {
      val user = findUser(userName) ?: return@codeExecutor redirectToLogin(http)
      if (!user.isAdmin) {
        return@codeExecutor http.error(403)
      }
      handle(user)
    }
  }
}
