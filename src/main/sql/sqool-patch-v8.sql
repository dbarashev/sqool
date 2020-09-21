CREATE OR REPLACE FUNCTION RecordAttemptResult(_attemptId TEXT, _success BOOLEAN, _errorMsg TEXT, _resultLines TEXT)
    RETURNS VOID AS $$
UPDATE Contest.Attempt
SET status = (CASE _success WHEN true THEN 'success'::AttemptStatus ELSE 'failure'::AttemptStatus END),
    count = count+1
WHERE attempt_id = _attemptId;

INSERT INTO AttemptHistory(attempt_id, task_id, variant_id, contest_code, user_id, attempt_text, testing_start_ts)
SELECT attempt_id, task_id, variant_id, contest_code, user_id, attempt_text, testing_start_ts
FROM Attempt
WHERE attempt_id = _attemptId
ON CONFLICT(attempt_id) DO UPDATE
    SET task_id = EXCLUDED.task_id,
        variant_id=EXCLUDED.variant_id,
        contest_code=EXCLUDED.contest_code,
        user_id=EXCLUDED.user_id,
        attempt_text=EXCLUDED.attempt_text,
        testing_start_ts=EXCLUDED.testing_start_ts;

INSERT INTO Contest.GradingDetails(attempt_id, error_msg, result_set) VALUES (_attemptId, _errorMsg, _resultLines);
$$ LANGUAGE SQL;


ALTER TABLE contest.usercontest add column is_forced BOOLEAN DEFAULT false;

CREATE OR REPLACE VIEW availablecontests AS
SELECT uc.user_id,
       uc.contest_code,
       c.name AS contest_name,
       c.variant_choice,
       uc.variant_id AS assigned_variant_id,
       (json_agg(json_object('{id,name}'::text[], ARRAY[(v.id)::text, v.name])))::text AS variants_json_array
FROM (((contest.usercontest uc
    JOIN contest.contest c ON ((uc.contest_code = c.code)))
    JOIN contest.variantcontest vc ON ((c.code = vc.contest_code)))
    JOIN contest.variant v ON ((vc.variant_id = v.id)))
WHERE c.dates @> now() OR uc.is_forced OR uc.variant_id is not null
GROUP BY c.code, uc.user_id, uc.contest_code;
