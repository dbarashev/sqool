package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class MatcherSpecTest {
    private val relationSpec: RelationSpec
    private val matcherSpec : MatcherSpec

    init {
        val keyAttribute = listOf(
                TaskResultColumn("ship", SqlDataType.TEXT),
                TaskResultColumn("port", SqlDataType.INT))
        val nonKeyAttributes = listOf(
                TaskResultColumn("transfers_num", SqlDataType.INT),
                TaskResultColumn("transfer_size", SqlDataType.BIGINT),
                TaskResultColumn("product", SqlDataType.TEXT))
        relationSpec = RelationSpec(keyAttribute, nonKeyAttributes)
        matcherSpec = MatcherSpec(relationSpec)
    }

    @Test
    fun testWrongKeyColsProjectionDefaultMessage() {
        assertEquals("Множество кортежей (ship, port) отличается от результатов робота", matcherSpec.wrongKeyColsProjMessage)
    }

    @Test
    fun testRightKeyColsProjectionDefaultMessage() {
        assertEquals("Кортежи (ship, port) найдены верно", matcherSpec.rightKeyColsProjMessage)
    }

    @Test
    fun testDiffErrorDefaultMessage() {
        val defaultDiffErrorMessage = matcherSpec.getDiffErrorMessage(TaskResultColumn("transfers_num", SqlDataType.INT))
        assertEquals("Максимальное расхождение transfers_num равно ", defaultDiffErrorMessage)
    }

    @Test
    fun testSetDiffErrorMessage() {
        val transfersNum = TaskResultColumn("transfers_num", SqlDataType.INT)
        val diffErrorMessage = "Максимальное расхождение количества перевозок равно "
        matcherSpec.setDiffErrorMessage(transfersNum, diffErrorMessage)
        assertEquals(diffErrorMessage, matcherSpec.getDiffErrorMessage(transfersNum))
    }

    @Test
    fun testSetNonNumericAttributeDiffErrorMessageThrowsException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            matcherSpec.setDiffErrorMessage(TaskResultColumn("product", SqlDataType.TEXT), "sth")
        }
        assertEquals("Non-numeric attributes can't have a difference error", exception.message)
    }

    @Test
    fun testSetKeyAttributeDiffErrorMessageThrowsException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            matcherSpec.setDiffErrorMessage(TaskResultColumn("port", SqlDataType.INT), "sth")
        }
        assertEquals("Non-key attributes can't have a difference error", exception.message)
    }

    @Test
    fun testSetNotExistingAttributeDiffErrorMessageThrowsException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            matcherSpec.setDiffErrorMessage(TaskResultColumn("foo", SqlDataType.INT), "sth")
        }
        assertEquals("No such non-key attribute in the relation", exception.message)
    }

    @Test
    fun testGetNonNumericAttributeDiffErrorMessageThrowsException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            matcherSpec.getDiffErrorMessage(TaskResultColumn("product", SqlDataType.TEXT))
        }
        assertEquals("Non-numeric attributes can't have a difference error", exception.message)
    }

    @Test
    fun testGetKeyAttributeDiffErrorMessageThrowsException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            matcherSpec.getDiffErrorMessage(TaskResultColumn("port", SqlDataType.INT))
        }
        assertEquals("Non-key attributes can't have a difference error", exception.message)
    }

    @Test
    fun testGetNotExistingAttributeDiffErrorMessageThrowsException() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            matcherSpec.getDiffErrorMessage(TaskResultColumn("foo", SqlDataType.INT))
        }
        assertEquals("No such non-key attribute in the relation", exception.message)
    }
}