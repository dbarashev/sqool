package com.bardsoftware.sqool.contest.admin

import com.bardsoftware.sqool.contest.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Strings
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private val JSON_MAPPER = ObjectMapper()

object Variants : Table("Contest.VariantDto") {
    val id = integer("id").primaryKey()
    val name = text("name")
    val tasks_id_json_array = text("tasks_id_json_array")

    fun asJson(row: ResultRow): JsonNode {
        return JSON_MAPPER.createObjectNode().also {
            it.put("id", row[id])
            it.put("name", row[name])
            it.set("tasks", JSON_MAPPER.readTree(row[tasks_id_json_array]))
        }
    }
}

class VariantAllHandler(flags: Flags) : DbHandler<RequestArgs>(flags) {
    override fun args(): RequestArgs = RequestArgs()

    override fun handle(http: HttpApi, argValues: RequestArgs): HttpResponse {
        return transaction {
            http.json(Variants.selectAll().map(Variants::asJson).toList())
        }
    }
}

data class VariantEditArgs(var id: String, var name: String, var tasksJson: String) : RequestArgs()

class VariantEditHandler(flags: Flags) : DbHandler<VariantEditArgs>(flags) {
    override fun args(): VariantEditArgs = VariantEditArgs("", "", "")

    override fun handle(http: HttpApi, argValues: VariantEditArgs): HttpResponse = transaction {
        when (Strings.emptyToNull(argValues.id)) {
            null -> {
                Variants.insert {
                    it[name] = argValues.name
                    it[tasks_id_json_array] = argValues.tasksJson
                }
                http.ok()
            }
            else -> {
                Variants.update(where = {Variants.id eq argValues.id.toInt()}) {
                    it[name] = argValues.name
                    it[tasks_id_json_array] = argValues.tasksJson
                }
                http.ok()
            }
        }
    }
}