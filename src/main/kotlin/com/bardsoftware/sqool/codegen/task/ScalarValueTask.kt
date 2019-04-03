package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.CodeGenerator
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType

class ScalarValueTask(name: String,
                      robotQuery: String,
                      private val resultTypeEnum: SqlDataType
) : Task(name, robotQuery) {
    override val resultType: String
        get() = resultTypeEnum.toString()

    override fun generateDynamicCode(codeGenerator: CodeGenerator): String = """
        |${codeGenerator.generateDynamicCodeHeader()}
        |
        |${generateFunDef(
            funName = userQueryFunName, returnType = resultType,
            body = "{1}", language = Language.SQL)}
        """.trimMargin()

    override fun generateStaticCode(): String {
        val matcherFunName = "${name}_Matcher"
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

        return """
            |${generateFunDef(
                funName = robotQueryFunName, returnType = resultType,
                body = robotQuery, language = Language.SQL)}
            |
            |${generateFunDef(
                funName = userQueryFunName, returnType = resultType,
                body = userQueryMock, language = Language.SQL)}
            |
            |${generateFunDef(
                funName = matcherFunName, returnType = "SETOF TEXT",
                body = matcherCode, language = Language.PLPGSQL)}
            |
            |DROP FUNCTION $userQueryFunName() CASCADE;
            """.trimMargin()
    }
}