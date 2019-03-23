package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.*
import com.bardsoftware.sqool.db.ContestDb
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
abstract class DbHandler<T: RequestArgs>(private val flags: Flags) : RequestHandler<T>(){

  val driver = JdbcSqliteDriver(
      name = "jdbc:postgresql://${flags.postgresAddress}:${flags.postgresPort}/${flags.postgresDatabase}",
      properties = Properties().also {
        it.setProperty("user", flags.postgresUser)
        it.setProperty("password", flags.postgresPassword)
      }
  )
  private val db = ContestDb(driver)

  fun withDatabase(work: (ContestDb) -> HttpResponse): HttpResponse {
    driver.execute(sql = "SET search_path=Contest;", parameters = 0, identifier = null)
    return work(db)
  }
}

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

data class TaskNewArgs(var name: String, var description: String, var result: String) : RequestArgs()
class TaskNewHandler(flags: Flags) : DbHandler<TaskNewArgs>(flags) {
  override fun args(): TaskNewArgs = TaskNewArgs(name = "", description = "", result = "")

  override fun handle(http: HttpApi, argValues: TaskNewArgs): HttpResponse {
    val resultSpecSql = argValues.result
    val resultSpecJson = resultSpecSql.split(",").mapIndexed { index, colSpec ->
      val (name, type) = colSpec.trim().split(Regex("\\s+"), limit = 2)
      return@mapIndexed """
        |{ "col_num": ${index + 1},
        |  "col_name": "$name",
        |  "col_type": "$type"
        |}""".trimMargin()
    }.joinToString(prefix = "[", postfix = "]")
    return withDatabase {
      // SQLDelight generates non-standard code which does not work with PostgreSQL
      // so we have to fallback to hand-made SQL here.
      driver.execute(1, """
        |INSERT INTO TaskDto(name, description, result_json)
        |VALUES (?, ?, ?)
        """.trimMargin(), 3) {
        bindString(1, argValues.name)
        bindString(2, argValues.description)
        bindString(3, resultSpecJson)
      }
      http.ok()
    }
  }
}
