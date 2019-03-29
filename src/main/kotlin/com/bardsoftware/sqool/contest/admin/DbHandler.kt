package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.Flags
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
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
