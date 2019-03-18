package com.bardsoftware.sqool.codegen

data class TaskResultColumn(val name: String, val type: SqlDataType) {
    override fun toString(): String = "$name $type"
}

/**
 * Basic data types occurred in task results.
 */
enum class SqlDataType(val isNumeric: Boolean = true) {
    INT, BIGINT, NUMERIC, TEXT(false), BOOLEAN(false),
    DOUBLE_PRECISION {
        override fun toString(): String = "DOUBLE PRECISION"
    }
}
