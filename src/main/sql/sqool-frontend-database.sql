DROP SCHEMA IF EXISTS Contest CASCADE;
CREATE SCHEMA Contest;

SET search_path=Contest,public;

---------------------------------------------------------------------
-- Script is just an SQL code which creates database tables. It is used in the contest variants.
CREATE TABLE Script(
  id INT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
  description TEXT,
  body TEXT
);

CREATE OR REPLACE VIEW ScriptDto AS
SELECT id, description, body
FROM Script;

-----------------------------------------------------------------------------------------------------------------------
-- Tables for storing contest tasks, users and their submission attempts.
CREATE TABLE Contest.ContestUser(
  id SERIAL PRIMARY KEY,
  name TEXT UNIQUE,
  nick TEXT,
  passwd TEXT,
  is_admin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE Contest.Task(
  id INT PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
  name TEXT,
  real_name TEXT UNIQUE,
  description TEXT,
  solution TEXT,
  score INT CHECK(score BETWEEN 1 AND 10) DEFAULT 1,
  difficulty INT CHECK(difficulty BETWEEN 1 AND 3) DEFAULT 1,
  author_id INT REFERENCES Contest.ContestUser,
  script_id INT REFERENCES Contest.Script
);

CREATE TABLE Contest.TaskResult(
  task_id INT NOT NULL REFERENCES Task,
  col_num INT NOT NULL,
  col_name TEXT NOT NULL,
  col_type TEXT NOT NULL DEFAULT 'TEXT',
  CHECK(col_num > 0 AND col_name != '' OR col_num = 0 AND col_name = ''),
  PRIMARY KEY(task_id, col_num)
);

----------------------------------------------
-- Updatable view for showing tasks in the admin UI
CREATE OR REPLACE VIEW Contest.TaskDto AS
-- All tasks with non-empty results will have result_json looking like this:
-- [{"name": "col1", "type": "INT"}, {"name": "col2", type: "TEXT"}]
SELECT id, name, script_id, author_id,
  COALESCE(description, '') AS description,
  COALESCE(solution, '') AS solution,
  array_to_json(array_agg(json_object('{name, type}', ARRAY[col_name, col_type])))::TEXT AS result_json
FROM Contest.Task T JOIN Contest.TaskResult R ON T.id=R.task_id
GROUP BY T.id
UNION ALL
-- All tasks with empty results will have empty array in the result_json:
-- []
SELECT id, name, script_id, author_id,
  COALESCE(description, '') AS description,
  COALESCE(solution, '') AS solution,
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
      INSERT INTO Contest.Task(name, real_name, description, solution, script_id, author_id)
        VALUES (NEW.name, NEW.name, NEW.description, NEW.solution, NEW.script_id, NEW.author_id)
        RETURNING id
    )
    SELECT id INTO new_task_id
    FROM T;
  ELSE
    UPDATE Contest.Task SET name = NEW.name, real_name = NEW.name, description = NEW.description,
                            solution = NEW.solution, script_id = NEW.script_id
    WHERE id = NEW.id;
    SELECT NEW.id INTO new_task_id;
  END IF;

  DELETE FROM Contest.TaskResult WHERE task_id = new_task_id;
  WITH T AS (
    SELECT new_task_id AS task_id, X.*
    FROM json_to_recordset(NEW.result_json::JSON) AS X(col_num INT, col_name TEXT, col_type TEXT)
  )
  INSERT INTO Contest.TaskResult(task_id, col_num, col_name, col_type)
  SELECT task_id, col_num, col_name, col_type
  FROM T;
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

---------------------------------------------------------------------
-- Contest is a tournament which runs in some time interval.
-- Contest consists of a few variants.
CREATE TYPE VariantChoice AS ENUM('RANDOM', 'ALL', 'ANY');
CREATE TABLE Contest(
  code TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  dates TSTZRANGE NOT NULL DEFAULT tstzrange(NOW(), NOW() + interval '1h'),
  variant_choice VariantChoice NOT NULL DEFAULT 'ANY'
);

