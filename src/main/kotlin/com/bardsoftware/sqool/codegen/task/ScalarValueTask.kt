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

import com.bardsoftware.sqool.codegen.task.spec.SqlDataType

class ScalarValueTask(name: String, robotQuery: String, private val resultTypeEnum: SqlDataType) : Task(name, robotQuery) {
  override val resultType: String
    get() = resultTypeEnum.toString()
  override val mockSolution: String
    get() = "SELECT NULL::$resultType"
  override val mockSolutionError: Regex
    get() = "Нет, ваш результат NULL".toRegex()

  override fun generateDynamicCode(variant: String): String = """
        |${generateDynamicCodeHeader(variant)}
        |
        |${generateFunDef(
            funName = userQueryFunName, returnType = resultType,
            body = "{1}", language = Language.SQL
        )}
        """.trimMargin()

  override fun generateStaticCode(): String {
    val matcherFunName = "${name}_Matcher"
    val matcherCode = """DECLARE
        |   result_robot $resultType;
        |   result_user $resultType;
        |BEGIN
        |SELECT $robotQueryFunName() into result_robot;
        |SELECT $userQueryFunName() into result_user;
        |
        |IF (result_user IS NULL) THEN
        |   RETURN NEXT 'Нет, ваш результат NULL';
        |   RETURN;
        |END IF;
        |
        |IF (result_robot = result_user) THEN
        |   RETURN;
        |END IF;
        |
        |IF (result_robot < result_user) THEN
        |   RETURN NEXT 'Нет, у робота получилось меньше. Ваш результат: ' || result_user::TEXT;
        |   RETURN;
        |END IF;
        |
        |IF (result_robot > result_user) THEN
        |   RETURN NEXT 'Нет, у робота получилось больше. Ваш результат: ' || result_user::TEXT;
        |   RETURN;
        |END IF;
        |
        |END;
        """.trimMargin()

    return """
            |${generateFunDef(
                funName = robotQueryFunName, returnType = resultType,
                body = solution, language = Language.SQL
            )}
            |
            |${generateFunDef(
                funName = userQueryFunName, returnType = resultType,
                body = mockSolution, language = Language.SQL
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

  override fun equals(other: Any?) =
      other is ScalarValueTask && other.resultTypeEnum == resultTypeEnum && super.equals(other)

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + resultTypeEnum.hashCode()
    return result
  }
}
