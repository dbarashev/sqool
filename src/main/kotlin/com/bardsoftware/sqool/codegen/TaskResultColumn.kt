package com.bardsoftware.sqool.codegen

data class TaskResultColumn(val name: String, val type: SqlDataType)

/**
 * Basic data types occurred in task results.
 */
enum class SqlDataType {
    INT, BIGINT, NUMERIC, TEXT, BOOLEAN,
    DOUBLE_PRECISION {
        override fun toString(): String {
            return "DOUBLE PRECISION"
        }
    }
}
