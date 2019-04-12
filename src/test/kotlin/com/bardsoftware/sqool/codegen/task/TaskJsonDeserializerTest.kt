package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TaskJsonDeserializerTest {
    @Test
    fun testDeserializeScalarValueTask() {
        val json = """
            [{
                "name":"task1",
                "keyAttributes":[
                    {"type":"INT"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        val expectedTask = ScalarValueTask("task1", "solution", SqlDataType.INT)

        val tasks = deserializeJsonTasks(json)
        assertEquals(1, tasks.size)
        assertEquals(expectedTask, tasks[0])
    }

    @Test
    fun testDeserializeSingleColumnTask() {
        val json = """
            [{
                "name":"task1",
                "keyAttributes":[
                    {"name":"id", "type":"DOUBLE PRECISION"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        val expectedTask = SingleColumnTask(
                "task1", "solution", TaskResultColumn("id", SqlDataType.DOUBLE_PRECISION))

        val tasks = deserializeJsonTasks(json)
        assertEquals(1, tasks.size)
        assertEquals(expectedTask, tasks[0])
    }

    @Test
    fun testDeserializeMultiColumnTask() {
        val json = """
            [{
                "name":"task1",
                "keyAttributes":[
                    {"name":"id", "type":"DOUBLE PRECISION"}
                ],
                "nonKeyAttributes":[
                    {"name":"text", "type":"TEXT"},
                    {"name":"num", "type":"INT"}
                ],
                "solution":"solution"
            }]
            """
        val relationSpec = RelationSpec(
                listOf(TaskResultColumn("id", SqlDataType.DOUBLE_PRECISION)),
                listOf(TaskResultColumn("text", SqlDataType.TEXT), TaskResultColumn("num", SqlDataType.INT)))
        val matcherSpec = MatcherSpec(relationSpec)
        val expectedTask = MultiColumnTask("task1", "solution", matcherSpec)

        val tasks = deserializeJsonTasks(json)
        assertEquals(1, tasks.size)
        assertEquals(expectedTask, tasks[0])
    }

    @Test
    fun testDeserializeMultipleTask() {
        val json = """
            [{
                "name":"task1",
                "keyAttributes":[
                    {"type":"INT"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            },
            {
                "name":"task1",
                "keyAttributes":[
                    {"name":"id", "type":"DOUBLE PRECISION"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        val expectedTask1 = ScalarValueTask("task1", "solution", SqlDataType.INT)
        val expectedTask2 = SingleColumnTask("task1", "solution", TaskResultColumn("id", SqlDataType.DOUBLE_PRECISION))

        val tasks = deserializeJsonTasks(json)
        assertEquals(2, tasks.size)
        assertEquals(expectedTask1, tasks[0])
        assertEquals(expectedTask2, tasks[1])
    }

    @Test
    fun testUnexpectedField() {
        val json = """
            [{
                "name":"task1",
                "field":"task1",
                "keyAttributes":[
                    {"type":"INT"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
    }

    @Test
    fun testMissingField() {
        val json = """
            [{
                "keyAttributes":[
                    {"type":"INT"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
    }

    @Test
    fun testMalformedJson() {
        val json = """
            [{
                "name":"task1"
                "keyAttributes":[
                    {"type":"INT"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
    }

    @Test
    fun testMissingAttributeName() {
        val json = """
            [{
                "name":"task1",
                "keyAttributes":[
                    {"name":"id", "type":"DOUBLE PRECISION"}
                    {"type":"INT"}
                ],
                "nonKeyAttributes":[],
                "solution":"solution"
            }]
            """
        assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
    }
}