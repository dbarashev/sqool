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

package com.bardsoftware.sqool.contest.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskEditHandlerTest {
  @Test
  fun `test valid result spec of scalar value task`() {
    assertEquals("""
      |[{ "col_num": 0,
      |  "col_name": "",
      |  "col_type": "INT"
      |}]
    """.trimMargin(), buildResultJson("INT"))
  }

  @Test
  fun `test valid result spec of single column task`() {
    assertEquals("""
      |[{ "col_num": 1,
      |  "col_name": "foo",
      |  "col_type": "INT"
      |}]
    """.trimMargin(), buildResultJson("foo INT"))
  }

  @Test
  fun `test valid result spec of multi column task`() {
    assertEquals("""
      |[{ "col_num": 1,
      |  "col_name": "foo",
      |  "col_type": "INT"
      |}, { "col_num": 2,
      |  "col_name": "bar",
      |  "col_type": "TEXT"
      |}]
    """.trimMargin(), buildResultJson("foo INT, bar TEXT"))
  }

  @Test
  fun `test invalid result spec`() {
    assertThrows(TaskValidationException::class.java) {
      buildResultJson("INTT")
    }
    assertThrows(TaskValidationException::class.java) {
      buildResultJson("foo INT, bar TXT")
    }
    assertThrows(TaskValidationException::class.java) {
      buildResultJson("foo INT bar TEXT")
    }
    assertThrows(TaskValidationException::class.java) {
      buildResultJson("foo INT; bar TEXT")
    }
    assertThrows(TaskValidationException::class.java) {
      buildResultJson("INT foo, bar TEXT")
    }
  }

}
