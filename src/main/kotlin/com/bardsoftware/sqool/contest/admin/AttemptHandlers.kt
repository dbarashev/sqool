package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.HttpApi
import com.bardsoftware.sqool.contest.HttpResponse
import com.bardsoftware.sqool.contest.RequestArgs
import com.bardsoftware.sqool.contest.RequestHandler
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object Attempts : Table("Contest.Attempt") {
  val task_id = integer("task_id")
  val user_id = integer("user_id")
  val attempt_text = integer("attempt_text")
}

data class AttemptArgs(var user_id: String, var task_id: String) : RequestArgs()
class AttemptHandler : RequestHandler<AttemptArgs>() {
  override fun handle(http: HttpApi, argValues: AttemptArgs): HttpResponse {
    return transaction {
      http.json(hashMapOf("attempt_text" to "attempt must be in this place"))
    }
  }

  override fun args(): AttemptArgs = AttemptArgs("", "")
}

