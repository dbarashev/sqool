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

import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn

class SingleColumnTask(name: String, robotQuery: String, private val spec: TaskResultColumn) : ColumnTask(name, robotQuery) {
  override val resultType: String
    get() = "TABLE($spec)"
  override val mockSolution: String
    get() = "SELECT NULL::${spec.type}"
  override val mockSolutionError: Regex
    get() = """Ваши результаты отличаются от результатов робота
        |Размер пересечения результатов робота и ваших: \d+ строк
        |Размер объединения результатов робота и ваших: \d+ строк
        """.trimMargin().toRegex()

  override fun generateStaticCode(): String {
    val matcherFunName = "${name}_Matcher"
    val matcherCode = """DECLARE
        |   $intxnSizeVar INT;
        |   $unionSizeVar INT;
        |   robot_size INT;
        |   user_size INT;
        |
        |BEGIN
        |
        |${generateUnionIntersectionCheck(spec.name)}
        |
        |SELECT COUNT(*) INTO robot_size FROM $mergedView WHERE query_id = 1;
        |SELECT COUNT(*) INTO user_size FROM $mergedView WHERE query_id = 2;
        |
        |IF robot_size != user_size THEN
        |   RETURN NEXT 'Ваши результаты совпадают с результатами робота как множества, но отличаются размером';
        |   RETURN NEXT 'У вас в результате ' || user_size::TEXT || ' строк';
        |   RETURN NEXT 'У робота в результате ' || robot_size::TEXT || ' строк';
        |   RETURN;
        |end if;
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
           |${generateMergedViewCreation()}
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
      other is SingleColumnTask && other.spec == spec && super.equals(other)

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + spec.hashCode()
    return result
  }
}
