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

import com.bardsoftware.sqool.codegen.task.Task

class Contest(val code: String, val name: String, val variants: List<Variant>)

class Variant(val name: String, val tasks: List<Task>, val schemas: List<Schema>) {
  fun generateStaticCode(schemasDir: String) = """
      |DROP SCHEMA IF EXISTS $name CASCADE;
      |CREATE SCHEMA $name;
      |SET search_path=$name,${getExtensionSchemaName()};
      |${schemas.joinToString(separator = "\n") { "\\i '$schemasDir/${it.description}.sql';" }}
      |
      |${tasks.joinToString("\n\n") { it.generateStaticCode() }}
      """.trimMargin()
}

// This function returns the name of the schema where required extensions, such as HSTORE,
// are registered.
fun getExtensionSchemaName() = "ext"
