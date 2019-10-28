SET search_path=Contest,public;
ALTER TABLE Task ADD COLUMN has_result BOOLEAN NOT NULL DEFAULT TRUE;

DROP VIEW TaskDto;
----------------------------------------------
-- Updatable view for showing tasks in the admin UI
CREATE OR REPLACE VIEW Contest.TaskDto AS
-- All tasks with non-empty results will have result_json looking like this:
-- [{"name": "col1", "type": "INT"}, {"name": "col2", type: "TEXT"}]
SELECT id, name, script_id, author_id,
  COALESCE(description, '') AS description,
  COALESCE(solution, '') AS solution,
  has_result AS has_result,
  array_to_json(array_agg(json_object('{name, type, num}', ARRAY[col_name, col_type, col_num::TEXT])))::TEXT AS result_json
FROM Contest.Task T JOIN Contest.TaskResult R ON T.id=R.task_id
GROUP BY T.id
UNION ALL
-- All tasks with empty results will have empty array in the result_json:
-- []
SELECT id, name, script_id, author_id,
  COALESCE(description, '') AS description,
  COALESCE(solution, '') AS solution,
  has_result AS has_result,
  '[]' AS result_json
FROM Contest.Task T LEFT JOIN Contest.TaskResult R ON T.id=R.task_id
WHERE R.task_id IS NULL
GROUP BY T.id;

CREATE OR REPLACE FUNCTION TaskDto_Insert()
RETURNS TRIGGER AS $$
DECLARE
  new_task_id INT;
BEGIN
  IF NEW.id IS NULL THEN
    WITH T AS (
      INSERT INTO Contest.Task(name, real_name, description, has_result, solution, script_id, author_id)
        VALUES (NEW.name, NEW.name, NEW.description, NEW.has_result, NEW.solution, NEW.script_id, NEW.author_id)
        RETURNING id
    )
    SELECT id INTO new_task_id
    FROM T;
  ELSE
    UPDATE Contest.Task
    SET name = NEW.name,
        real_name = NEW.name,
        description = NEW.description,
        has_result = NEW.has_result,
        solution = NEW.solution,
        script_id = NEW.script_id
    WHERE id = NEW.id;
    SELECT NEW.id INTO new_task_id;
  END IF;

  DELETE FROM Contest.TaskResult WHERE task_id = new_task_id;
  IF NEW.has_result THEN
      WITH T AS (
        SELECT new_task_id AS task_id, X.*
        FROM json_to_recordset(NEW.result_json::JSON) AS X(col_num INT, col_name TEXT, col_type TEXT)
      )
      INSERT INTO Contest.TaskResult(task_id, col_num, col_name, col_type)
      SELECT task_id, col_num, col_name, col_type
      FROM T;
  END IF;
RETURN NEW;
end;
$$ LANGUAGE plpgsql;

CREATE TRIGGER TaskDto_Insert_Trigger
INSTEAD OF INSERT ON Contest.TaskDto
FOR EACH ROW
EXECUTE PROCEDURE TaskDto_Insert();

CREATE TRIGGER TaskDto_Update_Trigger
    INSTEAD OF UPDATE ON Contest.TaskDto
    FOR EACH ROW
EXECUTE PROCEDURE TaskDto_Insert();
