package com.bardsoftware.sqool.codegen

data class TaskResultSpec(val setName: String, val setType: SqlDataType)

/**
 * Basic data types occurred in task results.
 */
enum class SqlDataType {
    INT, TEXT, BOOLEAN,
    DOUBLE_PRECISION {
        override fun toString(): String {
            return "DOUBLE PRECISION"
        }
    }
}
