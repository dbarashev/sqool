package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.bardsoftware.sqool.contest.admin.Scripts
import com.bardsoftware.sqool.contest.admin.Tasks
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Exception
import java.lang.IllegalArgumentException

fun resultRowToTask(row: ResultRow): Task {
    return try {
        val attributesJson = row[Tasks.result_json]
        val keyAttributes = ObjectMapper().readValue<List<AttributeDto>>(
                attributesJson, object : TypeReference<List<AttributeDto>>() {}
        )
        TaskDto(row[Tasks.name], row[Tasks.solution], row[Tasks.script_id], keyAttributes).toTask()
    } catch (exception: Exception) {
        when (exception) {
            is UnrecognizedPropertyException, is InvalidDefinitionException, is JsonMappingException,
            is TypeCastException, is IllegalArgumentException -> throw TaskDeserializationException(exception.message, exception)
            else -> throw exception
        }
    }
}

class TaskDeserializationException : Exception {
    constructor(message: String?, cause: Throwable) : super(message, cause)

    constructor(message: String?) : super(message)
}

class TaskDto(
        private val name: String,
        private val solution: String,
        private val schemaId: Int?,
        private val keyAttributes: List<AttributeDto>,
        private val nonKeyAttributes: List<AttributeDto> = emptyList()
) {
    private val schema = getSchema()

    private fun getSchema() = if (schemaId == null) null else {
        val schema = transaction {
            Scripts.select { Scripts.id eq schemaId }
                    .map { Schema(it[Scripts.description], it[Scripts.body]) }
        }
        if (schema.isEmpty()) {
            throw TaskDeserializationException("Schema for task $name not found")
        }
        schema.first()
    }

    fun toTask(): Task {
        if (!isValid()) {
            throw TaskDeserializationException("Invalid task json")
        }

        if (keyAttributes.size == 1 && keyAttributes[0].name.isEmpty()) {
            return ScalarValueTask(name, solution, schema, SqlDataType.getEnum(keyAttributes[0].type))
        }

        if (keyAttributes.size == 1 && nonKeyAttributes.isEmpty()) {
            val type = SqlDataType.getEnum(keyAttributes[0].type)
            val column = TaskResultColumn(keyAttributes[0].name, type)
            return SingleColumnTask(name, solution, schema, column)
        }

        return buildMultiColumnTask()
    }

    private fun isValid() = name.isNotEmpty() && solution.isNotEmpty()

    private fun buildMultiColumnTask(): Task {
        val keyAttributes = keyAttributes.map { it.toTaskResultColumn() }
        val nonKeyAttributes = nonKeyAttributes.map { it.toTaskResultColumn() }
        val relationSpec = RelationSpec(keyAttributes, nonKeyAttributes)
        val matcherSpec = MatcherSpec(relationSpec)
        return MultiColumnTask(name, solution, schema, matcherSpec)
    }
}

class AttributeDto {
    val name: String = ""
    val type: String = ""

    fun toTaskResultColumn(): TaskResultColumn = TaskResultColumn(name, SqlDataType.getEnum(type))
}