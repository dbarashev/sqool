package com.bardsoftware.sqool.codegen.task

import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn

class SingleColumnTask(name: String, robotQuery: String,
                       private val spec: TaskResultColumn
) : ColumnTask(name, robotQuery) {
    override val resultType: String
        get() = "TABLE($spec)"

    override fun generateStaticCode(): String {
        val userQueryMock = "SELECT NULL::${spec.type}"
        val matcherFunName = "${name}_Matcher"
        val matcherCode = """DECLARE
            |   $intxnSizeVar INT;
            |   $unionSizeVar INT;
            |   robot_size INT;
            |   user_size INT;
            |
            |BEGIN
            |
            |${generateUnionIntersectionCheck(spec.name)}
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

    override fun equals(other: Any?) =
            other is SingleColumnTask && other.spec == spec && super.equals(other)
}