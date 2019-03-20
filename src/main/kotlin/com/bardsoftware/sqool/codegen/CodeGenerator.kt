package com.bardsoftware.sqool.codegen

data class TaskCheckCode(val staticCode: String, val perSubmissionCode: String)

fun generateMultipleColumnQueryRobot(taskName: String,
                                     matcherSpec: MatcherSpec,
                                     contestName: String,
                                     pathToSchema: String,
                                     robotQuery: String
): TaskCheckCode {
    val taskResultType = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "TABLE(", ")")
    val robotQueryFunName = "${taskName}_Robot"
    val userQueryFunName = "${taskName}_User"
    val userQueryMock = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "SELECT ") { "NULL::${it.type}" }

    val mergedView = "${taskName}_Merged"
    val keyColNamesList = matcherSpec.relationSpec.keyCols.map { it.name }
    val maxAbsDiffChecks = matcherSpec.relationSpec.cols
            .filter { it.type.kind != SqlDataType.Kind.NON_NUMERIC }
            .joinToString("\n\n") {
                val diffVar = if (it.type.kind == SqlDataType.Kind.INTEGER) "max_abs_int_diff" else "max_abs_rational_diff"
                generateMaxAbsDiffCheck(maxAbsDiffVar = diffVar, colToCheck = it.name, mergedView = mergedView,
                        keyCols = keyColNamesList, failedCheckMessage = matcherSpec.getDiffErrorMessage(it))
            }
    val matcherFunName = "${taskName}_Matcher"
    val keyColNamesString = matcherSpec.relationSpec.keyCols.joinToString(", ") { it.name }
    val matcherCode = """DECLARE
        |   intxn_size INT;
        |   union_size INT;
        |   max_abs_int_diff BIGINT;
        |   max_abs_rational_diff DOUBLE PRECISION;
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
        |${generateUnionIntersectionCheck(unionSizeVar = "union_size", intxnSizeVar = "intxn_size", colNames = keyColNamesString,
            mergedView = mergedView, failedCheckMessage = matcherSpec.wrongKeyColsProjMessage)}
        |
        |RETURN NEXT '${matcherSpec.rightKeyColsProjMessage}';
        |
        |$maxAbsDiffChecks
        |
        |END;
        """.trimMargin()

    val staticCode = """${generateStaticCodeHeader(contestName, pathToSchema)}
        |
        |${generateFunDef(funName = robotQueryFunName, returnType = taskResultType, body = robotQuery, language = Language.SQL)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = taskResultType, body = userQueryMock, language = Language.SQL)}
        |
        |${generateMergedViewCreation(taskName)}
        |
        |${generateFunDef(funName = matcherFunName, returnType = "SETOF TEXT", body = matcherCode, language = Language.PLPGSQL)}
        |
        |DROP FUNCTION $userQueryFunName() CASCADE;
        """.trimMargin()

    val perSubmissionCode = """${generatePerSubmissionCodeHeader(contestName)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = taskResultType, body = "{1}", language = Language.SQL)}
        |
        |${generateMergedViewCreation(taskName)}
        """.trimMargin()

    return TaskCheckCode(staticCode, perSubmissionCode)
}

fun generateSingleColumnQueryRobot(taskName: String,
                                   spec: TaskResultColumn,
                                   contestName: String,
                                   pathToSchema: String,
                                   robotQuery: String
): TaskCheckCode {
    val taskResultType = "TABLE($spec)"
    val robotQueryFunName = "${taskName}_Robot"
    val userQueryFunName = "${taskName}_User"
    val userQueryMock = "SELECT NULL::${spec.type}"

    val mergedView = "${taskName}_Merged"
    val matcherFunName = "${taskName}_Matcher"
    val matcherCode = """DECLARE
        |   intxn_size INT;
        |   union_size INT;
        |   robot_size INT;
        |   user_size INT;
        |
        |BEGIN
        |
        |${generateUnionIntersectionCheck("union_size", "intxn_size", spec.name, mergedView)}
        |
        |SELECT COUNT(*) INTO robot_size FROM $mergedView WHERE query_id = 0;
        |SELECT COUNT(*) INTO user_size FROM $mergedView WHERE query_id = 1;
        |
        |IF robot_size != user_size THEN
        |   RETURN NEXT 'Ваши результаты совпадают с результатами робота как множества, но отличаются размером';
        |   RETURN NEXT 'У вас в результате ' || user_size::TEXT || ' строк';
        |   RETURN NEXT 'У робота в результате ' || robot_size::TEXT || ' строк';
        |   RETURN;
        |end if;
        |
        |END;
        """.trimMargin()

    val staticCode = """${generateStaticCodeHeader(contestName, pathToSchema)}
        |
        |${generateFunDef(funName = robotQueryFunName, returnType = taskResultType, body = robotQuery, language = Language.SQL)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = taskResultType, body = userQueryMock, language = Language.SQL)}
        |
        |${generateMergedViewCreation(taskName)}
        |
        |${generateFunDef(funName = matcherFunName, returnType = "SETOF TEXT", body = matcherCode, language = Language.PLPGSQL)}
        |
        |DROP FUNCTION $userQueryFunName() CASCADE;
        """.trimMargin()

    val perSubmissionCode = """${generatePerSubmissionCodeHeader(contestName)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = taskResultType, body = "{1}", language = Language.SQL)}
        |
        |${generateMergedViewCreation(taskName)}
        """.trimMargin()

    return TaskCheckCode(staticCode, perSubmissionCode)
}

