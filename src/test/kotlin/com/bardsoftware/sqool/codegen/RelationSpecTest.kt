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

import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RelationSpecTest {
  @Test
  fun testConstructor() {
    RelationSpec(listOf(TaskResultColumn("id", SqlDataType.INT)))
  }

  @Test
  fun testDefaultNonKeyAttributes() {
    val relationSpec = RelationSpec(listOf(TaskResultColumn("id", SqlDataType.INT)))
    assertTrue(relationSpec.nonKeyCols.isEmpty())
  }

  @Test
  fun testNoKeyAttributesException() {
    val exception = assertThrows(IllegalArgumentException::class.java) {
      RelationSpec(emptyList(), listOf(TaskResultColumn("id", SqlDataType.INT)))
    }
    assertEquals("Key columns set can't be empty", exception.message)
  }

  @Test
  fun testDuplicateKeyAttributeException() {
    val exception = assertThrows(IllegalArgumentException::class.java) {
      RelationSpec(listOf(TaskResultColumn("id", SqlDataType.INT), TaskResultColumn("id", SqlDataType.BIGINT)))
    }
    assertEquals("Column names must be unique", exception.message)
  }

  @Test
  fun testDuplicateNonKeyAttributeException() {
    val exception = assertThrows(IllegalArgumentException::class.java) {
      val keyAttribute = listOf(TaskResultColumn("key", SqlDataType.TEXT))
      val nonKeyAttributes = listOf(TaskResultColumn("id", SqlDataType.INT), TaskResultColumn("id", SqlDataType.BIGINT))
      RelationSpec(keyAttribute, nonKeyAttributes)
    }
    assertEquals("Column names must be unique", exception.message)
  }

  @Test
  fun testDuplicateKeyAndNonKeyAttributeException() {
    val exception = assertThrows(IllegalArgumentException::class.java) {
      val keyAttribute = listOf(TaskResultColumn("id", SqlDataType.TEXT))
      val nonKeyAttributes = listOf(TaskResultColumn("id", SqlDataType.INT))
      RelationSpec(keyAttribute, nonKeyAttributes)
    }
    assertEquals("Column names must be unique", exception.message)
  }

  @Test
  fun testResultColumnOrdering() {
    RelationSpec(listOf(
        TaskResultColumn("id", SqlDataType.INT, 2),
        TaskResultColumn("value", SqlDataType.TEXT, 1)
    ))
  }
}

