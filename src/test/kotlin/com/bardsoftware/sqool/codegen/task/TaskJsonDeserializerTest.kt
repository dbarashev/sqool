package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.CodeGenerator
import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TaskJsonDeserializerTest {
    @Test
    fun testDeserializeScalarValueTask() {
        val json = "[{\"name\":\"task1\", \"keyAttributes\":[{\"type\":\"INT\"}],\"attributes\":[],\"solution\":\"solution\"}]"
        val expectedTask = ScalarValueTask("task1", "solution", SqlDataType.INT)

        val tasks = deserializeJsonTasks(json)
        assertEquals(1, tasks.size)
        assertTrue(expectedTask.equalsTo(tasks[0]))
    }

    @Test
    fun testDeserializeSingleColumnTask() {
        val json = "[{\"name\":\"task1\", \"keyAttributes\":[{\"name\":\"id\", \"type\":\"DOUBLE PRECISION\"}]," +
                "\"attributes\":[],\"solution\":\"solution\"}]"
        val expectedTask = SingleColumnTask(
                "task1", "solution", TaskResultColumn("id", SqlDataType.DOUBLE_PRECISION))

        val tasks = deserializeJsonTasks(json)
        assertEquals(1, tasks.size)
        assertTrue(expectedTask.equalsTo(tasks[0]))
    }

    @Test
    fun testDeserializeMultiColumnTask() {
        val json = "[{\"name\":\"task1\", \"keyAttributes\":[{\"name\":\"id\", \"type\":\"DOUBLE PRECISION\"}]," +
                "\"attributes\":[{\"name\":\"text\", \"type\":\"TEXT\"}, {\"name\":\"num\", \"type\":\"INT\"}],\"solution\":\"solution\"}]"
        val relationSpec = RelationSpec(
                listOf(TaskResultColumn("id", SqlDataType.DOUBLE_PRECISION)),
                listOf(TaskResultColumn("text", SqlDataType.TEXT), TaskResultColumn("num", SqlDataType.INT)))
        val matcherSpec = MatcherSpec(relationSpec)
        val expectedTask = MultiColumnTask("task1", "solution", matcherSpec)

        val tasks = deserializeJsonTasks(json)
        assertEquals(1, tasks.size)
        assertTrue(expectedTask.equalsTo(tasks[0]))
    }

    @Test
    fun testDeserializeMultipleTask() {
        val json = "[{\"name\":\"task1\", \"keyAttributes\":[{\"type\":\"INT\"}],\"attributes\":[],\"solution\":\"solution\"}," +
                "{\"name\":\"task1\", \"keyAttributes\":[{\"name\":\"id\", \"type\":\"DOUBLE PRECISION\"}]," +
                "\"attributes\":[],\"solution\":\"solution\"}]"
        val expectedTask1 = ScalarValueTask("task1", "solution", SqlDataType.INT)
        val expectedTask2 = SingleColumnTask("task1", "solution", TaskResultColumn("id", SqlDataType.DOUBLE_PRECISION))

        val tasks = deserializeJsonTasks(json)
        assertEquals(2, tasks.size)
        assertTrue(expectedTask1.equalsTo(tasks[0]))
        assertTrue(expectedTask2.equalsTo(tasks[1]))
    }

    @Test
    fun testUnexpectedField() {
        val json = "[{\"name\":\"task1\", \"field\":\"task1\", \"keyAttributes\":[{\"type\":\"INT\"}]," +
                "\"attributes\":[],\"solution\":\"solution\"}]"
        val exception = assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
        assertEquals("Unrecognized task field specified", exception.message)
    }

    @Test
    fun testMissingField() {
        val json = "[{\"keyAttributes\":[{\"type\":\"INT\"}]," +
                "\"attributes\":[],\"solution\":\"solution\"}]"
        val exception = assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
        assertEquals("Required task filed is missing", exception.message)
    }

    @Test
    fun testMalformedJson() {
        val json = "[{\"name\":\"task1\" \"keyAttributes\":[{\"type\":\"INT\"}]," +
                "\"attributes\":[],\"solution\":\"solution\"}]"
        val exception = assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
        assertEquals("Malformed JSON", exception.message)
    }

    @Test
    fun testMissingAttributeName() {
        val json = "[{\"name\":\"task1\", \"keyAttributes\":[{\"name\":\"id\",\"type\":\"INT\"}, {\"type\":\"INT\"}]," +
                "\"attributes\":[],\"solution\":\"solution\"}]"
        val exception = assertThrows(TaskDeserializationException::class.java) {
            deserializeJsonTasks(json)
        }
        assertEquals("Attribute's name isn't specified", exception.message)
    }

    private fun Task.equalsTo(task: Task): Boolean {
        val generator = CodeGenerator("hse2019", "/workspace/hse2019/cw2/schema2.sql")
        return this.generateDynamicCode(generator) == task.generateDynamicCode(generator)
                && this.generateStaticCode() == task.generateStaticCode()
    }
}