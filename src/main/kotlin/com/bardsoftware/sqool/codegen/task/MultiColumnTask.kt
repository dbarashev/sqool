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

import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import java.util.regex.Pattern

class MultiColumnTask(name: String, robotQuery: String, private val matcherSpec: MatcherSpec) : ColumnTask(name, robotQuery) {
  override val resultType: String
    get() = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "TABLE(", ")")
  override val mockSolution: String
    get() = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "SELECT ") { "NULL::${it.type}" }
  override val mockSolutionError: Regex
    get() = """
            |${Pattern.quote(matcherSpec.wrongKeyColsProjMessage)}
            |Размер пересечения результатов робота и ваших: \d+ строк
            |Размер объединения результатов робота и ваших: \d+ строк
            """.trimMargin().toRegex()


  override fun generateStaticCode(): String {
    val keyColNamesList = matcherSpec.relationSpec.keyCols.map { it.name }
    val maxAbsDiffChecks = matcherSpec.relationSpec.nonKeyCols
        .filter { it.type.kind != SqlDataType.Kind.NON_NUMERIC }
        .joinToString("\n\n") {
          val diffVar = if (it.type.kind == SqlDataType.Kind.INTEGER) "max_abs_int_diff" else "max_abs_decimal_diff"
          generateMaxAbsDiffCheck(
              maxAbsDiffVar = diffVar, colToCheck = it.name,
              keyCols = keyColNamesList, failedCheckMessage = matcherSpec.getDiffErrorMessage(it)
          )
        }
    val matcherFunName = "${name}_Matcher"
    val keyColNamesString = matcherSpec.relationSpec.keyCols.joinToString(", ") { it.name }
    val matcherCode = """
            |DECLARE
            |   $intxnSizeVar INT;
            |   $unionSizeVar INT;
            |   max_abs_int_diff BIGINT;
            |   max_abs_decimal_diff DOUBLE PRECISION;
            |BEGIN
            |
            |IF NOT EXISTS (
            |       SELECT SUM(query_id) FROM $mergedView
            |       GROUP BY ${matcherSpec.relationSpec.getAllColsList().joinToString(", ") { it.name }}
            |       HAVING SUM(query_id) <> 3
            |   ) THEN
            |   RETURN;
            |END IF;
            |
            |${generateUnionIntersectionCheck(
                colNames = keyColNamesString, failedCheckMessage = matcherSpec.wrongKeyColsProjMessage
            )}
            |
            |RETURN NEXT '${matcherSpec.rightKeyColsProjMessage}';
            |
            |$maxAbsDiffChecks
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

  private fun generateMaxAbsDiffCheck(
      maxAbsDiffVar: String, colToCheck: String,
      keyCols: List<String>, failedCheckMessage: String
  ): String = """
        |SELECT MAX(ABS(diff)) INTO $maxAbsDiffVar FROM (
        |   SELECT SUM($colToCheck * CASE query_id WHEN 1 THEN 1 ELSE -1 END) AS diff
        |   FROM $mergedView
        |   GROUP BY ${keyCols.joinToString(", ")}
        |) AS T;
        |RETURN NEXT '$failedCheckMessage ' || $maxAbsDiffVar::TEXT;
        """.trimIndent()

  override fun equals(other: Any?) =
      other is MultiColumnTask && other.matcherSpec == matcherSpec && super.equals(other)

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + matcherSpec.hashCode()
    return result
  }
}
