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

data class MatcherSpec(
    val relationSpec: RelationSpec,
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
    if (!relationSpec.nonKeyCols.contains(column)) {
      throw IllegalArgumentException("No such non-key attribute in the relation")
    }
  }
}

