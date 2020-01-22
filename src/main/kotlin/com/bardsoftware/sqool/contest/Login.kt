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

import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import org.apache.commons.codec.digest.DigestUtils
import java.io.IOException

data class LoginReq(var email: String, var name: String, var password: String, var createIfMissing: String, var redirectUrl: String, var action: String) : RequestArgs()

data class LoginPageArgs(var redirectUrl: String) : RequestArgs()

class LoginPageHandler : RequestHandler<LoginPageArgs>() {
  override fun args(): LoginPageArgs = LoginPageArgs("")

  override fun handle(http: HttpApi, argValues: LoginPageArgs): HttpResponse {
    return http.render("login.ftl", mapOf(
        "redirectUrl" to if (argValues.redirectUrl == "") "/me2" else argValues.redirectUrl
    ))
  }

}
/**
 * @author dbarashev@bardsoftware.com
 */
class LoginHandler : RequestHandler<LoginReq>() {
  private fun trySignin(http: HttpApi, user: User, req: LoginReq): HttpResponse {
    return if (DigestUtils.md5Hex(req.password) == user.password) {
      http.chain {
        session("name", user.name)
        redirect(req.redirectUrl)
      }
    } else {
      http.redirect("/error403")
    }
  }

  override fun handle(http: HttpApi, req: LoginReq): HttpResponse {
    return UserStorage.exec {
      when (req.action) {
        "signup" -> {
          val existingUser = findUser(req.name, req.email) ?:
          if (req.createIfMissing.toBoolean()) {
            createUser(req.name, req.password, req.email)?.also { it ->
              try {
                sendEmail("""
                Привет, ${req.name}, это Дмитрий Барашев и робот SQooL. Просто подтверждаем, что вы зарегистрированы в SQooL и можете теперь решать задачи контестов, когда они появятся. 
                
                На этот адрес вам будут приходить рецензии, если таковые подразумеваются контестом.

                Хорошей учебы!
                
                _-- Дмитрий Барашев_
                
              """.trimIndent(), mapOf(
                    "from" to "DBMS Class <dbms@mg.barashev.net>",
                    "to" to req.email,
                    "subject" to "Welcome to SQooL",
                    "h:Reply-To" to "dbms@barashev.net"
                ))
              } catch (ex: IOException) {
                LOGGER.error("Failed to send an email to ${req.email} (${req.name})", ex)
              }
            }
          } else null

          if (existingUser == null) {
            http.render("signup.ftl", mapOf(
                "name" to req.name,
                "password" to req.password,
                "email" to req.email,
                "redirectUrl" to req.redirectUrl)
            )
          } else {
            trySignin(http, existingUser, req)
          }
        }
        "signin" -> {
          val existingUser = findUser(req.name, req.email)
          if (existingUser == null) {
            http.redirect("/login")
          } else {
            trySignin(http, existingUser, req)
          }
        }
        else -> http.error(400, "Unknown action ${req.action}")
      }
    }
  }

  override fun args(): LoginReq = LoginReq("", "", "", "false", "", "signup")
}

class LogoutHandler : RequestHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = redirectToLogin(http)
}

fun redirectToLogin(http: HttpApi, redirectUrl: String = "/me2") = http.chain {
  clearSession()
  redirect("/login?redirectUrl=$redirectUrl")
}

