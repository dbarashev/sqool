package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.Task


class Variant(val name: String, val tasks: List<Task>, val schemas: List<Schema>) {
    fun generateStaticCode(schemasDir: String) = """
        |DROP SCHEMA IF EXISTS $name CASCADE;
        |CREATE SCHEMA $name;
        |SET search_path=$name;
        |${schemas.joinToString(separator = "\n") { "\\i '$schemasDir/${it.getDescription()}.sql';" }}
        |
        |${tasks.joinToString("\n\n") { it.generateStaticCode() }}
        """.trimMargin()
}