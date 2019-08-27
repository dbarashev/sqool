package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.RequestArgs
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*

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