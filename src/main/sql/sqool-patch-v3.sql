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


CREATE TABLE Contest.AttemptHistory(
   attempt_id TEXT PRIMARY KEY,
   task_id INT,
   variant_id INT,
   contest_code TEXT,
   user_id INT,
   status AttemptStatus DEFAULT 'failure',
   attempt_text TEXT,
   testing_start_ts TIMESTAMP
);

CREATE OR REPLACE FUNCTION RecordAttemptResult(_attemptId TEXT, _success BOOLEAN, _errorMsg TEXT, _resultLines TEXT)
    RETURNS VOID AS $$
    UPDATE Contest.Attempt
    SET status = (CASE _success WHEN true THEN 'success'::AttemptStatus ELSE 'failure'::AttemptStatus END),
        count = count+1
    WHERE attempt_id = _attemptId;

    INSERT INTO AttemptHistory(attempt_id, task_id, variant_id, contest_code, user_id, attempt_text, testing_start_ts)
    SELECT attempt_id, task_id, variant_id, contest_code, user_id, attempt_text, testing_start_ts
    FROM Attempt
    WHERE attempt_id = _attemptId;

    INSERT INTO Contest.GradingDetails(attempt_id, error_msg, result_set) VALUES (_attemptId, _errorMsg, _resultLines);
$$ LANGUAGE SQL;

ALTER TABLE ContestUser ADD COLUMN email TEXT UNIQUE;

DROP FUNCTION GetOrCreateContestUser;
CREATE OR REPLACE FUNCTION GetOrCreateContestUser(argName TEXT, argPass TEXT, generateNick BOOLEAN)
RETURNS TABLE(id INT, nick TEXT, name TEXT, passwd TEXT, is_admin BOOLEAN, email TEXT, code INT) AS $$
DECLARE
  _id INT;
  _name TEXT;
  _passwd TEXT;
  _nick TEXT;
  _is_admin BOOLEAN;
  _email TEXT;
BEGIN
  SELECT ContestUser.id, ContestUser.name, ContestUser.nick, ContestUser.passwd, ContestUser.is_admin, ContestUser.email
  INTO _id, _name, _nick, _passwd, _is_admin, _email
  FROM ContestUser WHERE ContestUser.name=argName;
  IF FOUND THEN
    IF md5(argPass) <> _passwd THEN
      RETURN QUERY SELECT NULL::INT, NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::BOOLEAN, NULL::TEXT, 1;
      RETURN;
    END IF;

    RETURN QUERY SELECT _id, _nick, _name, _passwd, _is_admin, _email, 0;
    RETURN;
  END IF;

  LOOP
    IF generateNick THEN
      _nick := (SELECT GenNickname());
    ELSE
      _nick := argName;
      generateNick := TRUE;
    END IF;
  EXIT WHEN NOT EXISTS (SELECT * FROM ContestUser WHERE ContestUser.nick = _nick);
  END LOOP;

  WITH T AS (
      INSERT INTO ContestUser (name, nick, passwd, is_admin) VALUES (argName, _nick, md5(argPass), COALESCE(_is_admin, FALSE)) RETURNING ContestUser.id
  )
  SELECT T.id INTO _id FROM T;
  INSERT INTO UserContest(user_id, contest_code) SELECT _id, Contest.code FROM Contest;
  RETURN QUERY SELECT U.id, U.nick, U.name, U.passwd, U.is_admin, U.email, 0 AS code FROM ContestUser U WHERE U.name = argName;
  RETURN;
END;
$$ LANGUAGE plpgsql;


