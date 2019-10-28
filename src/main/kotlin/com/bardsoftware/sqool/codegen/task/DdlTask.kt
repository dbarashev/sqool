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

/**
 * @author dbarashev@bardsoftware.com
 */
class DdlTask(name: String, solution: String) : Task(name, solution) {
  override val resultType: String
    get() = "VOID"

  override val mockSolution: String
    get() = """
      |BEGIN
      |CREATE TABLE Foo(id INT PRIMARY KEY, value TEXT);
      |END;""".trimMargin()

  override val mockSolutionError: Regex
    get() = ".*".toRegex()

  override fun generateStaticCode(): String {
    val matcherFunName = "${name}_Matcher"
    val matcherCode = """
        | BEGIN
        | PERFORM $userQueryFunName();
        | RETURN;
        | EXCEPTION
        |   WHEN OTHERS THEN
        |     RETURN NEXT SQLERRM;
        |   RETURN;
        | END;
        """.trimMargin()
    return """
            |${generateFunDef(
        funName = userQueryFunName, returnType = resultType,
        body = mockSolution, language = Language.PLPGSQL
    )}
            |
            |${generateFunDef(
        funName = matcherFunName, returnType = "SETOF TEXT",
        body = matcherCode, language = Language.PLPGSQL
    )}
            |
            |DROP FUNCTION $userQueryFunName() CASCADE;
            """.trimMargin()
  }

  override fun generateDynamicCode(variant: String): String {
    val userFxn = generateFunDef(
        funName = userQueryFunName,
        returnType = resultType,
        body = """
          BEGIN
          {1}
          END;""".trimIndent(),
        language = Language.PLPGSQL
    )
    return """
        |${generateDynamicCodeHeader(variant)}
        |
        |$userFxn
        """.trimMargin()
  }

}
