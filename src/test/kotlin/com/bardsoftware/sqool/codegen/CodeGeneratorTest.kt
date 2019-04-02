package com.bardsoftware.sqool.codegen

import com.bardsoftware.sqool.codegen.task.*
import com.bardsoftware.sqool.codegen.task.spec.MatcherSpec
import com.bardsoftware.sqool.codegen.task.spec.RelationSpec
import com.bardsoftware.sqool.codegen.task.spec.SqlDataType
import com.bardsoftware.sqool.codegen.task.spec.TaskResultColumn
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class CodeGeneratorTest {
    @Test
    fun testStaticCodeHeader() {
        val generator = CodeGenerator("cw1", "/hse/cw1/schema.sql")
        val expectedHeader = """
            |CREATE SCHEMA cw1;
            |SET search_path=cw1;
            |\i /hse/cw1/schema.sql;
            """.trimMargin()
        assertEquals(expectedHeader, generator.generateStaticCodeHeader())
    }

    @Test
    fun testSingleColumnQueryRobotCode() {
        val expectedStaticCode = """
            |CREATE OR REPLACE FUNCTION Task3_Robot()
            |RETURNS TABLE(id INT) AS $$
            |SELECT 11;
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE FUNCTION Task3_User()
            |RETURNS TABLE(id INT) AS $$
            |SELECT NULL::INT
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE VIEW Task3_Merged AS
            |   SELECT 0 AS query_id, * FROM Task3_Robot()
            |   UNION ALL
            |   SELECT 1 AS query_id, * FROM Task3_User();
            |
            |CREATE OR REPLACE FUNCTION Task3_Matcher()
            |RETURNS SETOF TEXT AS $$
            |DECLARE
            |   intxn_size INT;
            |   union_size INT;
            |   robot_size INT;
            |   user_size INT;
            |
            |BEGIN
            |
            |SELECT COUNT(1) INTO intxn_size FROM (
            |   SELECT id FROM Task3_Merged WHERE query_id = 0
            |   INTERSECT
            |   SELECT id FROM Task3_Merged WHERE query_id = 1
            |) AS T;
            |
            |SELECT COUNT(1) INTO union_size FROM (
            |   SELECT id FROM Task3_Merged WHERE query_id = 0
            |   UNION
            |   SELECT id FROM Task3_Merged WHERE query_id = 1
            |) AS T;
            |
            |IF intxn_size != union_size THEN
            |   RETURN NEXT 'Ваши результаты отличаются от результатов робота';
            |   RETURN NEXT 'Размер пересечения результатов робота и ваших: ' || intxn_size::TEXT || ' строк';
            |   RETURN NEXT 'Размер объединения результатов робота и ваших: ' || union_size::TEXT || ' строк';
            |   RETURN;
            |end if;
            |
            |SELECT COUNT(*) INTO robot_size FROM Task3_Merged WHERE query_id = 0;
            |SELECT COUNT(*) INTO user_size FROM Task3_Merged WHERE query_id = 1;
            |
            |IF robot_size != user_size THEN
            |   RETURN NEXT 'Ваши результаты совпадают с результатами робота как множества, но отличаются размером';
            |   RETURN NEXT 'У вас в результате ' || user_size::TEXT || ' строк';
            |   RETURN NEXT 'У робота в результате ' || robot_size::TEXT || ' строк';
            |   RETURN;
            |end if;
            |
            |END;
            |$$ LANGUAGE PLPGSQL;
            |
            |DROP FUNCTION Task3_User() CASCADE;
            """.trimMargin()
        val expectedPerSubmissionCode = """
            |SELECT set_config(
            |   'search_path',
            |   'cw1,' || current_setting('search_path'),
            |   false
            |);
            |
            |CREATE OR REPLACE FUNCTION Task3_User()
            |RETURNS TABLE(id INT) AS $$
            |{1}
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE VIEW Task3_Merged AS
            |   SELECT 0 AS query_id, * FROM Task3_Robot()
            |   UNION ALL
            |   SELECT 1 AS query_id, * FROM Task3_User();
            """.trimMargin()

        val generator = CodeGenerator("cw1", "/hse/cw1/schema.sql")
        val spec = TaskResultColumn("id", SqlDataType.INT)
        val task = SingleColumnTask("Task3", "SELECT 11;", spec)
        assertEquals(expectedStaticCode, task.generateStaticCode())
        assertEquals(expectedPerSubmissionCode, task.generateDynamicCode(generator))
    }

    @Test
    fun testScalarValueQueryRobotCode() {
        val expectedStaticCode = """
            |CREATE OR REPLACE FUNCTION Task12_Robot()
            |RETURNS TEXT AS $$
            |SELECT 'Some text';
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE FUNCTION Task12_User()
            |RETURNS TEXT AS $$
            |SELECT NULL::TEXT
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE FUNCTION Task12_Matcher()
            |RETURNS SETOF TEXT AS $$
            |DECLARE
            |   result_robot TEXT;
            |   result_user TEXT;
            |BEGIN
            |SELECT Task12_Robot() into result_robot;
            |SELECT Task12_User() into result_user;
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
            |$$ LANGUAGE PLPGSQL;
            |
            |DROP FUNCTION Task12_User() CASCADE;
            """.trimMargin()
        val expectedPerSubmissionCode = """
            |SELECT set_config(
            |   'search_path',
            |   'cw2,' || current_setting('search_path'),
            |   false
            |);
            |
            |CREATE OR REPLACE FUNCTION Task12_User()
            |RETURNS TEXT AS $$
            |{1}
            |$$ LANGUAGE SQL;
            """.trimMargin()

        val generator = CodeGenerator("cw2", "/hse/cw2/schema.sql")
        val task = ScalarValueTask("Task12", "SELECT 'Some text';", SqlDataType.TEXT)
        assertEquals(expectedStaticCode, task.generateStaticCode())
        assertEquals(expectedPerSubmissionCode, task.generateDynamicCode(generator))
    }

    @Test
    fun testMultipleColumnQueryRobotCode() {
        val expectedStaticCode = """
            |CREATE OR REPLACE FUNCTION Task05_Robot()
            |RETURNS TABLE(ship TEXT, port INT, transfers_num INT, transfer_size DOUBLE PRECISION, product TEXT) AS $$
            |SELECT 'ship', 1, 10, 500::DOUBLE PRECISION, 'prod'
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE FUNCTION Task05_User()
            |RETURNS TABLE(ship TEXT, port INT, transfers_num INT, transfer_size DOUBLE PRECISION, product TEXT) AS $$
            |SELECT NULL::TEXT, NULL::INT, NULL::INT, NULL::DOUBLE PRECISION, NULL::TEXT
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE VIEW Task05_Merged AS
            |   SELECT 0 AS query_id, * FROM Task05_Robot()
            |   UNION ALL
            |   SELECT 1 AS query_id, * FROM Task05_User();
            |
            |CREATE OR REPLACE FUNCTION Task05_Matcher()
            |RETURNS SETOF TEXT AS $$
            |DECLARE
            |   intxn_size INT;
            |   union_size INT;
            |   max_abs_int_diff BIGINT;
            |   max_abs_decimal_diff DOUBLE PRECISION;
            |BEGIN
            |
            |IF NOT EXISTS (
            |       SELECT SUM(query_id) FROM Task05_Merged
            |       GROUP BY ship, port, transfers_num, transfer_size, product
            |       HAVING SUM(query_id) <> 3
            |   ) THEN
            |   RETURN;
            |END IF;
            |
            |SELECT COUNT(1) INTO intxn_size FROM (
            |   SELECT ship, port FROM Task05_Merged WHERE query_id = 0
            |   INTERSECT
            |   SELECT ship, port FROM Task05_Merged WHERE query_id = 1
            |) AS T;
            |
            |SELECT COUNT(1) INTO union_size FROM (
            |   SELECT ship, port FROM Task05_Merged WHERE query_id = 0
            |   UNION
            |   SELECT ship, port FROM Task05_Merged WHERE query_id = 1
            |) AS T;
            |
            |IF intxn_size != union_size THEN
            |   RETURN NEXT 'Множество пар (корабль, порт) отличается от результатов робота';
            |   RETURN NEXT 'Размер пересечения результатов робота и ваших: ' || intxn_size::TEXT || ' строк';
            |   RETURN NEXT 'Размер объединения результатов робота и ваших: ' || union_size::TEXT || ' строк';
            |   RETURN;
            |end if;
            |
            |RETURN NEXT 'Кортежи (ship, port) найдены верно';
            |
            |SELECT MAX(ABS(diff)) INTO max_abs_int_diff FROM (
            |   SELECT SUM(transfers_num * CASE query_id WHEN 1 THEN 1 ELSE -1 END) AS diff
            |   FROM Task05_Merged
            |   GROUP BY ship, port
            |) AS T;
            |RETURN NEXT 'Максимальное расхождение transfers_num равно  ' || max_abs_int_diff::TEXT;
            |
            |SELECT MAX(ABS(diff)) INTO max_abs_decimal_diff FROM (
            |   SELECT SUM(transfer_size * CASE query_id WHEN 1 THEN 1 ELSE -1 END) AS diff
            |   FROM Task05_Merged
            |   GROUP BY ship, port
            |) AS T;
            |RETURN NEXT 'Максимальное расхождение transfer_size равно  ' || max_abs_decimal_diff::TEXT;
            |
            |END;
            |$$ LANGUAGE PLPGSQL;
            |
            |DROP FUNCTION Task05_User() CASCADE;
            """.trimMargin()
        val expectedPerSubmissionCode = """
            |SELECT set_config(
            |   'search_path',
            |   'cw3,' || current_setting('search_path'),
            |   false
            |);
            |
            |CREATE OR REPLACE FUNCTION Task05_User()
            |RETURNS TABLE(ship TEXT, port INT, transfers_num INT, transfer_size DOUBLE PRECISION, product TEXT) AS $$
            |{1}
            |$$ LANGUAGE SQL;
            |
            |CREATE OR REPLACE VIEW Task05_Merged AS
            |   SELECT 0 AS query_id, * FROM Task05_Robot()
            |   UNION ALL
            |   SELECT 1 AS query_id, * FROM Task05_User();
            """.trimMargin()

        val keyAttribute = listOf(
                TaskResultColumn("ship", SqlDataType.TEXT),
                TaskResultColumn("port", SqlDataType.INT))
        val nonKeyAttributes = listOf(
                TaskResultColumn("transfers_num", SqlDataType.INT),
                TaskResultColumn("transfer_size", SqlDataType.DOUBLE_PRECISION),
                TaskResultColumn("product", SqlDataType.TEXT))
        val relationSpec = RelationSpec(keyAttribute, nonKeyAttributes)
        val matcherSpec = MatcherSpec(relationSpec, "Множество пар (корабль, порт) отличается от результатов робота")

        val generator = CodeGenerator("cw3", "/cw3/schema.sql")
        val task = MultiColumnTask("Task05", "SELECT 'ship', 1, 10, 500::DOUBLE PRECISION, 'prod'", matcherSpec)
        assertEquals(expectedStaticCode, task.generateStaticCode())
        assertEquals(expectedPerSubmissionCode, task.generateDynamicCode(generator))
    }
}