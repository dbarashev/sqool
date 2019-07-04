package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.CodeGenerator

abstract class ColumnTask(name: String, robotQuery: String) : Task(name, robotQuery) {
    protected val mergedView = "${name}_Merged"
    protected val unionSizeVar = "union_size"
    protected val intxnSizeVar = "intxn_size"

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
        |CREATE OR REPLACE VIEW $mergedView AS
        |   SELECT 1 AS query_id, * FROM ${name}_Robot()
        |   UNION ALL
        |   SELECT 2 AS query_id, * FROM ${name}_User();
        """.trimMargin()

    protected fun generateUnionIntersectionCheck(colNames: String,
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