package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.codegen.*
import com.bardsoftware.sqool.codegen.docker.ContestImageManager
import com.bardsoftware.sqool.codegen.docker.ImageCheckResult
import com.bardsoftware.sqool.contest.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.format.DateTimeFormatterBuilder
import java.io.ByteArrayOutputStream

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
  val variants_id_json_array = text("variants_id_json_array")

  fun asJson(row: ResultRow): JsonNode {
    return JSON_MAPPER.createObjectNode().also {
      it.put("code", row[code])
      it.put("name", row[name])
      it.put("start_ts", row[start_ts].toString(DATE_FORMATTER))
      it.put("end_ts", row[end_ts].toString(DATE_FORMATTER))
      it.set<JsonNode>("variants", JSON_MAPPER.readTree(row[variants_id_json_array]))
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

enum class ContestEditMode { INSERT, UPDATE }
data class ContestEditArgs(
        var code: String, var name: String,
        var start_ts: String, var end_ts: String,
        var variants: String
) : RequestArgs()

class ContestEditHandler(private val mode: ContestEditMode) : RequestHandler<ContestEditArgs>() {
  override fun args(): ContestEditArgs = ContestEditArgs("", "", "", "", "")

  override fun handle(http: HttpApi, argValues: ContestEditArgs): HttpResponse {
    return transaction {
      fun prepareStmt(it: UpdateBuilder<Number>) {
        it[Contests.code] = argValues.code
        it[Contests.name] = argValues.name
        it[Contests.start_ts] = DATE_FORMATTER.parseDateTime(argValues.start_ts)
        it[Contests.end_ts] = DATE_FORMATTER.parseDateTime(argValues.end_ts)
        it[Contests.variants_id_json_array] = argValues.variants
      }
      when (mode) {
        ContestEditMode.INSERT -> Contests.insert {
          prepareStmt(it)
        }
        ContestEditMode.UPDATE -> Contests.update {
          prepareStmt(it)
        }
      }
      http.ok()
    }
  }
}

data class ContestBuildArgs(var code: String) : RequestArgs()

class ContestBuildHandler(
        private val queryManager: DbQueryManager,
        private val imageManager: (Contest) -> ContestImageManager
) : RequestHandler<ContestBuildArgs>() {

  override fun args(): ContestBuildArgs = ContestBuildArgs("")

  override fun handle(http: HttpApi, argValues: ContestBuildArgs) = try {
      val contest = queryManager.findContest(argValues.code)
      val imageManager = imageManager(contest)
      imageManager.createImage()

      val errorStream = ByteArrayOutputStream()
      when (imageManager.checkImage(errorStream)) {
        ImageCheckResult.PASSED -> http.json(mapOf("status" to "OK"))
        ImageCheckResult.ERROR -> http.error(500, errorStream.toString())
        ImageCheckResult.FAILED -> http.json(mapOf("status" to "ERROR", "message" to errorStream.toString()))
      }.also { errorStream.close() }
    } catch (exception: MalformedDataException) {
      exception.printStackTrace()
      http.error(400, exception.message, exception)
    } catch (exception: NoSuchContestException) {
      exception.printStackTrace()
      http.error(404, "No such contest")
    }
}
