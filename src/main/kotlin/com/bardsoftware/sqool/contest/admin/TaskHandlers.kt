package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.Flags
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object Tasks : Table("Contest.TaskDto") {
  val id = integer("id").primaryKey()
  val name = text("name")
  val description = text("description")
  val result_json = text("result_json")
}

/**
 * @author dbarashev@bardsoftware.com
 */

class TaskAllHandler(flags: Flags) : DbHandler<RequestArgs>(flags) {
  override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
    return withDatabase { db ->
      http.json(db.contestQueries.selectAllTasks().executeAsList())
    }
  }

  override fun args(): RequestArgs {
    return RequestArgs()
  }
}

class TaskValidationException(msg: String) : Exception(msg)

data class TaskNewArgs(var name: String, var description: String, var result: String) : RequestArgs()
class TaskNewHandler(flags: Flags) : DbHandler<TaskNewArgs>(flags) {
  override fun args(): TaskNewArgs = TaskNewArgs(name = "", description = "", result = "")

  override fun handle(http: HttpApi, argValues: TaskNewArgs): HttpResponse {
    val resultJson = buildResultJson(argValues.result)


    return transaction {
      Tasks.insert {
        it[name] = argValues.name
        it[description] = argValues.description
        it[result_json] = resultJson
      }
      http.ok()
    }
  }
}

fun buildResultJson(resultSpecSql: String): String {
  val resultColumns = resultSpecSql.split(",").map { colSpec ->
    val words = colSpec.trim().split(Regex("\\s+"), limit = 2)
    // TODO: update this code to work with types consisting of two words, like "DOUBLE PRECISION"
    if (words.size > 2) {
      throw TaskValidationException("Malformed column specification $colSpec")
    }
    val (name, type) = if (words.size == 2) { words[0] to words[1] } else { "" to words[0] }
    val sqlType = try {
      SqlDataType.valueOf(type.toUpperCase())
    } catch (ex: IllegalArgumentException) {
      throw TaskValidationException("Unknown type $type")
    }
    return@map TaskResultColumn(name, sqlType)
  }

  val indexInc = if (resultColumns.size == 1 && resultColumns[0].name == "") 0 else 1
  return resultColumns.mapIndexed { index, column ->
    """
        |{ "col_num": ${index + indexInc},
        |  "col_name": "${column.name}",
        |  "col_type": "${column.type.name}"
        |}""".trimMargin()
  }.joinToString(prefix = "[", postfix = "]")
}

