/*
 * Copyright (c) BarD Software s.r.o 2019
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bardsoftware.sqool.codegen.task.spec

data class TaskResultColumn(val name: String, val type: SqlDataType, val num: Int = 0) {
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
