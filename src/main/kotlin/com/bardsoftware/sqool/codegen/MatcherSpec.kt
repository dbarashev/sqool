package com.bardsoftware.sqool.codegen

class MatcherSpec(val relationSpec: RelationSpec,
                  val wrongKeyColsProjMessage: String = "Множество кортежей ${relationSpec.keyCols
                          .joinToString(", ", "(", ")") { it.name }} отличается от результатов робота",
                  val rightKeyColsProjMessage: String = "Кортежи ${relationSpec.keyCols
                          .joinToString(", ", "(", ")") { it.name }} найдены верно"
) {
    private val diffErrorMessage = mutableMapOf<String, String>()

    fun setDiffErrorMessage(column: TaskResultColumn, message: String) {
        canHaveDiffError(column)
        diffErrorMessage[column.name] = message
    }

    fun getDiffErrorMessage(column: TaskResultColumn): String {
        canHaveDiffError(column)
        return diffErrorMessage.getOrDefault(column.name, "Максимальное расхождение ${column.name} равно ")
    }

    private fun canHaveDiffError(column: TaskResultColumn) {
        if (column.type.kind == SqlDataType.Kind.NON_NUMERIC) {
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

