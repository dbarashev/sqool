package com.bardsoftware.sqool.codegen

import java.lang.IllegalArgumentException

class MatcherSpec(val relationSpec: RelationSpec,
                  val wrongKeyColsProjMessage: String = "Множество кортежей ${relationSpec.keyCols
                          .joinToString(", ", "(", ")") { it.name }} отличется от результатов робота",
                  val rightKeyColsProjMessage: String = "Кортежи ${relationSpec.keyCols
                          .joinToString(", ", "(", ")") { it.name }} найдены верно"
) {
    private val diffErrorMessage = mutableMapOf<String, String>()

    fun setDiffErrorMessage(column: TaskResultColumn, message: String) {
        canHasDiffError(column)
        diffErrorMessage[column.name] = message
    }

    fun getDiffErrorMessage(column: TaskResultColumn): String {
        canHasDiffError(column)
        return diffErrorMessage.getOrDefault(column.name, "Максимальное расхождение ${column.name} равно ")
    }

    private fun canHasDiffError(column: TaskResultColumn) {
        if (!column.type.isNumeric) {
            throw IllegalArgumentException("Non-numeric attributes can't have a difference error")
        }
        if (relationSpec.keyCols.contains(column)) {
            throw IllegalArgumentException("Non-key attributes can't have a difference error")
        }
        if (!relationSpec.cols.contains(column)) {
            throw IllegalArgumentException("No such non-key attribute in the relation")
        }
    }
}

