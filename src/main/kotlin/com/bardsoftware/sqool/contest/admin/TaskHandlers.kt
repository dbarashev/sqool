package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.Flags
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Strings
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private val JSON_MAPPER = ObjectMapper()

object Tasks : Table("Contest.TaskDto") {
  val id = integer("id").primaryKey()
  val name = text("name")
  val description = text("description")
  val result_json = text("result_json")
  val solution = text("solution")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("id", row[id])
      it.put("name", row[name])
      it.put("description", row[description])
      it.put("solution", row[solution])
      it.put("result_json", row[result_json])
    }

  }
}

/**
 * @author dbarashev@bardsoftware.com
 */

class TaskAllHandler(flags: Flags) : DbHandler<RequestArgs>(flags) {
  override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
    return transaction {
      http.json(Tasks.selectAll().map(Tasks::asJson).toList())
    }
  }

  override fun args(): RequestArgs {
    return RequestArgs()
  }
}

class TaskValidationException(msg: String) : Exception(msg)

data class TaskEditArgs(var id: String,
                        var name: String,
                        var description: String,
                        var result: String,
                        var solution: String) : RequestArgs()
class TaskEditHandler(flags: Flags) : DbHandler<TaskEditArgs>(flags) {
  override fun args(): TaskEditArgs = TaskEditArgs(
      id = "", name = "", description = "", result = "", solution = "")

  override fun handle(http: HttpApi, argValues: TaskEditArgs): HttpResponse {
    val resultJson = buildResultJson(argValues.result)

    return transaction {
      when (Strings.emptyToNull(argValues.id)) {
        null -> {
          Tasks.insert {
            it[name] = argValues.name
            it[description] = argValues.description
            it[result_json] = resultJson
            it[solution] = argValues.solution
          }
          http.ok()
        }
        else -> {
          Tasks.update(where = {Tasks.id eq argValues.id.toInt()}) {
            it[name] = argValues.name
            it[description] = argValues.description
            it[result_json] = resultJson
            it[solution] = argValues.solution
          }
          http.ok()
        }
      }
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

