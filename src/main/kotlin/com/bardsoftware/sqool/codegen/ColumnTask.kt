package com.bardsoftware.sqool.codegen

abstract class ColumnTask(name: String, robotQuery: String) : Task(name, robotQuery) {
    override fun generateDynamicCode(codeGenerator: CodeGenerator): String = """
        |${codeGenerator.generateDynamicCodeHeader()}
        |
        |${generateFunDef(
            funName = "${name}_User", returnType = resultType,
            body = "{1}", language = Language.SQL)}
        |
        |${generateMergedViewCreation()}
        """.trimMargin()

    protected fun generateMergedViewCreation() = """
        |CREATE OR REPLACE VIEW ${name}_Merged AS
        |   SELECT 0 AS query_id, * FROM ${name}_Robot()
        |   UNION ALL
        |   SELECT 1 AS query_id, * FROM ${name}_User();
        """.trimMargin()

    protected fun generateUnionIntersectionCheck(unionSizeVar: String,
                                                 intxnSizeVar: String,
                                                 colNames: String,
                                                 failedCheckMessage: String = "Ваши результаты отличаются от результатов робота"
    ): String = """
        |SELECT COUNT(1) INTO $intxnSizeVar FROM (
        |   SELECT $colNames FROM ${name}_Merged WHERE query_id = 0
        |   INTERSECT
        |   SELECT $colNames FROM ${name}_Merged WHERE query_id = 1
        |) AS T;
        |
        |SELECT COUNT(1) INTO $unionSizeVar FROM (
        |   SELECT $colNames FROM ${name}_Merged WHERE query_id = 0
        |   UNION
        |   SELECT $colNames FROM ${name}_Merged WHERE query_id = 1
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