-------------------------------------------------------------------------
-- Variant is a collection of tasks which need to be solved
-- Variant is a part of contest and has a number which is unique in the contest scope
CREATE TABLE Variant(
  id INTEGER PRIMARY KEY GENERATED BY DEFAULT AS IDENTITY,
  name TEXT NOT NULL
);

CREATE TABLE VariantContest(
  variant_id INTEGER NOT NULL REFERENCES Contest.Variant ON DELETE CASCADE ON UPDATE CASCADE,
  contest_code TEXT NOT NULL REFERENCES Contest.Contest ON DELETE CASCADE ON UPDATE CASCADE,
  PRIMARY KEY (variant_id, contest_code)
);

CREATE OR REPLACE VIEW ContestDto AS
SELECT code, name, lower(dates) AS start_ts, upper(dates) AS end_ts, variant_choice,
       array_to_json(array_agg(variant_id))::TEXT AS variants_id_json_array
  FROM Contest.Contest C JOIN Contest.VariantContest V ON C.code = V.contest_code
  GROUP BY C.code
UNION ALL
-- Contests that have no variants will have empty array in the variants_id_json_array:
SELECT code, name, lower(dates) AS start_ts, upper(dates) AS end_ts, variant_choice, '[]' AS variants_id_json_array
  FROM Contest.Contest C LEFT JOIN Contest.VariantContest V ON C.code = V.contest_code
  WHERE V.contest_code IS NULL
  GROUP BY C.code;

CREATE OR REPLACE FUNCTION ContestDto_Insert()
  RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO Contest.Contest(name, code, dates)
  VALUES (NEW.name, NEW.code, tstzrange(NEW.start_ts, NEW.end_ts, '[]'));

  WITH T AS (
      SELECT NEW.code AS contest_code, value::INT AS variant_id
      FROM json_array_elements_text(NEW.variants_id_json_array::JSON)
  )
  INSERT INTO Contest.VariantContest(variant_id, contest_code)
  SELECT variant_id, contest_code FROM T;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ContestDto_Update()
  RETURNS TRIGGER AS $$
BEGIN
  UPDATE Contest.Contest
  SET name = NEW.name, dates = tstzrange(NEW.start_ts, NEW.end_ts, '[]')
  WHERE code = NEW.code;

  DELETE FROM Contest.VariantContest WHERE contest_code = NEW.code;
  WITH T AS (
      SELECT NEW.code AS contest_code, value::INT AS variant_id
      FROM json_array_elements_text(NEW.variants_id_json_array::JSON)
  )
  INSERT INTO Contest.VariantContest(variant_id, contest_code)
  SELECT variant_id, contest_code FROM T;

  RETURN NEW;
end;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ContestDto_Insert_Trigger
  INSTEAD OF INSERT ON ContestDto
  FOR EACH ROW
EXECUTE PROCEDURE ContestDto_Insert();

CREATE TRIGGER ContestDto_Update_Trigger
  INSTEAD OF UPDATE ON ContestDto
  FOR EACH ROW
EXECUTE PROCEDURE ContestDto_Update();

CREATE TABLE TaskVariant(
  task_id INT NOT NULL REFERENCES Contest.Task ON DELETE CASCADE ON UPDATE CASCADE,
  variant_id INT NOT NULL REFERENCES Contest.Variant ON DELETE CASCADE ON UPDATE CASCADE,
  PRIMARY KEY (task_id, variant_id)
);

CREATE OR REPLACE VIEW VariantDto AS
SELECT V.id, V.name, array_to_json(array_agg(task_id))::TEXT AS tasks_id_json_array,
       COALESCE(json_agg(DISTINCT (script_id)) FILTER (WHERE script_id IS NOT NULL), '[]')::TEXT AS scripts_id_json_array
  FROM Contest.Variant V JOIN Contest.TaskVariant TV ON V.id = TV.variant_id
    JOIN Contest.Task T ON TV.task_id = T.id
    LEFT JOIN Contest.Script S ON T.script_id = S.id
  GROUP BY V.id
UNION ALL
-- Variants that have no tasks will have empty arrays in the tasks_id_json_array and scripts_id_json_array:
SELECT id, name, '[]' AS tasks_id_json_array, '[]' AS scripts_id_json_array
  FROM Contest.Variant V LEFT JOIN Contest.TaskVariant T ON V.id = T.variant_id
  WHERE T.variant_id IS NULL
  GROUP BY V.id;

