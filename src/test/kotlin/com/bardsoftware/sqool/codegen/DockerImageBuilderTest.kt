package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.docker.*
import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.Flags
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.FlowLayout
import java.io.ByteArrayOutputStream

class DockerImageBuilderTest {
    private val outputStream = ByteArrayOutputStream()
    private val flags = mock<Flags> {
        on { postgresAddress } doReturn "localhost"
        on { postgresPort } doReturn "5432"
        on { postgresUser } doReturn "postgres"
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun runPsqlServer() {
            Runtime.getRuntime().exec("service postgresql start")
        }

        @AfterAll
        @JvmStatic
        fun stopPsqlServer() {
            Runtime.getRuntime().exec("service postgresql stop")
        }
    }

    @AfterEach
    fun cleanOutputStream() {
        outputStream.reset()
    }

    @Test
    fun testFileStructure() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw1",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw1/schema3.sql", tasks = listOf(task))

        val process = Runtime.getRuntime().exec("docker run --rm contest-image find /workspace")
        val folders = process.inputStream.bufferedReader()
                .use { it.readText() }
                .lines()
                .dropLastWhile { it.isEmpty() }
        val expectedFolders = listOf(
                "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw1",
                "/workspace/hse2019/cw1/Task3-dynamic.sql", "/workspace/hse2019/cw1/variant3-static.sql"
        )
        assertEquals(expectedFolders.sorted(), folders.sorted())
    }

    @Test
    fun testValidSql() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw3",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw3/schema3.sql", tasks = listOf(task))
        val result = checkImage("contest-image", listOf(task), flags, outputStream)
        assertEquals(ImageCheckResult.PASSED, result)
        assertEquals("", outputStream.toString())
    }

    @Test
    fun testInvalidSql() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECTY 11", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw2",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw2/schema3.sql", tasks = listOf(task))
        val result = checkImage("contest-image", listOf(task), flags, outputStream)

        assertEquals(ImageCheckResult.FAILED, result)
        val expectedOutput = """
            |Invalid sql:
            |CREATE SCHEMA
            |SET
            |/workspace/hse2019/cw2/schema3.sql: No such file or directory
            |ERROR:  syntax error at or near "SELECTY"
            |LINE 3: SELECTY 11
            |        ^
            |CREATE FUNCTION
            |ERROR:  function task3_robot() does not exist
            |LINE 2:    SELECT 0 AS query_id, * FROM Task3_Robot()
            |                                        ^
            |HINT:  No function matches the given name and argument types. You might need to add explicit type casts.
            |CREATE FUNCTION
            |DROP FUNCTION
            |
            """.trimMargin()
        assertEquals(expectedOutput, outputStream.toString())
    }
}