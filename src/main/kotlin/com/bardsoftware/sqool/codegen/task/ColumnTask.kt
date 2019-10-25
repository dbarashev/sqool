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

abstract class ColumnTask(name: String, robotQuery: String) : Task(name, robotQuery) {
  protected val mergedView = "${name}_Merged"
  protected val unionSizeVar = "union_size"
  protected val intxnSizeVar = "intxn_size"

  override fun generateDynamicCode(variant: String): String = """
        |${generateDynamicCodeHeader(variant)}
        |
        |${generateFunDef(
            funName = "${name}_User", returnType = resultType,
            body = "{1}", language = Language.SQL
        )}
        |
        |${generateMergedViewCreation()}
        """.trimMargin()

  protected fun generateMergedViewCreation() = """
        |CREATE OR REPLACE VIEW $mergedView AS
        |   SELECT 1 AS query_id, * FROM ${name}_Robot()
        |   UNION ALL
        |   SELECT 2 AS query_id, * FROM ${name}_User();
        """.trimMargin()

  protected fun generateUnionIntersectionCheck(
      colNames: String,
      failedCheckMessage: String = "Ваши результаты отличаются от результатов робота"
  ): String = """
        |SELECT COUNT(1) INTO $intxnSizeVar FROM (
        |   SELECT $colNames FROM $mergedView WHERE query_id = 1
        |   INTERSECT
        |   SELECT $colNames FROM $mergedView WHERE query_id = 2
        |) AS T;
        |
        |SELECT COUNT(1) INTO $unionSizeVar FROM (
        |   SELECT $colNames FROM $mergedView WHERE query_id = 1
        |   UNION
        |   SELECT $colNames FROM $mergedView WHERE query_id = 2
        |) AS T;
        |
        |IF $intxnSizeVar != $unionSizeVar THEN
        |   RETURN NEXT '$failedCheckMessage';
        |   RETURN NEXT 'Размер пересечения результатов робота и ваших: ' || $intxnSizeVar::TEXT || ' строк';
        |   RETURN NEXT 'Размер объединения результатов робота и ваших: ' || $unionSizeVar::TEXT || ' строк';
        |   RETURN;
        |end if;
        """.trimIndent()
}
