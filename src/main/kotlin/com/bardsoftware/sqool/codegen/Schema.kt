package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.contest.admin.Scripts
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Schema(private val id: Int) {
    private lateinit var description: String
    private lateinit var body: String

    fun getDescription(): String {
        if (!this::description.isInitialized) {
            init()
        }
        return description
    }

    fun getBody(): String {
        if (!this::body.isInitialized) {
            init()
        }
        return body
    }

    private fun init() = transaction {
        Scripts.select {
            Scripts.id eq id
        }.forEach {
            description = it[Scripts.description]
            body = it[Scripts.body]
        }
    }

    override fun equals(other: Any?) = other is Schema && other.id == id

    override fun hashCode() = id
}