fun generateScalarValueQueryRobot(taskName: String,
                                  resultType: SqlDataType,
                                  contestName: String,
                                  pathToSchema: String,
                                  robotQuery: String
): TaskCheckCode {
    val robotQueryFunName = "${taskName}_Robot"
    val userQueryFunName = "${taskName}_User"
    val matcherFunName = "${taskName}_Matcher"
    val userQueryMock = "SELECT NULL::$resultType"

    val matcherCode = """DECLARE
        |   result_robot $resultType;
        |   result_user $resultType;
        |BEGIN
        |SELECT $robotQueryFunName() into result_robot;
        |SELECT $userQueryFunName() into result_user;
        |
        |IF (result_robot = result_user) THEN
        |   RETURN;
        |END IF;
        |
        |IF (result_robot < result_user) THEN
        |   RETURN NEXT 'Нет, у робота получилось меньше. Ваш результат: ' || result_user::TEXT;
        |   RETURN;
        |END IF;
        |
        |IF (result_robot > result_user) THEN
        |   RETURN NEXT 'Нет, у робота получилось больше. Ваш результат: ' || result_user::TEXT;
        |   RETURN;
        |END IF;
        |
        |END;
        """.trimMargin()
    val staticCode = """${generateStaticCodeHeader(contestName, pathToSchema)}
        |
        |${generateFunDef(funName = robotQueryFunName, returnType = resultType.toString(), body = robotQuery, language = Language.SQL)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = resultType.toString(), body = userQueryMock, language = Language.SQL)}
        |
        |${generateFunDef(funName = matcherFunName, returnType = "SETOF TEXT", body = matcherCode, language = Language.PLPGSQL)}
        |
        |DROP FUNCTION $userQueryFunName() CASCADE;
        """.trimMargin()
    val perSubmissionCode = """${generatePerSubmissionCodeHeader(contestName)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = resultType.toString(), body = "{1}", language = Language.SQL)}
        """.trimMargin()

    return TaskCheckCode(staticCode, perSubmissionCode)
}

private fun generateFunDef(funName: String, returnType: String, body: String, language: Language) =
        """CREATE OR REPLACE FUNCTION $funName()
        |RETURNS $returnType AS $$
        |$body
        |$$ LANGUAGE $language;
        """.trimMargin()

private fun generateStaticCodeHeader(contestName: String, pathToSchema: String) =
        """CREATE SCHEMA $contestName;
        |SET search_path=$contestName;
        |\i $pathToSchema;
        """.trimMargin()

private fun generatePerSubmissionCodeHeader(contestName: String) =
        """SELECT set_config(
        |   'search_path',
        |   '$contestName,' || current_setting('search_path'),
        |   false
        |);
        """.trimMargin()

private fun generateMergedViewCreation(taskName: String) =
        """CREATE OR REPLACE VIEW ${taskName}_Merged AS
        |   SELECT 0 AS query_id, * FROM ${taskName}_Robot()
        |   UNION ALL
        |   SELECT 1 AS query_id, * FROM ${taskName}_User();
        """.trimMargin()

private fun generateUnionIntersectionCheck(unionSizeVar: String,
                                           intxnSizeVar: String,
                                           colNames: String,
                                           mergedView: String,
                                           failedCheckMessage: String = "Ваши результаты отличаются от результатов робота"
): String = """
        |SELECT COUNT(1) INTO $intxnSizeVar FROM (
        |   SELECT $colNames FROM $mergedView WHERE query_id = 0
        |   INTERSECT
        |   SELECT $colNames FROM $mergedView WHERE query_id = 1
        |) AS T;
        |
        |SELECT COUNT(1) INTO $unionSizeVar FROM (
        |   SELECT $colNames FROM $mergedView WHERE query_id = 0
        |   UNION
        |   SELECT $colNames FROM $mergedView WHERE query_id = 1
        |) AS T;
        |
        |IF $intxnSizeVar != $unionSizeVar THEN
        |   RETURN NEXT '$failedCheckMessage';
        |   RETURN NEXT 'Размер пересечения результатов робота и ваших: ' || $intxnSizeVar::TEXT || ' строк';
        |   RETURN NEXT 'Размер объединения результатов робота и ваших: ' || $unionSizeVar::TEXT || ' строк';
        |   RETURN;
        |end if;
        """.trimIndent()

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

private enum class Language {
    SQL, PLPGSQL
}