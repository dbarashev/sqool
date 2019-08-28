package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.Contest
import com.bardsoftware.sqool.codegen.docker.ContestImageManager
import com.bardsoftware.sqool.codegen.docker.ImageCheckResult
import com.bardsoftware.sqool.contest.ChainedHttpApi
import com.bardsoftware.sqool.contest.Http
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.storage.User
import com.bardsoftware.sqool.contest.storage.UserStorage
import com.nhaarman.mockito_kotlin.*
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
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
  private val handler = ContestBuildHandler(queryManagerMock, lambdaMock)

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
    handler.handle(httpMock, ContestBuildArgs("code"))
    verify(httpMock).redirect("/login")
  }

  @Test
  fun testNonAdminUser() {
    val httpMock = mockHttp(USER_NAME)
    mockUserStorageObject(mockUserStorage(false))
    handler.handle(httpMock, ContestBuildArgs("code"))
    verify(httpMock).error(403)
  }

  @Test
  fun testNotExistingUser() {
    val httpMock = mockHttp(USER_NAME)
    mockUserStorageObject(mockUserStorage(null))
    handler.handle(httpMock, ContestBuildArgs("code"))
    verify(httpMock).redirect("/login")
  }

  private fun makeAdminUserTest(code: String, message: String, result: ImageCheckResult): Http {
    mockUserStorageObject(mockUserStorage(true))
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
    mockUserStorageObject(mockUserStorage(true))
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

  private fun mockUserStorage(isAdmin: Boolean?): UserStorage {
    val userMock = isAdmin?.let {
      mock<User> { on { it.isAdmin } doReturn isAdmin }
    }
    return mock {
      on { findUser(USER_NAME) } doReturn userMock
    }
  }

  private fun mockUserStorageObject(userStorage: UserStorage) {
    unmockkObject(UserStorage.Companion)
    val codeSlot = slot<UserStorage.() -> HttpResponse>()
    mockkObject(UserStorage.Companion)
    every { UserStorage.exec(code = capture(codeSlot)) } answers {
      val code = codeSlot.captured
      userStorage.code()
    }
  }
}
