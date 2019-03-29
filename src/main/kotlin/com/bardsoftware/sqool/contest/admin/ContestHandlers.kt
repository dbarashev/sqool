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
import javax.sql.CommonDataSource

object Contests : Table("Contest.ContestDto") {
  val code = text("code").primaryKey()
  val name = text("name")
  val start_ts = datetime("start_ts")
  val end_ts = datetime("end_ts")

  fun asJson(row: ResultRow): JsonNode {
    return mapper.createObjectNode().also {
      it.put("code", row[Contests.code])
      it.put("name", row[Contests.name])
      it.put("start_ts", row[Contests.start_ts].millis)
      it.put("end_ts", row[Contests.end_ts].millis)
    }
  }
}

val mapper = ObjectMapper()

/**
 * @author dbarashev@bardsoftware.com
 */
class ContestAllHandler(dataSource: CommonDataSource) : RequestHandler<RequestArgs>() {
  override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
    return transaction {
      http.json(Contests.selectAll().map(Contests::asJson).toList())
    }
  }

  override fun args(): RequestArgs {
    return RequestArgs()
  }
}
