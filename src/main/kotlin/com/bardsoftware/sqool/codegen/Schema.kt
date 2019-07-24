package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.contest.admin.Scripts
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Schema(private val id: Int) {
    private data class Data(val description: String, val body: String)

    private val data by lazy {
        transaction {
            val script = Scripts.select { Scripts.id eq id }.first()
            Data(script[Scripts.description], script[Scripts.body])
        }
    }
    val description
        get() = data.description
    val body
        get() = data.body

    override fun equals(other: Any?) = other is Schema && other.id == this.id

    override fun hashCode() = id
}