CREATE OR REPLACE FUNCTION VariantDto_InsertUpdate()
RETURNS TRIGGER AS $$
DECLARE
  new_variant_id INT;
BEGIN
  IF NEW.id IS NULL THEN
    WITH T AS (
      INSERT INTO Contest.Variant(name)
        VALUES (NEW.name)
        RETURNING id
      )
    SELECT id INTO new_variant_id FROM T;
  ELSE
    UPDATE Contest.Variant SET name = NEW.name
    WHERE id = NEW.id;
    SELECT NEW.id INTO new_variant_id;
  END IF;

  DELETE FROM Contest.TaskVariant WHERE variant_id = new_variant_id;
  WITH T AS (
    SELECT new_variant_id AS variant_id, value::INT AS task_id
    FROM json_array_elements_text(NEW.tasks_id_json_array::JSON)
  )
  INSERT INTO Contest.TaskVariant(task_id, variant_id)
  SELECT task_id, variant_id FROM T;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER VariantDto_Insert_Trigger
  INSTEAD OF INSERT ON Contest.VariantDto
  FOR EACH ROW
EXECUTE PROCEDURE VariantDto_InsertUpdate();

CREATE TRIGGER VariantDto_Update_Trigger
  INSTEAD OF UPDATE ON Contest.VariantDto
  FOR EACH ROW
EXECUTE PROCEDURE VariantDto_InsertUpdate();

CREATE TABLE UserContest(
  user_id INT NOT NULL REFERENCES Contest.ContestUser ON DELETE CASCADE ON UPDATE CASCADE,
  contest_code TEXT NOT NULL REFERENCES Contest.Contest ON DELETE CASCADE ON UPDATE CASCADE,
  variant_id INT REFERENCES Contest.Variant ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (contest_code, variant_id) REFERENCES VariantContest(contest_code, variant_id),
  PRIMARY KEY (user_id, contest_code)
);

CREATE OR REPLACE VIEW AvailableContests AS
SELECT UC.user_id,
       UC.contest_code,
       C.name AS contest_name,
       C.variant_choice,
       UC.variant_id AS assigned_variant_id,
       json_agg(json_object('{id, name}', ARRAY[V.id::TEXT, V.name]))::TEXT AS variants_json_array
FROM Contest.UserContest UC
JOIN Contest C on UC.contest_code = C.code
JOIN Contest.VariantContest VC ON C.code = VC.contest_code
JOIN Contest.Variant V ON VC.variant_id = V.id
GROUP BY C.code, UC.user_id, UC.contest_code;

