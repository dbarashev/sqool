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

data class AuthDevArgs(var user_id: String) : RequestArgs()

class AuthDevHandler : RequestHandler<AuthDevArgs>() {
  override fun handle(http: HttpApi, argValues: AuthDevArgs): HttpResponse {
    http.session("user_id", argValues.user_id)
    return http.ok()
  }

  override fun args(): AuthDevArgs = AuthDevArgs("")
}

