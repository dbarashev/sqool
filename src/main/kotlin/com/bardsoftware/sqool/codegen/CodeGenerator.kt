package com.bardsoftware.sqool.codegen

data class TaskCheckCode(val staticCode: String, val perSubmissionCode: String)

fun generateSingleColumnQueryRobot(taskName: String,
                                   spec: TaskResultColumn,
                                   contestName: String,
                                   pathToSchema: String,
                                   robotQuery: String
): TaskCheckCode {
    val taskResultType = "TABLE(${spec.name} ${spec.type})"
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
        |SELECT COUNT(1) INTO intxn_size FROM (
        |   SELECT ${spec.name} FROM $mergedView WHERE query_id = 0
        |   INTERSECT
        |   SELECT ${spec.name} FROM $mergedView WHERE query_id = 1
        |) AS T;
        |
        |SELECT COUNT(1) INTO union_size FROM (
        |   SELECT ${spec.name} FROM $mergedView WHERE query_id = 0
        |   UNION
        |   SELECT ${spec.name} FROM $mergedView WHERE query_id = 1
        |) AS T;
        |
        |IF intxn_size != union_size THEN
        |   RETURN NEXT 'Ваши результаты отличаются от результатов робота';
        |   RETURN NEXT 'Размер пересечения результатов робота и ваших: ' || intxn_size::TEXT || ' строк';
        |   RETURN NEXT 'Размер объединения результатов робота и ваших: ' || union_size::TEXT || ' строк';
        |   RETURN;
        |end if;
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

    val viewCreation = """CREATE OR REPLACE VIEW $mergedView AS
        |   SELECT 0 AS query_id, * FROM $robotQueryFunName()
        |   UNION ALL
        |   SELECT 1 AS query_id, * FROM $userQueryFunName();
        """.trimMargin()

    val staticCode = """${generateStaticCodeHeader(contestName, pathToSchema)}
        |
        |${generateFunDef(funName = robotQueryFunName, returnType = taskResultType, body = robotQuery, language = Language.SQL)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = taskResultType, body = userQueryMock, language = Language.SQL)}
        |
        |$viewCreation
        |
        |${generateFunDef(funName = matcherFunName, returnType = "SETOF TEXT", body = matcherCode, language = Language.PLPGSQL)}
        |
        |DROP FUNCTION $userQueryFunName() CASCADE;
        """.trimMargin()

    val perSubmissionCode = """${generatePerSubmissionCodeHeader(contestName)}
        |
        |${generateFunDef(funName = userQueryFunName, returnType = taskResultType, body = "{1}", language = Language.SQL)}
        |
        |$viewCreation
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
        |IF (result_robot < result_user) THEN
        |   RETURN NEXT 'Нет, у робота получилось меньше. Ваш результат: ' || result_user::TEXT;
        |   RETURN;
        |END IF;
        |
        |IF (result_robot > result_user) THEN
        |   RETURN NEXT 'Нет, у робота получилось больше. Ваш результат: ' || result_user::TEXT;
        |    RETURN;
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

private enum class Language {
    SQL, PLPGSQL
}