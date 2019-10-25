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

abstract class Task(val name: String, val solution: String) {
  protected val robotQueryFunName = "${name}_Robot"
  protected val userQueryFunName = "${name}_User"
  abstract val resultType: String
  abstract val mockSolution: String
  abstract val mockSolutionError: Regex

  abstract fun generateStaticCode(): String

  abstract fun generateDynamicCode(variant: String): String

  protected fun generateDynamicCodeHeader(variant: String) = """
      |SELECT set_config(
      |   ''search_path'',
      |   ''$variant,${getExtensionSchemaName()},'' || current_setting(''search_path''),
      |   false
      |);
      """.trimMargin()

  protected fun generateFunDef(funName: String, returnType: String, body: String, language: Language) = """
      |CREATE OR REPLACE FUNCTION $funName()
      |RETURNS $returnType AS $$
      |$body
      |$$ LANGUAGE $language;
      """.trimMargin()

  override fun equals(other: Any?) =
      other is Task && other.name == name && other.solution == solution

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + solution.hashCode()
    return result
  }

  protected enum class Language {
    SQL, PLPGSQL
  }
}
