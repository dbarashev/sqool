SET search_path=Contest,public;

DROP FUNCTION StartAttemptTesting;

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

CREATE OR REPLACE VIEW MyAttempts AS
SELECT  T.id AS task_id,
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
GROUP BY T.id, TR.task_id, A.user_id, A.task_id, A.variant_id, S.id, D.error_msg, D.result_set, U.id;
