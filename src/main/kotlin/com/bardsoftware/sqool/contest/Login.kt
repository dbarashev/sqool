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
import org.apache.commons.codec.digest.DigestUtils

data class LoginReq(val name: String, val password: String, val createIfMissing: Boolean)

/**
 * @author dbarashev@bardsoftware.com
 */
class LoginHandler {
  fun handle(http: HttpApi, req: LoginReq): HttpResponse {
    return UserStorage.exec {
      (findUser(req.name) ?: if (req.createIfMissing) {
        createUser(req.name, req.password)
      } else null)?.let {
        if (DigestUtils.md5Hex(req.password) == it.password) {
          http.chain {
            session("name", it.name)
            redirect("/me2")
          }
        } else {
          http.redirect("/error403")
        }
      } ?: http.render("signup.ftl", mapOf("name" to req.name, "password" to req.password))
    }
  }
}

class LogoutHandler : RequestHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = redirectToLogin(http)
}

fun redirectToLogin(http: HttpApi) = http.chain {
  clearSession()
  redirect("/login")
}

