-- When grader executes this code, it creates a new random schema
-- and search path includes only that schema name, like "qwertyuiop"

-- Here we add (prepend) contest schema to the search path
SELECT set_config(
    ''search_path'',
    ''cw2_common,'' || current_setting(''search_path''),
    false
  );

-- Now we take student's code and put it inside stored procedure. 
-- This code is processed as template with placeholders. In this particular
-- case there is a single placeholder {1} and the template engine 
-- is MessageFormat from Java SE
CREATE OR REPLACE FUNCTION Task111_User() RETURNS TABLE(pie_name TEXT) as $$
  {1}
$$ LANGUAGE SQL;

-- Re-create view definition.
CREATE OR REPLACE VIEW Task111_Merged AS
  SELECT 0 AS query_id, * FROM Task111_Robot()
  UNION ALL
  SELECT 1 AS query_id, * FROM Task111_User();


-- Now we have a search path which includes static code and per-submission
-- code. Running SELECT * FROM Task111_Matcher() in such environment
-- will execute matcher's code which will compare the results of (static) robot's query
-- and student's submission