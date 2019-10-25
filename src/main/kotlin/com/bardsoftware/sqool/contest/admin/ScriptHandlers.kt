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

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private val JSON_MAPPER = ObjectMapper()

/**
 * @author dbarashev@bardsoftware.com
 */
object Scripts : Table("Contest.ScriptDto") {
  val id = integer("id").primaryKey()
  val description = text("description")
  val body = text("body")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("id", row[id])
      it.put("description", row[description])
      it.put("body", row[body])
    }
  }
}

class ScriptAllHandler : AdminHandler<RequestArgs>() {
  override fun args(): RequestArgs = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withAdminUser(http) {
    http.json(Scripts.selectAll().map(Scripts::asJson).toList())
  }
}

data class ScriptEditArgs(var id: String, var description: String, var body: String) : RequestArgs()

class ScriptEditHandler : AdminHandler<ScriptEditArgs>() {
  override fun args(): ScriptEditArgs = ScriptEditArgs(id = "", description = "", body = "")

  override fun handle(http: HttpApi, argValues: ScriptEditArgs) = withAdminUser(http) {
    when (argValues.id) {
      "" -> {
        Scripts.insert {
          it[description] = argValues.description
          it[body] = argValues.body
        }
        http.ok()
      }
      else -> {
        Scripts.update(where = { Scripts.id eq argValues.id.toInt() }) {
          it[description] = argValues.description
          it[body] = argValues.body
        }
        http.ok()
      }
    }
  }
}

data class ScriptBodyArgs(var id: String) : RequestArgs()

class ScriptBodyHandler : RequestHandler<ScriptBodyArgs>() {
  override fun args(): ScriptBodyArgs = ScriptBodyArgs(id = "")

  override fun handle(http: HttpApi, argValues: ScriptBodyArgs): HttpResponse {
    return transaction {
      val scriptRow = Scripts.select(where = {Scripts.id eq argValues.id.toInt()}).firstOrNull() ?: return@transaction http.error(404)
      println("id=${argValues.id} brow=$scriptRow body=${scriptRow[Scripts.body]}")
      http.text(scriptRow[Scripts.body])
    }
  }
}
