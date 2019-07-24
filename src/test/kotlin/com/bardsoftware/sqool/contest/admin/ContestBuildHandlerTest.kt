package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.Flags
import com.bardsoftware.sqool.contest.Http
import com.nhaarman.mockito_kotlin.*
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

class ContestBuildHandlerTest {
    private val handler = ContestBuildHandler(flags)
    private val httpMock = mock<Http>()

    companion object {
        private val flags = mock<Flags> {
            on { postgresAddress } doReturn (System.getProperty("postgres.ip") ?: "localhost")
            on { postgresPort } doReturn "5432"
            on { postgresUser } doReturn "postgres"
            on { postgresPassword } doReturn ""
        }

        @BeforeAll
        @JvmStatic
        fun setUpDB() {
            val dataSource = HikariDataSource().apply {
                username = flags.postgresUser
                password = flags.postgresPassword
                jdbcUrl = "jdbc:postgresql://${flags.postgresAddress}:${flags.postgresPort}/${flags.postgresUser}"
            }
            val createSchemaSql = """
                DROP SCHEMA IF EXISTS Contest CASCADE; 
                CREATE SCHEMA Contest;
                """.trimIndent()
            dataSource.connection.createStatement().execute(createSchemaSql)
            Database.connect(dataSource)
            transaction {
                SchemaUtils.create(Contests, Variants, Tasks)
            }
        }
    }

    @Test
    fun testHandleNotExistingContest() {
        val args = ContestBuildArgs("not_existing")
        handler.handle(httpMock, args)

        verify(httpMock).error(404, "No such contest", null)
        verify(httpMock, never()).ok()
        verify(httpMock, never()).json(any())
    }

    @Test
    fun testHandleCorrectContest() {
        transaction {
            Contests.insert {
                it[code] = "empty"
                it[name] = "empty"
                it[start_ts] = DateTime.now()
                it[end_ts] = DateTime.now()
                it[variants_id_json_array] = "[]"
            }
        }
        val args = ContestBuildArgs("empty")
        handler.handle(httpMock, args)

        verify(httpMock, never()).error(any(), any(), anyOrNull())
        verify(httpMock, never()).ok()
        verify(httpMock).json(mapOf("status" to "OK"))
    }

    @Test
    fun testHandleMalformedTaskSolutionContest() {
        val resultJson = """[{
            "name": "",
            "type": "INT"
            }]""".trimIndent()
        insertContest(1, "invalid", resultJson, "SELECTY")
        val args = ContestBuildArgs("1")
        handler.handle(httpMock, args)

        verify(httpMock, never()).error(any(), any(), anyOrNull())
        verify(httpMock, never()).ok()
        val errorMessage = """
            Static code testing:
            Invalid sql:
            psql:/workspace/invalid/invalid/static.sql:1: NOTICE:  schema "invalid" does not exist, skipping
            DROP SCHEMA
            CREATE SCHEMA
            SET
            psql:/workspace/invalid/invalid/static.sql:9: ERROR:  syntax error at or near "SELECTY"
            LINE 3: SELECTY
                    ^
            CREATE FUNCTION
            CREATE FUNCTION
            DROP FUNCTION
            
            
            """.trimIndent()
        verify(httpMock).json(mapOf("status" to "ERROR", "message" to errorMessage))
    }

    @Test
    fun testHandleMalformedTaskResultContest() {
        val resultJson = """[{
            "nam": "",
            }]""".trimIndent()
        insertContest(2, "invalid", resultJson, "SELECT 12;")
        val args = ContestBuildArgs("2")
        handler.handle(httpMock, args)

        verify(httpMock).error(eq(400), any(), anyOrNull())
        verify(httpMock, never()).ok()
        verify(httpMock, never()).json(any())
    }

    private fun insertContest(id: Int, name: String, resultJson: String, solution: String) = transaction {
        Tasks.insert {
            it[Tasks.id] = id
            it[Tasks.name] = name
            it[description] = ""
            it[result_json] = resultJson
            it[Tasks.solution] = solution
        }
        Variants.insert {
            it[Variants.id] = id
            it[Variants.name] = name
            it[tasks_id_json_array] = "[$id]"
            it[scripts_id_json_array] = "[]"
        }
        Contests.insert {
            it[code] = id.toString()
            it[Contests.name] = name
            it[start_ts] = DateTime.now()
            it[end_ts] = DateTime.now()
            it[variants_id_json_array] = "[$id]"
        }
    }
}
