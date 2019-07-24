package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.Task

class Contest(val code: String, val name: String, val variants: List<Variant>)

class Variant(val name: String, val tasks: List<Task>, val schemas: List<Schema>) {
    fun generateStaticCode(schemasDir: String) = """
        |DROP SCHEMA IF EXISTS $name CASCADE;
        |CREATE SCHEMA $name;
        |SET search_path=$name;
        |${schemas.joinToString(separator = "\n") { "\\i '$schemasDir/${it.description}.sql';" }}
        |
        |${tasks.joinToString("\n\n") { it.generateStaticCode() }}
        """.trimMargin()
}