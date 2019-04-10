package com.bardsoftware.sqool.contest.admin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskNewHandlerTest {
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
