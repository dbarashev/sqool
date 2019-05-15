package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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

class ScriptAllHandler : RequestHandler<RequestArgs>() {
  override fun args(): RequestArgs = RequestArgs()

  override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
    return transaction {
      http.json(Scripts.selectAll().map(Scripts::asJson).toList())
    }
  }
}

private val JSON_MAPPER = ObjectMapper()
