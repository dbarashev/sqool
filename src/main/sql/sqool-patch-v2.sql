SET search_path=Contest,public;

ALTER TABLE Contest.GradingDetails ALTER COLUMN attempt_id SET NOT NULL;

ALTER TABLE Contest.Attempt ADD COLUMN contest_code TEXT REFERENCES Contest.Contest ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE Contest.Attempt DROP CONSTRAINT Attempt_pkey CASCADE;
with T as (select distinct contest_code,variant_id from usercontest where variant_id is not null)
update Attempt set contest_code=T.contest_code FROM T WHERE T.variant_id=Attempt.variant_id;
ALTER TABLE Contest.Attempt ADD PRIMARY KEY (task_id, user_id, variant_id, contest_code);

DROP VIEW MyAttempts CASCADE;
CREATE OR REPLACE VIEW MyAttempts AS
SELECT  T.id AS task_id,
        T.script_id AS schema_id,
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
        A.contest_code,
        S.nick AS user_nick,
        S.name AS user_name,
        S.uni AS user_uni,
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
GROUP BY T.id, TR.task_id, A.user_id, A.task_id, A.variant_id, A.contest_code, S.id, D.error_msg, D.result_set, U.id;

DROP TABLE Contest.SolutionReview;
CREATE TABLE Contest.SolutionReview (
  attempt_id TEXT NOT NULL REFERENCES Contest.Attempt(attempt_id) ON UPDATE CASCADE ON DELETE CASCADE,
  reviewer_id INT REFERENCES Contest.ContestUser,
  solution_review TEXT,
  PRIMARY KEY(attempt_id, reviewer_id)
);

CREATE OR REPLACE VIEW TaskSubmissionsStats AS
SELECT T.id AS task_id, T.name AS task_name, A.contest_code, SUM(CASE WHEN A.status = 'success' THEN 1 ELSE 0 END) AS solved,
       SUM(CASE WHEN A.count > 0 THEN 1 ELSE 0 END) AS attempted
FROM Contest.Attempt A
JOIN Contest.Task T ON A.task_id = T.id
GROUP BY T.id, A.contest_code;

CREATE OR REPLACE VIEW ReviewByUser AS
SELECT S.attempt_id, S.solution_review, A.user_id
FROM Contest.SolutionReview S
JOIN Contest.Attempt A ON S.attempt_id = A.attempt_id;

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
JOIN Leaderboardview l ON l.nick=u.nick
WHERE status = 'success' GROUP BY u.id;

CREATE OR REPLACE FUNCTION AssignVariant(_user_id INT, _contest_code TEXT, _variant_id INT)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Contest.UserContest(user_id, contest_code, variant_id) VALUES (_user_id, _contest_code, _variant_id)
  ON CONFLICT (user_id, contest_code)
  DO UPDATE SET variant_id = _variant_id;
  PERFORM AcceptVariant(_user_id, _variant_id, _contest_code);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION AcceptVariant(_user_id INT, _variant_id INT, _contest_code TEXT)
RETURNS VOID AS $$
BEGIN
  INSERT INTO Contest.Attempt(user_id, task_id, variant_id, contest_code, status)
  SELECT _user_id, task_id, _variant_id, _contest_code, 'virgin'
  FROM Contest.TaskVariant
  WHERE variant_id = _variant_id
  ON CONFLICT DO NOTHING;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION StartAttemptTesting;
CREATE OR REPLACE FUNCTION StartAttemptTesting(_user_id INT, _task_id INT, _variant_id INT, _contest_code TEXT, _attempt_id TEXT, _attempt_text TEXT)
RETURNS VOID AS $$
  DELETE FROM Contest.GradingDetails WHERE attempt_id IN (
    SELECT attempt_id
    FROM Contest.Attempt
    WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id AND contest_code = _contest_code
  );

  UPDATE Contest.Attempt SET status = 'testing', testing_start_ts = NOW(), attempt_id = _attempt_id, attempt_text = _attempt_text
  WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id AND contest_code = _contest_code;
$$ LANGUAGE SQL;


