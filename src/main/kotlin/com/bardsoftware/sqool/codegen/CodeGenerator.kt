package com.bardsoftware.sqool.codegen

class CodeGenerator(private val contestName: String, private val pathToSchema: String) {
    fun generateStaticCodeHeader() = """
        |CREATE SCHEMA $contestName;
        |SET search_path=$contestName;
        |\i $pathToSchema;
        """.trimMargin()

    //TODO: check if this header is really needed
    /*fun generateDynamicCodeHeader() = """
        |SELECT set_config(
        |   'search_path',
        |   '$contestName,' || current_setting('search_path'),
        |   false
        |);
        """.trimMargin()*/
}

