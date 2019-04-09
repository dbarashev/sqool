package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import java.lang.Exception
import java.lang.IllegalArgumentException

fun deserializeJsonTasks(jsonArray: String): List<Task> {
    val mapper = ObjectMapper()
    return try {
        val task = mapper.readValue<List<TaskDto>>(jsonArray, object : TypeReference<List<TaskDto>>() {})
        task.map { it.toTask() }
    } catch (exception: Exception) {
        when (exception) {
            is UnrecognizedPropertyException, is InvalidDefinitionException, is JsonMappingException,
            is TypeCastException, is IllegalArgumentException -> throw throw TaskDeserializationException(exception)
            else -> throw exception
        }
    }
}

class TaskDeserializationException(cause: Throwable?) : Exception(cause)

class TaskDto {
    val name = ""
    val solution: String = ""
    val keyAttributes = emptyList<AttributeDto>()
    val nonKeyAttributes = emptyList<AttributeDto>()

    fun toTask(): Task {
        if (!isValid()) {
            throw TaskDeserializationException(null)
        }

        if (keyAttributes.size == 1 && keyAttributes[0].name == null) {
            return ScalarValueTask(name, solution, SqlDataType.getEnum(keyAttributes[0].type))
        }

        if (keyAttributes.size == 1 && nonKeyAttributes.isEmpty()) {
            val type = SqlDataType.getEnum(keyAttributes[0].type)
            val column = TaskResultColumn(keyAttributes[0].name as String, type)
            return SingleColumnTask(name, solution, column)
        }

        return buildMultiColumnTask()
    }

    private fun isValid() = !name.isEmpty() && !solution.isEmpty()

    private fun buildMultiColumnTask(): Task {
        val keyAttributes = keyAttributes.map { it.toTaskResultColumn() }
        val nonKeyAttributes = nonKeyAttributes.map { it.toTaskResultColumn() }
        val relationSpec = RelationSpec(keyAttributes, nonKeyAttributes)
        val matcherSpec = MatcherSpec(relationSpec)
        return MultiColumnTask(name, solution, matcherSpec)
    }

    class AttributeDto {
        val name: String? = null
        val type: String = ""

        fun toTaskResultColumn(): TaskResultColumn = TaskResultColumn(name as String, SqlDataType.getEnum(type))
    }
}