package com.bardsoftware.sqool.codegen

abstract class Task(val name: String, val robotQuery: String) {
    protected val robotQueryFunName = "${name}_Robot"
    protected val userQueryFunName = "${name}_User"
    abstract val resultType: String

    abstract fun generateStaticCode(): String

    abstract fun generateDynamicCode(codeGenerator: CodeGenerator): String

    protected fun generateFunDef(funName: String, returnType: String, body: String, language: Language) = """
        |CREATE OR REPLACE FUNCTION $funName()
        |RETURNS $returnType AS $$
        |$body
        |$$ LANGUAGE $language;
        """.trimMargin()

    protected enum class Language {
        SQL, PLPGSQL
    }
}