package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.ScalarValueTask
import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.Flags
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayOutputStream

class DockerImageBuilderTest {
    private val outputStream = ByteArrayOutputStream()
    private val flags = mock<Flags> {
        on { postgresAddress } doReturn (System.getProperty("postgres.ip") ?: "localhost")
        on { postgresPort } doReturn "5432"
        on { postgresUser } doReturn "postgres"
        on { postgresPassword } doReturn ""
    }

    @AfterEach
    fun cleanOutputStream() {
        outputStream.reset()
    }

    @Test
    fun testFileStructure() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        val variant = Variant("cw1", "/workspace/hse2019/cw1/schema3.sql", listOf(task))
        buildDockerImage("contest-image", "hse2019", listOf(variant))

        val expectedFolders = listOf(
                "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw1", "/workspace/hse2019/init.sql",
                "/workspace/hse2019/cw1/Task3-dynamic.sql", "/workspace/hse2019/cw1/static.sql"
        )
        checkFileStructure(expectedFolders)
    }

    @Test
    fun testMultipleVariantsFileStructure() {
        val firstVariantTasks = listOf(
                ScalarValueTask("Task1", "", SqlDataType.INT), ScalarValueTask("Task2", "", SqlDataType.INT)
        )
        val firstVariant = Variant("cw1", "/workspace/hse2019/cw1/schema3.sql", firstVariantTasks)
        val secondVariantTasks = listOf(ScalarValueTask("Task1", "", SqlDataType.INT))
        val secondVariant = Variant("cw2", "/workspace/hse2019/cw2/schema3.sql", secondVariantTasks)
        buildDockerImage("contest-image", "hse2019", listOf(firstVariant, secondVariant))

        val expectedFolders = listOf(
                "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw1", "/workspace/hse2019/cw2",
                "/workspace/hse2019/init.sql", "/workspace/hse2019/cw1/static.sql", "/workspace/hse2019/cw2/static.sql",
                "/workspace/hse2019/cw1/Task1-dynamic.sql", "/workspace/hse2019/cw1/Task2-dynamic.sql",
                "/workspace/hse2019/cw2/Task1-dynamic.sql"
        )
        checkFileStructure(expectedFolders)
    }

    @Test
    fun testValidStaticSql() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11 LIMIT 0;", spec)
        val variants = listOf(Variant("cw3", "/workspace/hse2019/cw3/schema3.sql", listOf(task)))
        buildDockerImage("contest-image", "hse2019", variants)
        val result = checkImage("contest-image", "hse2019", variants, flags, outputStream)

        val expectedOutput = """
            |Static code testing:
            |OK
            |Dynamic code testing:
            |OK
            |
            """.trimMargin()
        assertEquals(expectedOutput, outputStream.toString())
        assertEquals(ImageCheckResult.PASSED, result)
    }

    @Test
    fun testMultipleVariantsValidStaticSql() {
        val firstVariantTasks = listOf(
                ScalarValueTask("Task12", "SELECT 'Some text'", SqlDataType.TEXT),
                ScalarValueTask("Task33", "SELECT 33", SqlDataType.INT)
        )
        val firstVariant = Variant("cw4", "/workspace/hse2019/cw4/schema3.sql", firstVariantTasks)

        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11 LIMIT 0;", spec)
        val secondVariant = Variant("cw5", "/workspace/hse2019/cw5/schema3.sql", listOf(task))

        val variants = listOf(firstVariant, secondVariant)
        buildDockerImage("contest-image", "hse2019", variants)
        val result = checkImage("contest-image", "hse2019", variants, flags, outputStream)

        val expectedOutput = """
            |Static code testing:
            |OK
            |Dynamic code testing:
            |OK
            |
            """.trimMargin()
        assertEquals(expectedOutput, outputStream.toString())
        assertEquals(ImageCheckResult.PASSED, result)
    }

    @Test
    fun testInvalidStaticSql() {
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECTY 11", spec)
        val variants = listOf(Variant("cw2", "/workspace/hse2019/cw2/schema3.sql", listOf(task)))
        buildDockerImage("contest-image", "hse2019", variants)
        val result = checkImage("contest-image", "hse2019", variants, flags, outputStream)

        val expectedOutput = """
            |Static code testing:
            |Invalid sql:
            |psql:/workspace/hse2019/cw2/static.sql:1: NOTICE:  schema "cw2" does not exist, skipping
            |DROP SCHEMA
            |CREATE SCHEMA
            |SET
            |psql:/workspace/hse2019/cw2/static.sql:4: /workspace/hse2019/cw2/schema3.sql: No such file or directory
            |psql:/workspace/hse2019/cw2/static.sql:9: ERROR:  syntax error at or near "SELECTY"
            |LINE 3: SELECTY 11
            |        ^
            |CREATE FUNCTION
            |psql:/workspace/hse2019/cw2/static.sql:19: ERROR:  function task3_robot() does not exist
            |LINE 2:    SELECT 1 AS query_id, * FROM Task3_Robot()
            |                                        ^
            |HINT:  No function matches the given name and argument types. You might need to add explicit type casts.
            |CREATE FUNCTION
            |DROP FUNCTION
            |
            |
            """.trimMargin()
        assertEquals(expectedOutput, outputStream.toString())
        assertEquals(ImageCheckResult.FAILED, result)
    }

    @Test
    fun testMultipleVariantsInvalidStaticSql() {
        val firstVariantTasks = listOf(
                ScalarValueTask("Task12", "SELECT 'Some text", SqlDataType.TEXT),
                ScalarValueTask("Task33", "SELECT 3!", SqlDataType.INT)
        )
        val firstVariant = Variant("cw14", "/workspace/hse2019/cw14/schema3.sql", firstVariantTasks)

        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11 LIMITY 0;", spec)
        val secondVariant = Variant("cw52", "/workspace/hse2019/cw52/schema3.sql", listOf(task))

        val variants = listOf(firstVariant, secondVariant)
        buildDockerImage("contest-image", "hse2019", variants)
        val result = checkImage("contest-image", "hse2019", variants, flags, outputStream)

        val expectedOutput = """
            |Static code testing:
            |Invalid sql:
            |psql:/workspace/hse2019/cw14/static.sql:1: NOTICE:  schema "cw14" does not exist, skipping
            |DROP SCHEMA
            |CREATE SCHEMA
            |SET
            |psql:/workspace/hse2019/cw14/static.sql:4: /workspace/hse2019/cw14/schema3.sql: No such file or directory
            |psql:/workspace/hse2019/cw14/static.sql:9: ERROR:  unterminated quoted string at or near "'Some text
            |"
            |LINE 3: SELECT 'Some text
            |               ^
            |CREATE FUNCTION
            |CREATE FUNCTION
            |DROP FUNCTION
            |psql:/workspace/hse2019/cw14/static.sql:52: ERROR:  return type mismatch in function declared to return integer
            |DETAIL:  Actual return type is numeric.
            |CONTEXT:  SQL function "task33_robot"
            |CREATE FUNCTION
            |CREATE FUNCTION
            |DROP FUNCTION
            |psql:/workspace/hse2019/cw52/static.sql:1: NOTICE:  schema "cw52" does not exist, skipping
            |DROP SCHEMA
            |CREATE SCHEMA
            |SET
            |psql:/workspace/hse2019/cw52/static.sql:4: /workspace/hse2019/cw52/schema3.sql: No such file or directory
            |psql:/workspace/hse2019/cw52/static.sql:9: ERROR:  syntax error at or near "0"
            |LINE 3: SELECT 11 LIMITY 0;
            |                         ^
            |CREATE FUNCTION
            |psql:/workspace/hse2019/cw52/static.sql:19: ERROR:  function task3_robot() does not exist
            |LINE 2:    SELECT 1 AS query_id, * FROM Task3_Robot()
            |                                        ^
            |HINT:  No function matches the given name and argument types. You might need to add explicit type casts.
            |CREATE FUNCTION
            |DROP FUNCTION
            |
            |
            """.trimMargin()
        assertEquals(expectedOutput, outputStream.toString())
        assertEquals(ImageCheckResult.FAILED, result)
    }

    private fun checkFileStructure(expectedFolders: List<String>) {
        val process = Runtime.getRuntime().exec("docker run --rm contest-image find /workspace")
        val folders = process.inputStream.bufferedReader()
                .use { it.readText() }
                .lines()
                .dropLastWhile { it.isEmpty() }
        assertEquals(expectedFolders.sorted(), folders.sorted())
    }
}
