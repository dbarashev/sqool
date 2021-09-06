SET search_path=Contest,public;

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
        case when S.name = '' then S.email else S.name end AS user_name,
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

