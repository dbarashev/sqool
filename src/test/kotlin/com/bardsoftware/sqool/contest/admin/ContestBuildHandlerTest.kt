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

import com.bardsoftware.sqool.codegen.Contest
import com.bardsoftware.sqool.codegen.docker.ContestImageManager
import com.bardsoftware.sqool.codegen.docker.ImageCheckResult
import com.bardsoftware.sqool.contest.ChainedHttpApi
import com.bardsoftware.sqool.contest.Http
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.io.PrintWriter

private const val USER_NAME = "user"

class ContestBuildHandlerTest {
  private val contestMock = mock<Contest>()
  private val queryManagerMock = mock<DbQueryManager>()
  private val imageManagerMock = mock<ContestImageManager>()
  private val lambdaMock = mock<(Contest) -> ContestImageManager> {
    on { invoke(contestMock) } doReturn imageManagerMock
  }

  @Test
  fun testHandleNotExistingContest() {
    val http = makeAdminUserTest("not_existing", NoSuchContestException())
    verify(http).error(404, "No such contest", null)
    verify(http, never()).ok()
    verify(http, never()).json(any())
  }

  @Test
  fun testHandleCorrectContest() {
    val http = makeAdminUserTest("correct", "", ImageCheckResult.PASSED)
    verify(http, never()).error(any(), any(), anyOrNull())
    verify(http, never()).ok()
    verify(http).json(mapOf("status" to "OK"))
  }

  @Test
  fun testHandleInvalidContest() {
    val http = makeAdminUserTest("invalid", "error", ImageCheckResult.FAILED)
    verify(http, never()).error(any(), any(), anyOrNull())
    verify(http, never()).ok()
    verify(http).json(mapOf("status" to "ERROR", "message" to "error"))
  }

  @Test
  fun testHandleMalformedDataContest() {
    val exception = MalformedDataException("malformed")
    val http = makeAdminUserTest("malformed", exception)
    verify(http).error(400, exception.message, exception)
    verify(http, never()).ok()
    verify(http, never()).json(any())
  }

  @Test
  fun testUnauthenticatedUser() {
    val httpMock = mockHttp(null)
    val handler = getContestBuildHandler(true)
    handler.handle(httpMock, ContestBuildArgs("code"))
    verify(httpMock).redirect("/login")
  }

  @Test
  fun testNonAdminUser() {
    val httpMock = mockHttp(USER_NAME)
    val handler = getContestBuildHandler(false)
    handler.handle(httpMock, ContestBuildArgs("code"))
    verify(httpMock).error(403)
  }

  @Test
  fun testNotExistingUser() {
    val httpMock = mockHttp(USER_NAME)
    val handler = getContestBuildHandler(null)
    handler.handle(httpMock, ContestBuildArgs("code"))
    verify(httpMock).redirect("/login")
  }

  private fun makeAdminUserTest(code: String, message: String, result: ImageCheckResult): Http {
    val handler = getContestBuildHandler(true)
    val httpMock = mockHttp(USER_NAME)
    whenever(queryManagerMock.findContest(code)).thenReturn(contestMock)
    whenever(imageManagerMock.checkImage(any())).doAnswer {
      val writer = PrintWriter(it.getArgument<OutputStream>(0))
      writer.print(message)
      writer.flush()
      result
    }
    handler.handle(httpMock, ContestBuildArgs(code))
    verify(queryManagerMock).findContest(code)
    verify(lambdaMock).invoke(contestMock)
    return httpMock
  }

  private fun makeAdminUserTest(code: String, exception: Exception): Http {
    val handler = getContestBuildHandler(true)
    val httpMock = mockHttp(USER_NAME)
    whenever(queryManagerMock.findContest(code)).doAnswer { throw exception }
    handler.handle(httpMock, ContestBuildArgs(code))
    verify(queryManagerMock).findContest(code)
    return httpMock
  }

  private fun mockHttp(user: String?): Http {
    val mock = mock<Http> {
      on { session("name") } doReturn user
    }
    whenever(mock.chain(any())).doAnswer {
      val body = it.getArgument<HttpApi.() -> Unit>(0)
      val chainedApi = ChainedHttpApi(mock)
      chainedApi.body()
      return@doAnswer { chainedApi.lastResult }
    }
    return mock
  }

  /**
   * Created [ContestBuildHandler] parameterized with [CodeExecutor].
   * The [CodeExecutor] wraps [UserStorage] mock.
   * If [isAdmin] isn't null the [UserStorage] mock returns [User] mock on [UserStorage.findUser], otherwise it returns null.
   * The [User] mock has specified [isAdmin] flag.
   */
  private fun getContestBuildHandler(isAdmin: Boolean?): ContestBuildHandler {
    val userMock = isAdmin?.let {
      mock<User> { on { it.isAdmin } doReturn isAdmin }
    }
    val userStorageMock = mock<UserStorage> {
      on { findUser(USER_NAME) } doReturn userMock
    }
    val codeExecutor: CodeExecutor = { code ->
      userStorageMock.code()
    }
    return ContestBuildHandler(queryManagerMock, lambdaMock, codeExecutor)
  }
}
