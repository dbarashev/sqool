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

package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.getExtensionSchemaName

/**
 * @author dbarashev@bardsoftware.com
 */
class DdlTask(name: String, solution: String) : Task(name, solution) {
  override val resultType: String
    get() = "VOID"

  override val mockSolution: String
    get() = """
      |CREATE TABLE Foo(id INT PRIMARY KEY, value TEXT)
      """.trimMargin()

  override val mockSolutionError: Regex
    get() = ".*".toRegex()

  override fun generateStaticCode(): String {
    val matcherFunName = "${name}_Matcher"
    val matcherCode = """
        | BEGIN
        | END;
        """.trimMargin()
    return """
            |${generateFunDef(
        funName = matcherFunName, returnType = "SETOF TEXT",
        body = matcherCode, language = Language.PLPGSQL
    )}
            """.trimMargin()
  }

  override fun generateDynamicCodeHeader(variant: String) = """
      |SELECT set_config(
      |   ''search_path'',
      |   current_setting(''search_path'') || '',$variant,${getExtensionSchemaName()}'',
      |   false
      |);
      """.trimMargin()

  override fun generateDynamicCode(variant: String): String {
    return """
        |${generateDynamicCodeHeader(variant)}
        |
        |{1}
        |;
        """.trimMargin()
  }

}
