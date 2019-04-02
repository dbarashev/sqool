package com.bardsoftware.sqool.codegen

class MultiColumnTask(name: String,
                      robotQuery: String,
                      private val matcherSpec: MatcherSpec
) : ColumnTask(name, robotQuery) {
    override val resultType: String
        get() = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "TABLE(", ")")

    override fun generateStaticCode(): String {
        val robotQueryFunName = "${name}_Robot"
        val userQueryFunName = "${name}_User"
        val userQueryMock = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "SELECT ") { "NULL::${it.type}" }

        val mergedView = "${name}_Merged"
        val keyColNamesList = matcherSpec.relationSpec.keyCols.map { it.name }
        val maxAbsDiffChecks = matcherSpec.relationSpec.cols
                .filter { it.type.kind != SqlDataType.Kind.NON_NUMERIC }
                .joinToString("\n\n") {
                    val diffVar = if (it.type.kind == SqlDataType.Kind.INTEGER) "max_abs_int_diff" else "max_abs_decimal_diff"
                    generateMaxAbsDiffCheck(
                            maxAbsDiffVar = diffVar, colToCheck = it.name,
                            mergedView = mergedView, keyCols = keyColNamesList,
                            failedCheckMessage = matcherSpec.getDiffErrorMessage(it))
                }
        val matcherFunName = "${name}_Matcher"
        val keyColNamesString = matcherSpec.relationSpec.keyCols.joinToString(", ") { it.name }
        val matcherCode = """
            |DECLARE
            |   intxn_size INT;
            |   union_size INT;
            |   max_abs_int_diff BIGINT;
            |   max_abs_decimal_diff DOUBLE PRECISION;
            |BEGIN
            |
            |IF NOT EXISTS (
            |       SELECT SUM(query_id) FROM $mergedView
            |       GROUP BY ${matcherSpec.relationSpec.getAllColsList().joinToString(", ") { it.name }}
            |       HAVING SUM(query_id) <> 3
            |   ) THEN
            |   RETURN;
            |END IF;
            |
            |${generateUnionIntersectionCheck(
                unionSizeVar = "union_size", intxnSizeVar = "intxn_size",
                colNames = keyColNamesString, failedCheckMessage = matcherSpec.wrongKeyColsProjMessage)}
            |
            |RETURN NEXT '${matcherSpec.rightKeyColsProjMessage}';
            |
            |$maxAbsDiffChecks
            |
            |END;
            """.trimMargin()

        return """
            |${generateFunDef(
                funName = robotQueryFunName, returnType = resultType,
                body = robotQuery, language = Language.SQL)}
            |
            |${generateFunDef(
                funName = userQueryFunName, returnType = resultType,
                body = userQueryMock, language = Language.SQL)}
            |
            |${generateMergedViewCreation()}
            |
            |${generateFunDef(
                funName = matcherFunName, returnType = "SETOF TEXT",
                body = matcherCode, language = Language.PLPGSQL)}
            |
            |DROP FUNCTION $userQueryFunName() CASCADE;
            """.trimMargin()
    }

    private fun generateMaxAbsDiffCheck(maxAbsDiffVar: String,
                                        colToCheck: String,
                                        mergedView: String,
                                        keyCols: List<String>,
                                        failedCheckMessage: String
    ): String = """
        |SELECT MAX(ABS(diff)) INTO $maxAbsDiffVar FROM (
        |   SELECT SUM($colToCheck * CASE query_id WHEN 1 THEN 1 ELSE -1 END) AS diff
        |   FROM $mergedView
        |   GROUP BY ${keyCols.joinToString(", ")}
        |) AS T;
        |RETURN NEXT '$failedCheckMessage ' || $maxAbsDiffVar::TEXT;
        """.trimIndent()
}