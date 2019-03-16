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
class TaskAllHandler(private val flags: Flags) {
  fun handle(http: HttpApi): HttpResponse {
    val driver = JdbcSqliteDriver(
        name = "jdbc:postgresql://${flags.postgresAddress}:${flags.postgresPort}/${flags.postgresDatabase}",
        properties = Properties().also {
          it.setProperty("user", flags.postgresUser)
          it.setProperty("password", flags.postgresPassword)
        }
    )
    val db = ContestDb(driver)
    driver.execute(sql = "SET search_path=Contest;", parameters = 0, identifier = null)
    val tasks = db.contestQueries.selectAllTasks().executeAsList()
    return http.json(tasks)
  }
}
