-- These statements create a schema for some particular contest
-- where "contest" might be a variant of control work or actual contest.
-- There is a single schema with tables and robot procedures for all
-- contest participants and separate schema with student's code 
-- for every single submission

-- Create schema
CREATE SCHEMA cw2_common;
SET search_path=cw2_common;
-- Create tables (they are stored in a separate file to allow for 
-- code reuse between different variants)
\i /workspace/au2018/cw2/schema1.sql


-- Task 11
-- This is robot's query. The query itself is written by the teacher
-- while boilerplate function can be generated from task name and
-- result set specification
CREATE OR REPLACE FUNCTION Task111_Robot() RETURNS TABLE(pie_name TEXT) as $$
SELECT P.Title
FROM
     Ingredients I
JOIN PieComponents PC ON I.Id = PC.IngredientId
JOIN Pies P ON PC.PieId = P.Id
JOIN IngredientsRemaining IR ON IR.IngredientId = I.Id
WHERE I.Title='Мука' AND PC.Amount*10 <=  IR.Amount;
$$ LANGUAGE SQL;

-- This is a stub of the student's query. It is required here to make matcher's code
-- compilable. In the code below we delete this stored procedure, as well
-- as the view, and re-create them again when student's submission comes
-- to the grader.
-- This code can be completely generated
-- === from here ===
CREATE OR REPLACE FUNCTION Task111_User() RETURNS TABLE(pie_name TEXT) as $$
SELECT NULL::TEXT
$$ LANGUAGE SQL;

CREATE OR REPLACE VIEW Task111_Merged AS
  SELECT 0 AS query_id, * FROM Task111_Robot()
  UNION ALL
  SELECT 1 AS query_id, * FROM Task111_User();
-- === up to here === 

-- This is a simple matcher.
-- It calculates the size of intersection and union of robot's and 
-- student's results, and then takes some actions depending on
-- the sizes. This particular matcher can be completely generated 
-- because the result set is very simple (just one column) and 
-- we do not add any query-specific hints to the robot's answers.
CREATE OR REPLACE FUNCTION Task111_Matcher()
RETURNS SETOF TEXT AS $$
DECLARE
  -- Size of results intersection
  intxn_size INT;
  -- Size of the union of results
  union_size INT;
  -- Size of the robot's result set
  robot_size INT;
  -- Size of the student's result set
  user_size INT;

BEGIN
-- Calculate intersection and union
SELECT COUNT(1) INTO intxn_size FROM (
  SELECT pie_name FROM Task111_Merged WHERE query_id = 0
  INTERSECT
  SELECT pie_name FROM Task111_Merged WHERE query_id = 1
) AS T;

SELECT COUNT(1) INTO union_size FROM (
  SELECT pie_name FROM Task111_Merged WHERE query_id = 0
  UNION
  SELECT pie_name FROM Task111_Merged WHERE query_id = 1
) AS T;

-- If size of intersection is not the same as size of the union 
-- then student's submission has failed
IF intxn_size != union_size THEN
  RETURN NEXT 'Ваши результаты отличаются от результатов робота';
  RETURN NEXT 'Размер пересечения результатов робота и ваших: ' || intxn_size::TEXT || ' строк';
  RETURN NEXT 'Размер объединения результатов робота и ваших: ' || union_size::TEXT || ' строк';
  RETURN;
end if;

-- Otherwise we need to check if there are any duplicates which may or 
-- may not be allowed by the task.
SELECT COUNT(*) INTO robot_size FROM Task111_Merged WHERE query_id = 0;
SELECT COUNT(*) INTO user_size FROM Task111_Merged WHERE query_id = 1;

-- If results sizes are different then something is wrong with 
-- the student's submission (most often it is the case when duplicates
-- are not removed)
IF robot_size != user_size THEN
  RETURN NEXT 'Ваши результаты совпадают с результатами робота как множества, но отличаются размером';
  RETURN NEXT 'У вас в результате ' || user_size::TEXT || ' строк';
  RETURN NEXT 'У робота в результате ' || robot_size::TEXT || ' строк';
  RETURN;
end if;

END;
$$ LANGUAGE plpgsql;

-- Now when matcher is created we drop a stub of student's stored procedure. It will
-- be created again when student's submission comes to the grader.
DROP FUNCTION Task111_User() CASCADE;