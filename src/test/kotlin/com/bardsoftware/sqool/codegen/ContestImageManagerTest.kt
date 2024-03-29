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

package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.docker.ContestImageManager
import com.bardsoftware.sqool.codegen.docker.ImageCheckResult
import com.bardsoftware.sqool.codegen.task.MultiColumnTask
import com.bardsoftware.sqool.codegen.task.ScalarValueTask
import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.Flags
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class ContestImageManagerTest {
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
    val variant = Variant("cw1", listOf(task), emptyList())
    val contest = Contest(name = "contest-image", code = "hse2019", variants = listOf(variant))
    val imageManager = ContestImageManager(contest, flags)
    imageManager.createImage()

    val expectedFolders = listOf(
        "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw1", "/workspace/hse2019/init.sql",
        "/workspace/hse2019/schema", "/workspace/hse2019/cw1/Task3-dynamic.sql", "/workspace/hse2019/cw1/static.sql"
    )
    checkFileStructure("hse2019", expectedFolders)
  }

  @Test
  fun testMultipleVariantsFileStructure() {
    val firstVariantTasks = listOf(
        ScalarValueTask("Task1", "", SqlDataType.INT),
        ScalarValueTask("Task2", "", SqlDataType.INT)
    )
    val firstVariant = Variant("cw1", firstVariantTasks, listOf(mockSchema("First", "")))

    val secondVariantTasks = listOf(ScalarValueTask("Task1", "", SqlDataType.INT))
    val secondVariant = Variant("cw2", secondVariantTasks, listOf(mockSchema("Second", "")))
    val contest = Contest(name = "contest-image", code = "hse2019", variants = listOf(firstVariant, secondVariant))
    val imageManager = ContestImageManager(contest, flags)
    imageManager.createImage()

    val expectedFolders = listOf(
        "/workspace", "/workspace/hse2019", "/workspace/hse2019/cw1", "/workspace/hse2019/cw2",
        "/workspace/hse2019/schema", "/workspace/hse2019/schema/First.sql", "/workspace/hse2019/schema/Second.sql",
        "/workspace/hse2019/init.sql", "/workspace/hse2019/cw1/static.sql", "/workspace/hse2019/cw2/static.sql",
        "/workspace/hse2019/cw1/Task1-dynamic.sql", "/workspace/hse2019/cw1/Task2-dynamic.sql",
        "/workspace/hse2019/cw2/Task1-dynamic.sql"
    )
    checkFileStructure("hse2019", expectedFolders)
  }

  @Test
  fun testValidStaticSql() {
    val spec = TaskResultColumn("id", SqlDataType.INT)
    val task = SingleColumnTask("Task3", "SELECT 11 LIMIT 0;", spec)
    val schema = mockSchema("schema3", "CREATE TABLE Contest(code TEXT NOT NULL PRIMARY KEY);")
    val variants = listOf(Variant("cw3", listOf(task), listOf(schema)))
    val contest = Contest(name = "contest-image", code = "hse2019", variants = variants)
    val imageManager = ContestImageManager(contest, flags)
    imageManager.createImage()
    val result = imageManager.checkImage(outputStream)

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
    val schema = mockSchema("schema3", "CREATE TABLE Contest(code TEXT NOT NULL PRIMARY KEY);")
    val firstVariant = Variant("cw4", firstVariantTasks, listOf(schema))

    val keyAttribute = listOf(
        TaskResultColumn("ship", SqlDataType.TEXT),
        TaskResultColumn("port", SqlDataType.INT)
    )
    val nonKeyAttributes = listOf(
        TaskResultColumn("transfers_num", SqlDataType.INT),
        TaskResultColumn("transfer_size", SqlDataType.DOUBLE_PRECISION),
        TaskResultColumn("product", SqlDataType.TEXT)
    )
    val relationSpec = RelationSpec(keyAttribute, nonKeyAttributes)
    val matcherSpec = MatcherSpec(relationSpec, "Множество пар (корабль, порт) отличается от результатов робота")
    val task = MultiColumnTask("Task05", "SELECT 'ship', 1, 10, 500::DOUBLE PRECISION, 'prod'", matcherSpec)
    val secondVariant = Variant("cw5", listOf(task), listOf(schema))

    val variants = listOf(firstVariant, secondVariant)
    val contest = Contest(name = "contest-image", code = "hse2019", variants = variants)
    val imageManager = ContestImageManager(contest, flags)
    imageManager.createImage()
    val result = imageManager.checkImage(outputStream)

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
    val schema = mockSchema("schema3", "CREATE TABLE Contest(code TEX NOT NULL PRIMARY KEY);")
    val variants = listOf(Variant("cw2", listOf(task), listOf(schema)))
    val contest = Contest(name = "My HSE Contest", code = "hse2019", variants = variants)
    val imageManager = ContestImageManager(contest, flags)
    imageManager.createImage()
    val result = imageManager.checkImage(outputStream)

    val expectedOutput = """
        |Static code testing:
        |Invalid sql:
        |psql:/workspace/hse2019/cw2/static.sql:1: NOTICE:  schema "cw2" does not exist, skipping
        |DROP SCHEMA
        |CREATE SCHEMA
        |SET
        |psql:/workspace/hse2019/schema/schema3.sql:1: ERROR:  type "tex" does not exist
        |LINE 1: CREATE TABLE Contest(code TEX NOT NULL PRIMARY KEY);
        |                                  ^
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
    val firstVariant = Variant("cw14", firstVariantTasks, emptyList())

    val spec = TaskResultColumn("id", SqlDataType.INT)
    val task = SingleColumnTask("Task3", "SELECT 11 LIMITY 0;", spec)
    val secondVariant = Variant("cw52", listOf(task), emptyList())

    val variants = listOf(firstVariant, secondVariant)
    val contest = Contest(name = "contest-image", code = "hse2019", variants = variants)
    val imageManager = ContestImageManager(contest, flags)
    imageManager.createImage()
    val result = imageManager.checkImage(outputStream)

    val expectedOutput = """
        |Static code testing:
        |Invalid sql:
        |psql:/workspace/hse2019/cw14/static.sql:1: NOTICE:  schema "cw14" does not exist, skipping
        |DROP SCHEMA
        |CREATE SCHEMA
        |SET
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

  private fun checkFileStructure(imageName: String, expectedFolders: List<String>) {
    val process = Runtime.getRuntime().exec("docker run --rm $imageName find /workspace")
    val folders = process.inputStream.bufferedReader()
        .use { it.readText() }
        .lines()
        .dropLastWhile { it.isEmpty() }
    assertEquals(expectedFolders.sorted(), folders.sorted())
  }

  private fun mockSchema(description: String, body: String): Schema {
    val mock = mock<Schema>()
    whenever(mock.description).thenReturn(description)
    whenever(mock.body).thenReturn(body)
    return mock
  }
}
