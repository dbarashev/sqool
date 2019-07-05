package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.Task

class Variant(val name: String, private val pathToSchema: String, val tasks: List<Task>) {
    fun generateStaticCode() = """
        |DROP SCHEMA IF EXISTS $name CASCADE;
        |CREATE SCHEMA $name;
        |SET search_path=$name;
        |\i $pathToSchema;
        |
        |${tasks.joinToString("\n\n") { it.generateStaticCode() }}
        """.trimMargin()
}