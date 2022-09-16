SET search_path=Contest,public;

DROP FUNCTION StartAttemptTesting;

-- Added _attempt_text argument
CREATE OR REPLACE FUNCTION StartAttemptTesting(_user_id INT, _task_id INT, _variant_id INT, _attempt_id TEXT, _attempt_text TEXT)
    RETURNS VOID AS $$
DELETE FROM Contest.GradingDetails WHERE attempt_id IN (
    SELECT attempt_id
    FROM Contest.Attempt
    WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id
);

UPDATE Contest.Attempt SET status = 'testing', testing_start_ts = NOW(), attempt_id = _attempt_id, attempt_text = _attempt_text
WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id;
$$ LANGUAGE SQL;

DROP VIEW MyAttempts CASCADE;

-- Added schema_id attribute
CREATE OR REPLACE VIEW MyAttempts AS
SELECT  T.id AS task_id,
        T.script_id AS schema_id,
        T.name,
        T.difficulty,
        T.score,
        T.description,
        CASE WHEN TR.task_id IS NULL THEN '[]'
             ELSE json_agg(json_object('{name, type, num}', ARRAY[col_name, col_type, col_num::TEXT]))::TEXT
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
GROUP BY T.id, TR.task_id, A.user_id, A.task_id, A.variant_id, A.attempt_id, A.status, A.count, A.testing_start_ts, S.id, D.error_msg, D.result_set, U.id;

-- This view just needs to be re-created as is because it was dependent on MyAttempts
CREATE OR REPLACE VIEW AttemptsByContest AS
SELECT C.contest_code, A.*
FROM Contest.VariantContest C
JOIN Contest.MyAttempts A ON C.variant_id = A.variant_id;

-- This view just needs to be re-created as is because it was dependent on MyAttempts
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

-- This view just needs to be re-created as is because it was dependent on MyAttempts
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