CREATE OR REPLACE FUNCTION AssignVariant(_user_id INT, _contest_code TEXT, _variant_id INT)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Contest.UserContest(user_id, contest_code, variant_id) VALUES (_user_id, _contest_code, _variant_id)
  ON CONFLICT (user_id, contest_code)
  DO UPDATE SET variant_id = _variant_id;
  PERFORM AcceptVariant(_user_id, _variant_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW TaskContest AS
SELECT C.code AS contest_code, V.variant_id, T.task_id
FROM Contest.Contest C
JOIN Contest.VariantContest V ON C.code = V.contest_code
JOIN Contest.TaskVariant T ON V.variant_id = T.variant_id;

CREATE TYPE AttemptStatus AS ENUM('success', 'failure', 'testing', 'virgin');
CREATE TABLE Contest.Attempt(
  task_id INT REFERENCES Contest.Task ON UPDATE CASCADE ON DELETE CASCADE,
  variant_id INT REFERENCES Contest.Variant ON UPDATE CASCADE ON DELETE CASCADE,
  user_id INT REFERENCES Contest.ContestUser,
  attempt_id TEXT UNIQUE,
  status AttemptStatus DEFAULT 'failure',
  attempt_text TEXT,
  count INT DEFAULT 0,
  testing_start_ts TIMESTAMP,
  FOREIGN KEY(task_id, variant_id) REFERENCES TaskVariant(task_id, variant_id) ON UPDATE CASCADE ON DELETE CASCADE,
  PRIMARY KEY(task_id, user_id, variant_id)
);
CREATE TABLE Contest.GradingDetails(
  attempt_id TEXT REFERENCES Contest.Attempt(attempt_id) ON UPDATE CASCADE ON DELETE CASCADE,
  error_msg TEXT,
  result_set TEXT
);

CREATE TABLE Contest.SolutionReview (
  task_id INT REFERENCES Contest.Task ON UPDATE CASCADE ON DELETE CASCADE,
  variant_id INT REFERENCES Contest.Variant ON UPDATE CASCADE ON DELETE CASCADE,
  user_id INT REFERENCES Contest.ContestUser,
  reviewer_id INT,
  solution_review TEXT,
  PRIMARY KEY(task_id, variant_id, user_id, reviewer_id),
  FOREIGN KEY(task_id, variant_id, user_id) REFERENCES Contest.Attempt(task_id, variant_id, user_id)  ON UPDATE CASCADE ON DELETE CASCADE
);

------------------------------------------------------------------------------------------------
-- This function generates a nickname from a random combination of first name (adjective)
-- and last name (real scientist last name)
CREATE OR REPLACE FUNCTION GenNickname() RETURNS TEXT AS $$
DECLARE
  fnames TEXT[];
  lnames TEXT[];
  _result TEXT;
BEGIN
fnames = ARRAY['admiring', 'adoring', 'agitated', 'amazing', 'angry', 'awesome',
    'backstabbing', 'berserk', 'big', 'boring', 'clever', 'cocky', 'compassionate',
    'condescending', 'cranky', 'desperate', 'determined', 'distracted', 'dreamy',
    'drunk', 'ecstatic', 'elated', 'elegant', 'evil', 'fervent', 'focused', 'furious',
    'gigantic', 'gloomy', 'goofy', 'grave', 'happy', 'high', 'hopeful', 'hungry',
    'insane', 'jolly', 'jovial', 'kickass', 'lonely', 'loving', 'mad', 'modest',
    'naughty', 'nauseous', 'nostalgic', 'pedantic', 'pensive', 'prickly', 'reverent',
    'romantic', 'sad', 'serene', 'sharp', 'sick', 'silly', 'sleepy', 'small', 'stoic',
    'stupefied', 'suspicious', 'tender', 'thirsty', 'tiny', 'trusting'];
lnames = ARRAY['albattani', 'allen', 'almeida', 'archimedes', 'ardinghelli', 'aryabhata',
    'austin', 'babbage', 'banach', 'bardeen', 'bartik', 'bassi', 'bell', 'bhabha', 'bhaskara',
    'blackwell', 'bohr', 'booth', 'borg', 'bose', 'boyd', 'brahmagupta', 'brattain', 'brown',
    'carson', 'chandrasekhar', 'colden', 'cori', 'cray', 'curie', 'darwin', 'davinci', 'dijkstra',
    'dubinsky', 'easley', 'einstein', 'elion', 'engelbart', 'euclid', 'euler', 'fermat', 'fermi',
    'feynman', 'franklin', 'galileo', 'gates', 'goldberg', 'goldstine', 'golick', 'goodall',
    'hamilton', 'hawking', 'heisenberg', 'heyrovsky', 'hodgkin', 'hoover', 'hopper', 'hugle',
    'hypatia', 'jang', 'jennings', 'jepsen', 'joliot', 'jones', 'kalam', 'kare', 'keller',
    'khorana', 'kilby', 'kirch', 'knuth', 'kowalevski', 'lalande', 'lamarr', 'leakey',
    'leavitt', 'lichterman', 'liskov', 'lovelace', 'lumiere', 'mahavira', 'mayer', 'mccarthy',
    'mcclintock', 'mclean', 'mcnulty', 'meitner', 'meninsky', 'mestorf', 'mirzakhani', 'morse',
    'newton', 'nobel', 'noether', 'northcutt', 'noyce', 'panini', 'pare', 'pasteur', 'payne',
    'perlman', 'pike', 'poincare', 'poitras', 'ptolemy', 'raman', 'ramanujan', 'ride', 'ritchie',
    'roentgen', 'rosalind', 'saha', 'sammet', 'shaw', 'shockley', 'sinoussi', 'snyder', 'spence',
    'stallman', 'swanson', 'swartz', 'swirles', 'tesla', 'thompson', 'torvalds', 'turing', 'varahamihira',
    'visvesvaraya', 'wescoff', 'williams', 'wilson', 'wing', 'wozniak', 'wright', 'yalow', 'yonath'];
WITH FirstName AS (
  SELECT unnest(fnames) AS value, generate_series(1, array_length(fnames, 1)) AS id
),
LastName AS (
  SELECT unnest(lnames) AS value, generate_series(1, array_length(lnames, 1)) AS id
),
Randoms AS (
  SELECT (0.5 + random() * (SELECT MAX(id) FROM FirstName))::INT AS fname_id,
           (0.5 + random() * (SELECT MAX(id) FROM LastName))::INT AS lname_id
)
SELECT F.value || '_' || L.value INTO _result
FROM FirstName F JOIN Randoms R ON F.id = R.fname_id JOIN LastName L ON L.id = R.lname_id;
RETURN _result;
END;
$$ LANGUAGE plpgsql;


/**********************************************************************************************************************
 * Procedures which are executed in the contest run time when users register and submit their solutions.
 */

-----------------------------------------------------------------------------------------------------------------------
-- Returns existing of creates new user with the given name and password, generating nickname if requested
CREATE OR REPLACE FUNCTION GetOrCreateContestUser(argName TEXT, argPass TEXT, generateNick BOOLEAN)
RETURNS TABLE(id INT, nick TEXT, name TEXT, passwd TEXT, is_admin BOOLEAN, code INT) AS $$
DECLARE
  _id INT;
  _name TEXT;
  _passwd TEXT;
  _nick TEXT;
  _is_admin BOOLEAN;
BEGIN
  SELECT ContestUser.id, ContestUser.name, ContestUser.nick, ContestUser.passwd, ContestUser.is_admin
  INTO _id, _name, _nick, _passwd, _is_admin
  FROM ContestUser WHERE ContestUser.name=argName;
  IF FOUND THEN
    IF md5(argPass) <> _passwd THEN
      RETURN QUERY SELECT NULL::INT, NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::BOOLEAN, 1;
      RETURN;
    END IF;

    RETURN QUERY SELECT _id, _nick, _name, _passwd, _is_admin, 0;
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
  RETURN QUERY SELECT U.id, U.nick, U.name, U.passwd, U.is_admin, 0 AS code FROM ContestUser U WHERE U.name = argName;
  RETURN;
END;
$$ LANGUAGE plpgsql;

-----------------------------------------------------------------------------------------------------------------------
-- Accepts all tasks authored by a randomly chosen author. This is useful for random assignment of test tasks.
CREATE OR REPLACE FUNCTION AcceptRandomAuthor(_user_id INT)
RETURNS VOID AS $$
-- INSERT INTO Contest.Attempt(user_id, task_id, status)
-- SELECT _user_id, T.id, 'virgin'::AttemptStatus
-- FROM Contest.Task T JOIN (
--   SELECT (0.5 + random()*2)::INT + (SELECT MIN(id) FROM ContestUser)- 1
-- ) AS R(author_id) using (author_id)
$$ LANGUAGE SQL;

-----------------------------------------------------------------------------------------------------------------------
-- Records that contest user _user_id decided to accept the challenge of task _task_id. It sets status "virgin"
-- meaning that no solution have been submitted yet
CREATE OR REPLACE FUNCTION MakeAttempt(_user_id INT, _task_id INT)
RETURNS VOID AS $$
  INSERT INTO Contest.Attempt(user_id, task_id, status) VALUES (_user_id, _task_id, 'virgin');
$$ LANGUAGE SQL;

CREATE OR REPLACE FUNCTION AcceptVariant(_user_id INT, _variant_id INT)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Contest.Attempt(user_id, task_id, variant_id, status)
  SELECT _user_id, task_id, _variant_id, 'virgin' FROM Contest.TaskVariant
  WHERE variant_id = _variant_id
  ON CONFLICT DO NOTHING;
END;
$$ LANGUAGE plpgsql;

-----------------------------------------------------------------------------------------------------------------------
-- Changes the status of the given _task_id to "testing" for the given user and records attempt identifier which
-- is opaque value.
CREATE OR REPLACE FUNCTION StartAttemptTesting(_user_id INT, _task_id INT, _variant_id INT, _attempt_id TEXT)
RETURNS VOID AS $$
  DELETE FROM Contest.GradingDetails WHERE attempt_id IN (
    SELECT attempt_id
    FROM Contest.Attempt
    WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id
  );

  UPDATE Contest.Attempt SET status = 'testing', testing_start_ts = NOW(), attempt_id = _attempt_id
  WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id;
$$ LANGUAGE SQL;

-----------------------------------------------------------------------------------------------------------------------
-- Records the result of submission grading.
-- Submission is identified by _attempt_id. Failed attempt is marked with _success set to FALSE, and in this case
-- _errorMsg contains error message provided by the grader and
-- _resultLines contains the first lines of the result set, if any
CREATE OR REPLACE FUNCTION RecordAttemptResult(_attemptId TEXT, _success BOOLEAN, _errorMsg TEXT, _resultLines TEXT)
RETURNS VOID AS $$
  UPDATE Contest.Attempt
  SET status = (CASE _success WHEN true THEN 'success'::AttemptStatus ELSE 'failure'::AttemptStatus END),
  count = count+1
  WHERE attempt_id = _attemptId;

  INSERT INTO Contest.GradingDetails(attempt_id, error_msg, result_set) VALUES (_attemptId, _errorMsg, _resultLines);
$$ LANGUAGE SQL;

-----------------------------------------------------------------------------------------------------------------------
-- View which provides data on user submissions
CREATE OR REPLACE VIEW MyAttempts AS
SELECT  T.id AS task_id,
        T.name,
        T.difficulty,
        T.score,
        T.description,
        CASE WHEN TR.task_id IS NULL THEN '[]'
             ELSE json_agg(json_object('{name, type}', ARRAY[col_name, col_type]))::TEXT
        END AS signature,
        T.author_id,
        U.nick AS author_nick,
        A.attempt_id,
        A.user_id,
        A.variant_id,
        S.nick AS user_nick,
        S.name AS user_name,
        A.status::TEXT,
        A.count,
        A.testing_start_ts,
        D.error_msg,
        D.result_set
FROM Contest.Task T
JOIN Contest.ContestUser U ON T.author_id = U.id
JOIN Contest.Attempt A ON A.task_id = T.id
JOIN Contest.ContestUser S ON A.user_id = S.id
LEFT JOIN Contest.GradingDetails D ON A.attempt_id = D.attempt_id
LEFT JOIN Contest.TaskResult TR ON TR.task_id = T.id
GROUP BY T.id, TR.task_id, A.user_id, A.task_id, A.variant_id, S.id, D.error_msg, D.result_set, U.id;

CREATE OR REPLACE VIEW AttemptsByContest AS
SELECT C.contest_code, A.*
FROM Contest.VariantContest C
JOIN Contest.MyAttempts A ON C.variant_id = A.variant_id;

CREATE OR REPLACE VIEW TaskSubmissionsStats AS
SELECT T.id AS task_id, T.name AS task_name, C.contest_code, SUM(CASE WHEN A.status = 'success' THEN 1 ELSE 0 END) AS solved,
       SUM(CASE WHEN A.count > 0 THEN 1 ELSE 0 END) AS attempted
FROM Contest.Attempt A
JOIN Contest.Task T ON A.task_id = T.id
JOIN Contest.VariantContest C ON C.variant_id = A.variant_id
GROUP BY T.id, C.contest_code;

/**********************************************************************************************************************
 * Administrative procedures and views
 */
CREATE OR REPLACE FUNCTION AddTask(_id INT, _name TEXT, _real_name TEXT, _args TEXT, _description TEXT, _score INT, _difficulty INT, _author_name TEXT)
RETURNS VOID AS $$
DECLARE
  _author_id INT;
BEGIN
SELECT id INTO _author_id FROM ContestUser WHERE name = _author_name;
IF NOT FOUND THEN
  RAISE EXCEPTION 'Пользователь с именем % не найден', _author_name;
END IF;
INSERT INTO Contest.Task(id, name, real_name, signature, description, score, difficulty, author_id)
VALUES (_id, _name, _real_name, _args, _description, _score, _difficulty, _author_id);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE VIEW GainPerTask AS
WITH SolvedCounts AS (
SELECT T.id, T.author_id, T.difficulty, T.score,
       SUM(CASE WHEN A.status = 'success' THEN 1 ELSE 0 END) AS solved, SUM(CASE WHEN A.count > 0 THEN 1 ELSE 0 END) AS attempted
FROM Contest.Task T LEFT OUTER JOIN Contest.Attempt A ON A.task_id = T.id
GROUP BY T.id
)
SELECT *, CASE WHEN attempted > 0 THEN score::NUMERIC(4,2)/(solved + 1) ELSE 0 END AS gain
FROM SolvedCounts;


CREATE OR REPLACE VIEW TasksByAuthor AS
SELECT A.id, A.nick,
       SUM(CASE WHEN difficulty = 1 THEN 1 ELSE 0 END) AS count1,
       SUM(CASE WHEN difficulty = 1 THEN gain ELSE 0 END) AS gain1,
       SUM(CASE WHEN difficulty = 2 THEN 1 ELSE 0 END) AS count2,
       SUM(CASE WHEN difficulty = 2 THEN gain ELSE 0 END) AS gain2,
       SUM(CASE WHEN difficulty = 3 THEN 1 ELSE 0 END) AS count3,
       SUM(CASE WHEN difficulty = 3 THEN gain ELSE 0 END) AS gain3,
       SUM(gain) AS total_gain
FROM ContestUser A JOIN GainPerTask T ON T.author_id = A.id
GROUP BY A.id;

CREATE OR REPLACE VIEW LeaderboardView AS
WITH SolvedCounts AS (
  SELECT T.id, T.author_id, T.difficulty, T.score, SUM(CASE WHEN A.status = 'success' THEN 1 ELSE 0 END) AS solved,
         SUM(CASE WHEN A.count > 0 THEN 1 ELSE 0 END) AS attempted
  FROM Contest.Task T JOIN Contest.Attempt A ON A.task_id = T.id
  GROUP BY T.id
),
AuthoredTaskGain AS (
  SELECT *, CASE WHEN attempted > 0 THEN score::NUMERIC(4,2)/(solved + 1) ELSE 0 END AS total_gain
  FROM SolvedCounts
),
SolvedTaskGain AS (
  SELECT user_id, SUM(total_gain) AS total_gain
  FROM MyAttempts A JOIN AuthoredTaskGain G ON A.task_id = G.id
  WHERE A.status = 'success'
  GROUP BY user_id
),
SumGain AS (
  SELECT
    COALESCE(S.user_id, A.author_id) AS user_id,
    COALESCE(S.total_gain, 0) AS solver_gain,
    COALESCE(A.total_gain, 0) AS author_gain,
    COALESCE(S.total_gain, 0) + COALESCE(A.total_gain, 0) AS total_gain
  FROM SolvedTaskGain S FULL OUTER JOIN AuthoredTaskGain A ON S.user_id = A.author_id
)
SELECT
  U.nick,
  SUM(COALESCE(total_gain, 0)) AS total_gain,
  SUM(COALESCE(solver_gain, 0)) AS solver_gain,
  SUM(COALESCE(author_gain, 0)) AS author_gain
FROM Contest.ContestUser U LEFT JOIN SumGain G ON U.id = G.user_id
GROUP BY U.id;

CREATE OR REPLACE VIEW DisclosedLeaderboard AS
SELECT u.name,
       max(l.total_gain) AS total_gain,
       max(solver_gain) AS solver_gain,
       max(author_gain) AS author_gain,
       array_agg(T.name)
FROM attempt a JOIN task t on a.task_id=t.id
JOIN contestuser u ON u.id=a.user_id
JOIN leaderboardview l ON l.nick=u.nick
WHERE status = 'success' GROUP BY u.id;

