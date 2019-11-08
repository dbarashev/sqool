DROP VIEW Contest.ReviewByUser;

CREATE OR REPLACE VIEW ReviewByUser AS
SELECT S.attempt_id, S.solution_review, A.user_id, A.contest_code, A.variant_id, A.task_id, T.name AS task_name, S.reviewer_id, UR.name AS reviewer_name
FROM Contest.SolutionReview S
JOIN Contest.Attempt A ON S.attempt_id = A.attempt_id
JOIN Contest.Task T ON T.id = A.task_id
JOIN Contest.ContestUser UR ON S.reviewer_id = UR.id;
