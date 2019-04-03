package com.bardsoftware.sqool.codegen.task.spec

class RelationSpec(val keyCols: List<TaskResultColumn>,
                   val cols: List<TaskResultColumn> = emptyList()
) {
    init {
        if (keyCols.isEmpty()) {
            throw IllegalArgumentException("Key columns set can't be empty")
        }

        val colNames = keyCols.map { it.name } + cols.map { it.name }
        if (colNames.size != colNames.toSet().size) {
            throw IllegalArgumentException("Column names must be unique")
        }
    }

    fun getAllColsList(): List<TaskResultColumn> = keyCols + cols
}