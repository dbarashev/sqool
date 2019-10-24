package com.bardsoftware.sqool.codegen.task.spec

data class TaskResultColumn(val name: String, val type: SqlDataType) {
  override fun toString(): String = "$name $type"
}

/**
 * Basic data types occurred in task results.
 */
enum class SqlDataType(val kind: Kind) {
  INT(Kind.INTEGER),
  BIGINT(Kind.INTEGER),
  NUMERIC(Kind.DECIMAL),
  TEXT(Kind.NON_NUMERIC),
  BOOLEAN(Kind.NON_NUMERIC),
  DOUBLE_PRECISION(Kind.DECIMAL) {
    override fun toString(): String = "DOUBLE PRECISION"
  };

  companion object {
    fun getEnum(value: String): SqlDataType =
        when (value) {
          "DOUBLE PRECISION" -> DOUBLE_PRECISION
          else -> valueOf(value)
        }
  }

  enum class Kind {
    NON_NUMERIC, DECIMAL, INTEGER
  }
}
