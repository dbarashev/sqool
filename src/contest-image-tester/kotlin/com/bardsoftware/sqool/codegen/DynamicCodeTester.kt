package com.bardsoftware.sqool.codegen

import java.io.File
import java.sql.SQLException
import java.text.MessageFormat
import java.util.*
import javax.sql.DataSource

enum class SubmissionResultStatus {
  SUCCESSFUL, FAILED, ERROR, TIMEOUT
}

data class SubmissionResult(val status: SubmissionResultStatus,
                            val message: String? = "") {
  companion object {
    val SUCCESSFUL = SubmissionResult(SubmissionResultStatus.SUCCESSFUL, null)
  }
}

class DynamicCodeTester {

  fun runTest(dataSource: DataSource, course: String, module: String, task: String, solution: String): SubmissionResult {
    val connection = dataSource.connection
    val schemaName = "A" + UUID.randomUUID().toString().replace("-", "")

    val postgresDbms = connection.createStatement()
    try {
      postgresDbms.execute("create schema $schemaName;")
      postgresDbms.execute("set search_path = $schemaName,$module;")
      postgresDbms.queryTimeout = 120

      val scriptTemplate = File("/workspace/$course/$module/$task-dynamic.sql").readText()
      val formatArgs = listOf(task, solution)

      val userScript = MessageFormat.format(scriptTemplate, *formatArgs.toTypedArray())
      postgresDbms.execute(userScript)


      val result = mutableListOf<String>()
      val rowSet = postgresDbms.executeQuery("SELECT * FROM ${task}_Matcher()")
      while (rowSet.next()) {
        result.add(rowSet.getString(1))
      }
      return if (result.isEmpty()) SubmissionResult.SUCCESSFUL
      else SubmissionResult(SubmissionResultStatus.FAILED, result.joinToString("\n"))
    } catch (ex: SQLException) {
      return SubmissionResult(SubmissionResultStatus.FAILED, ex.message)
    }
  }
}
