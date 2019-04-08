package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.fasterxml.jackson.annotation.JsonProperty
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
        val task = mapper.readValue<List<TaskDto>>(jsonArray, object: TypeReference<List<TaskDto>>() {})
        task.map { it.toTask() }
    } catch (exception: TypeCastException) {
        throw TaskDeserializationException("Attribute's name isn't specified", exception)
    } catch (exception: UnrecognizedPropertyException) {
        throw TaskDeserializationException("Unrecognized task field specified", exception)
    } catch (exception: InvalidDefinitionException) {
        throw TaskDeserializationException("Required task filed is missing", exception)
    } catch (exception: JsonMappingException) {
        throw TaskDeserializationException("Malformed JSON", exception)
    } catch (exception: IllegalArgumentException) {
        throw TaskDeserializationException(exception.message, exception)
    }
}

class TaskDeserializationException(message: String?, cause: Throwable) : Exception(message, cause)

data class TaskDto(@JsonProperty(value = "name") val name: String,
                   @JsonProperty(value = "solution") val solution: String,
                   @JsonProperty(value = "keyAttributes") val keyAttributes: List<AttributeDto>,
                   @JsonProperty(value = "attributes") val attributes: List<AttributeDto>
) {
    private constructor() : this("", "", emptyList(), emptyList())

    fun toTask(): Task {
        if(keyAttributes.size == 1 && keyAttributes[0].name == null) {
            return ScalarValueTask(name, solution, SqlDataType.getEnum(keyAttributes[0].type))
        }

        if(keyAttributes.size == 1 && attributes.isEmpty()) {
            val type = SqlDataType.getEnum(keyAttributes[0].type)
            val column = TaskResultColumn(keyAttributes[0].name as String, type)
            return SingleColumnTask(name, solution, column)
        }

        return buildMultiColumnTask()
    }

    private fun buildMultiColumnTask(): Task {
        val keyAttributes = keyAttributes.map { it.toTaskResultColumn() }
        val attributes = attributes.map { it.toTaskResultColumn() }
        val relationSpec = RelationSpec(keyAttributes, attributes)
        val matcherSpec = MatcherSpec(relationSpec)
        return MultiColumnTask(name, solution, matcherSpec)
    }

    data class AttributeDto(@JsonProperty(value = "name") val name: String?,
                            @JsonProperty(value = "type") val type: String
    ) {
        private constructor() : this(null, "")

        fun toTaskResultColumn(): TaskResultColumn = TaskResultColumn(name as String, SqlDataType.getEnum(type))
    }
}