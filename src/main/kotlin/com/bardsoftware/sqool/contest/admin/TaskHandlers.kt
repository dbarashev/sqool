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

import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.RequestArgs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Strings
import org.jetbrains.exposed.sql.*

private val JSON_MAPPER = ObjectMapper()

object Tasks : Table("Contest.TaskDto") {
  val id = integer("id").primaryKey()
  val name = text("name")
  val description = text("description")
  val hasResult = bool("has_result")
  val result_json = text("result_json")
  val solution = text("solution")
  val script_id = integer("script_id").nullable()
  val author_id = integer("author_id")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("id", row[id])
      it.put("name", row[name])
      it.put("description", row[description])
      it.put("has_result", row[hasResult])
      it.put("solution", row[solution])
      it.put("result_json", row[result_json])
      it.put("script_id", row[script_id])
      it.put("author_id", row[author_id])
    }
  }
}

/**
 * @author dbarashev@bardsoftware.com
 */

class TaskAllHandler : AdminHandler<RequestArgs>() {
  override fun args() = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withAdminUser(http) {
    http.json(Tasks.selectAll().orderBy(Tasks.name).map(Tasks::asJson).toList())
  }

}

class TaskValidationException(msg: String) : Exception(msg)

data class TaskEditArgs(
    var id: String,
    var name: String,
    var description: String,
    var hasResult: String,
    var result: String,
    var solution: String,
    var script_id: String
) : RequestArgs()

class TaskEditHandler : AdminHandler<TaskEditArgs>() {
  override fun args(): TaskEditArgs = TaskEditArgs(
      id = "", name = "", description = "", hasResult = "", result = "", solution = "", script_id = "")

  override fun handle(http: HttpApi, argValues: TaskEditArgs) = withAdminUser(http) { admin ->
    val resultJson = if (argValues.hasResult.toBoolean()) buildResultJson(argValues.result) else "[]"
    when (Strings.emptyToNull(argValues.id)) {
      null -> {
        Tasks.insert {
          it[name] = argValues.name
          it[description] = argValues.description
          it[hasResult] = argValues.hasResult.toBoolean()
          it[result_json] = resultJson
          it[solution] = argValues.solution
          it[script_id] = argValues.script_id.toIntOrNull()
          it[author_id] = admin.id
        }
        http.ok()
      }
      else -> {
        Tasks.update(where = { Tasks.id eq argValues.id.toInt() }) {
          it[name] = argValues.name
          it[description] = argValues.description
          it[hasResult] = argValues.hasResult.toBoolean()
          it[result_json] = resultJson
          it[solution] = argValues.solution
          it[script_id] = argValues.script_id.toIntOrNull()
        }
        http.ok()
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
    val (name, type) = if (words.size == 2) {
      words[0] to words[1]
    } else {
      "" to words[0]
    }
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

