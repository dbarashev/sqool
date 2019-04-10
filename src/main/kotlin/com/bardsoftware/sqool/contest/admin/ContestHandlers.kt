package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.format.DateTimeFormatterBuilder

private val DATE_FORMATTER = DateTimeFormatterBuilder()
    .appendYear(2, 4).appendLiteral('-')
    .appendMonthOfYear(2).appendLiteral('-')
    .appendDayOfMonth(2).appendLiteral(' ')
    .appendHourOfDay(2).appendLiteral(':')
    .appendMinuteOfHour(2).toFormatter()

private val JSON_MAPPER = ObjectMapper()

object Contests : Table("Contest.ContestDto") {
  val code = text("code").primaryKey()
  val name = text("name")
  val start_ts = datetime("start_ts")
  val end_ts = datetime("end_ts")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("code", row[Contests.code])
      it.put("name", row[Contests.name])
      it.put("start_ts", row[Contests.start_ts].toString(DATE_FORMATTER))
      it.put("end_ts", row[Contests.end_ts].toString(DATE_FORMATTER))
    }
  }
}

class ContestAllHandler : RequestHandler<RequestArgs>() {
  override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
    return transaction {
      http.json(Contests.selectAll().map(Contests::asJson).toList())
    }
  }

  override fun args(): RequestArgs {
    return RequestArgs()
  }
}

data class ContestNewArgs(var code: String, var name: String, var start_ts: String, var end_ts: String) : RequestArgs()

class ContestNewHandler : RequestHandler<ContestNewArgs>() {
  override fun args(): ContestNewArgs = ContestNewArgs("", "", "", "")

  override fun handle(http: HttpApi, argValues: ContestNewArgs): HttpResponse {
    return transaction {
      Contests.insert {
        it[code] = argValues.code
        it[name] = argValues.name
        it[start_ts] = DATE_FORMATTER.parseDateTime(argValues.start_ts)
        it[end_ts] = DATE_FORMATTER.parseDateTime(argValues.end_ts)
      }
      http.ok()
    }
  }
}
