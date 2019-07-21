package com.bardsoftware.sqool.codegen.task

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
        |   ''$variant,'' || current_setting(''search_path''),
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