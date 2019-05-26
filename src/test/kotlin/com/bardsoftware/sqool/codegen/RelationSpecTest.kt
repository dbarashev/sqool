package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class RelationSpecTest {
    @Test
    fun testConstructor() {
        RelationSpec(listOf(TaskResultColumn("id", SqlDataType.INT)))
    }

    @Test
    fun testDefaultNonKeyAttributes() {
        val relationSpec = RelationSpec(listOf(TaskResultColumn("id", SqlDataType.INT)))
        assertTrue(relationSpec.nonKeyCols.isEmpty())
    }

    @Test
    fun testNoKeyAttributesException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RelationSpec(emptyList(), listOf(TaskResultColumn("id", SqlDataType.INT)))
        }
        assertEquals("Key columns set can't be empty", exception.message)
    }

    @Test
    fun testDuplicateKeyAttributeException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            RelationSpec(listOf(TaskResultColumn("id", SqlDataType.INT), TaskResultColumn("id", SqlDataType.BIGINT)))
        }
        assertEquals("Column names must be unique", exception.message)
    }

    @Test
    fun testDuplicateNonKeyAttributeException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            val keyAttribute = listOf(TaskResultColumn("key", SqlDataType.TEXT))
            val nonKeyAttributes = listOf(TaskResultColumn("id", SqlDataType.INT), TaskResultColumn("id", SqlDataType.BIGINT))
            RelationSpec(keyAttribute, nonKeyAttributes)
        }
        assertEquals("Column names must be unique", exception.message)
    }

    @Test
    fun testDuplicateKeyAndNonKeyAttributeException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            val keyAttribute = listOf(TaskResultColumn("id", SqlDataType.TEXT))
            val nonKeyAttributes = listOf(TaskResultColumn("id", SqlDataType.INT))
            RelationSpec(keyAttribute, nonKeyAttributes)
        }
        assertEquals("Column names must be unique", exception.message)
    }
}

