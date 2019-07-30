package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.Contest
import com.bardsoftware.sqool.codegen.docker.ContestImageManager
import com.bardsoftware.sqool.codegen.docker.ImageCheckResult
import com.bardsoftware.sqool.contest.Http
import com.nhaarman.mockito_kotlin.*
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.io.PrintWriter

class ContestBuildHandlerTest {
    private val httpMock = mock<Http>()
    private val contestMock = mock<Contest>()
    private val queryManagerMock = mock<DbQueryManager>()
    private val imageManagerMock = mock<ContestImageManager>()
    private val lambdaMock = mock<(Contest) -> ContestImageManager> {
        on { invoke(contestMock) } doReturn imageManagerMock
    }
    private val handler = ContestBuildHandler(queryManagerMock, lambdaMock)

    @Test
    fun testHandleNotExistingContest() {
        makeTest("not_existing", NoSuchContestException())
        verify(httpMock).error(404, "No such contest", null)
        verify(httpMock, never()).ok()
        verify(httpMock, never()).json(any())
    }

    @Test
    fun testHandleCorrectContest() {
        makeTest("correct", "", ImageCheckResult.PASSED)
        verify(httpMock, never()).error(any(), any(), anyOrNull())
        verify(httpMock, never()).ok()
        verify(httpMock).json(mapOf("status" to "OK"))
    }

    @Test
    fun testHandleInvalidContest() {
        makeTest("invalid", "error", ImageCheckResult.FAILED)
        verify(httpMock, never()).error(any(), any(), anyOrNull())
        verify(httpMock, never()).ok()
        verify(httpMock).json(mapOf("status" to "ERROR", "message" to "error"))
    }

    @Test
    fun testHandleMalformedDataContest() {
        val exception = MalformedDataException("malformed")
        makeTest("malformed", exception)
        verify(httpMock).error(400, exception.message, exception)
        verify(httpMock, never()).ok()
        verify(httpMock, never()).json(any())
    }

    private fun makeTest(code: String, message: String, result: ImageCheckResult) {
        whenever(queryManagerMock.findContest(code)).thenReturn(contestMock)
        whenever(imageManagerMock.checkImage(any())).doAnswer {
            val writer = PrintWriter(it.getArgument<OutputStream>(0))
            writer.print(message)
            writer.flush()
            result
        }
        val args = ContestBuildArgs(code)
        handler.handle(httpMock, args)
        verify(queryManagerMock).findContest(code)
        verify(lambdaMock).invoke(contestMock)
    }

    private fun makeTest(code: String, exception: Exception) {
        whenever(queryManagerMock.findContest(code)).doAnswer { throw exception }
        val args = ContestBuildArgs(code)
        handler.handle(httpMock, args)
        verify(queryManagerMock).findContest(code)
    }
}
