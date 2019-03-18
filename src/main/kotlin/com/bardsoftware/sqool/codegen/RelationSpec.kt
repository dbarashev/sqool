package com.bardsoftware.sqool.codegen

import java.lang.IllegalArgumentException

class RelationSpec(val keyCols: Set<TaskResultColumn>,
                   val cols: Set<TaskResultColumn> = emptySet()
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

    fun getAllColsList(): List<TaskResultColumn> = keyCols.toList() + cols.toList()
}