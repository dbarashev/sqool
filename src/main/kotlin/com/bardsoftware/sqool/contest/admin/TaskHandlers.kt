package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.Flags
import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.db.ContestDb
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
open class DbHandler(private val flags: Flags) {

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

class TaskAllHandler(flags: Flags) : DbHandler(flags) {
  fun handle(http: HttpApi): HttpResponse {
    return withDatabase { db ->
      http.json(db.contestQueries.selectAllTasks().executeAsList())
    }
  }
}

class TaskNewHandler(flags: Flags) : DbHandler(flags) {
  fun handle(http: HttpApi): HttpResponse {
    return withDatabase {
      // SQLDelight generates non-standard code which does not work with PostgreSQL
      // so we have to fallback to hand-made SQL here.
      driver.execute(1, """
        |INSERT INTO TaskDto(name, description, result_json)
        |VALUES (?, ?, ?)
        """.trimMargin(), 3) {
        bindString(1, http.formValue("name") ?: "")
        bindString(2, http.formValue("description") ?: "")
        bindString(3, http.formValue("result") ?: "")
      }
      // This is not error, it just sends HTTP 200
      http.error(200)
    }
  }
}
