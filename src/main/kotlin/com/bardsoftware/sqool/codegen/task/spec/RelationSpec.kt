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

data class RelationSpec(
    val keyCols: List<TaskResultColumn>,
    val nonKeyCols: List<TaskResultColumn> = emptyList()
) {
  init {
    if (keyCols.isEmpty()) {
      throw IllegalArgumentException("Key columns set can't be empty")
    }


    val colNames = keyCols.map { it.name } + nonKeyCols.map { it.name }
    if (colNames.size != colNames.toSet().size) {
      throw IllegalArgumentException("Column names must be unique")
    }
  }

  fun getAllColsList(): List<TaskResultColumn> = keyCols + nonKeyCols
}
