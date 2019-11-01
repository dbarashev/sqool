DROP VIEW Contest.ReviewByUser;

CREATE OR REPLACE VIEW ReviewByUser AS
SELECT S.attempt_id, S.solution_review, A.user_id, A.contest_code, A.variant_id, A.task_id, T.name AS task_name
FROM Contest.SolutionReview S
JOIN Contest.Attempt A ON S.attempt_id = A.attempt_id
JOIN Contest.Task T ON T.id = A.task_id;
