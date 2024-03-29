/*
 * Copyright (c) BarD Software s.r.o 2021
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bardsoftware.sqool.bot

/**
 * @author dbarashev@bardsoftware.com
 */
import com.bardsoftware.libbotanique.DialogState
import com.bardsoftware.libbotanique.UserSessionStorage
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DSL.using
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.ThreadLocalTransactionProvider
import java.sql.ResultSet


val dataSource = HikariDataSource().apply {
  Class.forName("org.postgresql.Driver")
  username = System.getenv("PG_USER") ?: "postgres"
  //username = "hwmxsfem"
  val host = System.getenv("PG_HOST") ?: "localhost"
  //val host = "horton.elephantsql.com"
  val database = System.getenv("PG_DATABASE") ?: username
  //val database = username
  password = System.getenv("PG_PASSWORD") ?: ""
  //  password = "qq6wYuiYNhyEBTup7kVm9i_W_8lrYMPU"
  jdbcUrl = "jdbc:postgresql://$host:5432/$database"
  maximumPoolSize = 3
}

fun <T> executeQuery(query: String, code: (ResultSet) -> T): T {
  return dataSource.connection.use { conn ->
    conn.createStatement().use { stmt ->
      stmt.executeQuery(query).use { rs ->
        code(rs)
      }
    }
  }
}

val cp = DataSourceConnectionProvider(dataSource)
val databaseConfiguration = DefaultConfiguration()
    .set(cp)
    .set(SQLDialect.POSTGRES)
    .set(ThreadLocalTransactionProvider(cp, true))

fun <T> db(code: DSLContext.() -> T): T {
  return using(databaseConfiguration).run(code)
}

fun <T> txn(code: DSLContext.() -> T): T =
    db {
      transactionResult { ctx -> code(ctx.dsl()) }
    }

class UserSessionImpl(private val tgUserId: Long): UserSessionStorage {
  override val state: DialogState? = db {
    select(DSL.field("state_id", Int::class.java), DSL.field("data", String::class.java))
      .from("DialogState")
      .where(DSL.field("tg_id").eq(tgUserId))
      .firstOrNull()?.let {
        if (it.component1() == null) null else DialogState(it.component1(), it.component2())
      }
  }

  override fun reset() =
    db {
      deleteFrom(DSL.table("DialogState")).where(DSL.field("tg_id").eq(tgUserId)).execute()
      Unit
    }

  override fun save(stateId: Int, data: String) =
    txn {
      insertInto(DSL.table("DialogState"))
        .columns(
          DSL.field("tg_id", Long::class.java),
          DSL.field("state_id", Int::class.java),
          DSL.field("data", String::class.java)
        )
        .values(tgUserId, stateId, data)
        .onConflict(DSL.field("tg_id", Long::class.java)).doUpdate()
        .set(DSL.field("state_id", Int::class.java), stateId)
        .set(DSL.field("data", String::class.java), data)
        .execute()
      Unit
    }
}

fun userSessionProvider(tgUserId: Long) = UserSessionImpl(tgUserId)