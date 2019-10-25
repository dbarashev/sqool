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

package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.contest.admin.Scripts
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Schema(private val id: Int) {
  private data class Data(val description: String, val body: String)

  private val data by lazy {
    transaction {
      val script = Scripts.select { Scripts.id eq id }.first()
      Data(script[Scripts.description], script[Scripts.body])
    }
  }
  val description
    get() = data.description
  val body
    get() = data.body

  override fun equals(other: Any?) = other is Schema && other.id == this.id

  override fun hashCode() = id
}
