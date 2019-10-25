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

import com.bardsoftware.sqool.codegen.task.MultiColumnTask
import com.bardsoftware.sqool.codegen.task.ScalarValueTask
import com.bardsoftware.sqool.codegen.task.SingleColumnTask
import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.jetbrains.exposed.sql.ResultRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DbQueryManagerTest {
  private val dbQueryManager = DbQueryManager()

  @Test
  fun testConvertToScalarValueTask() {
    val taskName = "task1"
    val solution = "solution"
    val attributes = listOf(TaskResultColumn("", SqlDataType.INT))
    val resultRow = mockResultRow(taskName, solution, attributes)

    val expectedTask = ScalarValueTask("task1", "solution", SqlDataType.INT)
    assertEquals(expectedTask, dbQueryManager.resultRowToTask(resultRow))
  }

  @Test
  fun testConvertToSingleColumnTask() {
    val taskName = "task 2"
    val solution = "solution."
    val attributes = listOf(TaskResultColumn("id", SqlDataType.INT))
    val resultRow = mockResultRow(taskName, solution, attributes)

    val expectedTask = SingleColumnTask(
        "task 2", "solution.", TaskResultColumn("id", SqlDataType.INT)
    )
    assertEquals(expectedTask, dbQueryManager.resultRowToTask(resultRow))
  }

  @Test
  fun testConvertToMultiColumnTask() {
    val taskName = "task1"
    val solution = "solution"
    val attributes = listOf(
        TaskResultColumn("text", SqlDataType.TEXT),
        TaskResultColumn("num", SqlDataType.INT)
    )
    val resultRow = mockResultRow(taskName, solution, attributes)

    val relationSpec = RelationSpec(attributes)
    val matcherSpec = MatcherSpec(relationSpec)
    val expectedTask = MultiColumnTask("task1", "solution", matcherSpec)
    assertEquals(expectedTask, dbQueryManager.resultRowToTask(resultRow))
  }

  @Test
  fun testNoSolutionTask() {
    val taskName = "task1"
    val solution = ""
    val attributes = listOf(
        TaskResultColumn("text", SqlDataType.TEXT),
        TaskResultColumn("num", SqlDataType.INT)
    )
    val resultRow = mockResultRow(taskName, solution, attributes)

    assertThrows(MalformedDataException::class.java) {
      dbQueryManager.resultRowToTask(resultRow)
    }
  }

  @Test
  fun testNoNameTask() {
    val taskName = ""
    val solution = "solution"
    val attributes = listOf(
        TaskResultColumn("text", SqlDataType.TEXT),
        TaskResultColumn("num", SqlDataType.INT)
    )
    val resultRow = mockResultRow(taskName, solution, attributes)

    assertThrows(MalformedDataException::class.java) {
      dbQueryManager.resultRowToTask(resultRow)
    }
  }

  @Test
  fun `Test the result type of column tasks`() {
    buildTask("Task001", """
      [ {"name": "value", "type": "TEXT", "num": "2"}, {"name": "id", "type": "INT", "num": "1"} ] 
    """.trimIndent(), "SELECT NULL::INT, NULL::TEXT").let {
      assertEquals("TABLE(id INT, value TEXT)", it.resultType)
    }

    buildTask("Task002", """
      [ {"name": "id", "type": "INT", "num": "1"}, {"name": "value", "type": "TEXT", "num": "2"} ] 
    """.trimIndent(), "SELECT NULL::INT, NULL::TEXT").let {
      assertEquals("TABLE(id INT, value TEXT)", it.resultType)
    }

    buildTask("Task003", """
      [ {"name": "id", "type": "INT", "num": "1"} ] 
    """.trimIndent(), "SELECT NULL::INT").let {
      assertEquals("TABLE(id INT)", it.resultType)
    }
  }

  private fun mockResultRow(name: String, solution: String, attributes: List<TaskResultColumn>): ResultRow {
    val resultJson = attributes.joinToString(separator = "},{", prefix = "[{", postfix = "}]") {
      """"name" : "${it.name}", "type" : "${it.type.name}""""
    }
    return mock {
      on { get(Tasks.name) } doReturn name
      on { get(Tasks.solution) } doReturn solution
      on { get(Tasks.result_json) } doReturn resultJson
    }
  }
}
