package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class TaskJsonDeserializer : JsonDeserializer<Task> {
    override fun deserialize(task: JsonElement, typeOfSrc: Type, context: JsonDeserializationContext): Task {
        val taskObject = task.asJsonObject
        val name = taskObject["name"].asString
        val robotQuery = taskObject["solution"].asString
        val keyAttributes = taskObject["keyAttributes"].asJsonArray

        if(keyAttributes.size() == 1 && !keyAttributes[0].asJsonObject.has("name")) {
            val type = keyAttributes[0].asJsonObject["type"].asString
            return ScalarValueTask(name, robotQuery, SqlDataType.getEnum(type))
        }

        val attributes = taskObject["attributes"].asJsonArray
        if(keyAttributes.size() == 1 && attributes.size()== 0) {
            val spec = keyAttributes[0].asJsonObject
            val type = spec["type"].asString
            val column = TaskResultColumn(spec["name"].asString, SqlDataType.getEnum(type))
            return SingleColumnTask(name, robotQuery, column)
        }

        return buildMultiColumnTask(name, robotQuery, parseSpecList(keyAttributes), parseSpecList(attributes))
    }

    private fun parseSpecList(jsonArray: JsonArray): List<TaskResultColumn> = jsonArray
            .map { it.asJsonObject }
            .map {
                val type = it["type"].asString
                TaskResultColumn(it["name"].asString, SqlDataType.getEnum(type))
            }

    private fun buildMultiColumnTask(name: String, robotQuery: String,
                                     keyCols: List<TaskResultColumn>,
                                     cols: List<TaskResultColumn>
    ): Task {
        val relationSpec = RelationSpec(keyCols, cols)
        val matcherSpec = MatcherSpec(relationSpec)
        return MultiColumnTask(name, robotQuery, matcherSpec)
    }
}