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

import com.bardsoftware.sqool.contest.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*

private val JSON_MAPPER = ObjectMapper()

object Variants : Table("Contest.VariantDto") {
  val id = integer("id").primaryKey()
  val name = text("name")
  val tasks_id_json_array = text("tasks_id_json_array")
  val scripts_id_json_array = text("scripts_id_json_array")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("id", row[id])
      it.put("name", row[name])
      it.set<JsonNode>("tasks", JSON_MAPPER.readTree(row[tasks_id_json_array]))
    }
  }
}

class VariantAllHandler : AdminHandler<RequestArgs>() {
  override fun args(): RequestArgs = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs) = withAdminUser(http) {
    http.json(Variants.selectAll().map(Variants::asJson).toList())
  }
}

data class VariantEditArgs(var id: String, var name: String, var tasksJson: String) : RequestArgs()

class VariantEditHandler : AdminHandler<VariantEditArgs>() {
  override fun args(): VariantEditArgs = VariantEditArgs("", "", "")

  override fun handle(http: HttpApi, argValues: VariantEditArgs) = withAdminUser(http) {
    when (argValues.id) {
      "" -> {
        Variants.insert {
          it[name] = argValues.name
          it[tasks_id_json_array] = argValues.tasksJson
        }
        http.ok()
      }
      else -> {
        Variants.update(where = { Variants.id eq argValues.id.toInt() }) {
          it[name] = argValues.name
          it[tasks_id_json_array] = argValues.tasksJson
        }
        http.ok()
      }
    }
  }
}
