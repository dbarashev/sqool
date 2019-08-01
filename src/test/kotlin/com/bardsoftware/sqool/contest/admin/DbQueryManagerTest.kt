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
import org.junit.jupiter.api.Assertions.*
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

    val exception = assertThrows(MalformedDataException::class.java) {
      dbQueryManager.resultRowToTask(resultRow)
    }
    assertEquals("Invalid task json", exception.message)
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

    val exception = assertThrows(MalformedDataException::class.java) {
      dbQueryManager.resultRowToTask(resultRow)
    }
    assertEquals("Invalid task json", exception.message)
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