package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream


class DockerImageBuilderTest {
    private val stdOut = ByteArrayOutputStream()

    @BeforeEach
    fun setStdOut() {
        System.setOut(PrintStream(stdOut))
    }

    @AfterEach
    fun cleanStdOut() {
        System.setOut(null)
    }

    @Test
    fun testFileStructure() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw2",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw2/schema3.sql", tasks = listOf(task))

        val process = Runtime.getRuntime().exec("docker run --rm contest-image find /workspace")
        val folders = process.inputStream.bufferedReader()
                .use { it.readText() }
                .lines()
                .dropLastWhile { it.isEmpty() }
        val expectedFolders = listOf(
                "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw2",
                "/workspace/hse2019/cw2/Task3-dynamic.sql", "/workspace/hse2019/cw2/variant3-static.sql"
        )
        assertEquals(expectedFolders.sorted(), folders.sorted())
    }

    @Test
    fun testValidSqlStdout() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw2",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw2/schema3.sql", tasks = listOf(task))
        assertEquals("Contest image testing: OK\n", stdOut.toString())
    }

    @Test
    fun testInvalidSqlStdout() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECTY 11", spec)
        buildDockerImage(
                imageName = "contest-image", course = "hse2019", module = "cw2",
                variant = "variant3", schemaPath = "/workspace/hse2019/cw2/schema3.sql", tasks = listOf(task))

        val expectedOutput = """
            |Contest image testing: Invalid sql:
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
            """.trimMargin()
        assertEquals(expectedOutput, stdOut.toString())
    }
}
