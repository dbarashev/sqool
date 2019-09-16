-----------------------------------------------------------------------------------------------
-- Add some test data
INSERT INTO Contest.Script(description, body) VALUES
('Марсофлот', 'CREATE TABLE Planet(id INT, name TEXT)'),
('Пироги', 'CREATE TABLE Pie(id INT, name TEXT)');

INSERT INTO Contest.Contest(code, name, variant_choice) VALUES
  ('1', 'Random variant', 'RANDOM'),
  ('3', 'Any variant', 'ANY'),
  ('4', 'Not available', 'ANY'),
  ('5', 'Empty', 'ANY'),
  ('6', 'Single variant', 'ANY'),
  ('7', 'All variants', 'ANY'),
  ('8', 'Choose variant', 'ANY');

INSERT INTO Contest.Variant(id, name) VALUES
  (0, 'Variant 1'),
  (-1, 'Variant 2'),
  (-2, 'Variant 3'),
  (-3, 'Variant 4'),
  (-4, 'Variant 5');

INSERT INTO Contest.VariantContest(variant_id, contest_code)
SELECT V.id AS variant_id, C.code AS contest_code
FROM Contest.Contest C CROSS JOIN Contest.Variant V
WHERE C.name <> 'Empty' AND C.name <> 'Single variant';

INSERT INTO Contest.VariantContest(variant_id, contest_code) VALUES (-1, '6');

INSERT INTO Contest.ContestUser(id, name, nick, passwd, is_admin) VALUES (0, 'user', 'user', md5(''), TRUE);

INSERT INTO Contest.UserContest(user_id, contest_code, variant_id) VALUES
  (0, '1', NULL),
  (0, '3', -1),
  (0, '5', NULL),
  (0, '6', NULL),
  (0, '7', NULL),
  (0, '8', NULL);

INSERT INTO Contest.Task(id, name, author_id) VALUES
  (0, 'Solved', 0),
  (-1, 'Failed', 0),
  (-2, 'Testing', 0),
  (-3, 'Virgin', 0),
  (-4, 'From 5 variant', 0);

INSERT INTO Contest.TaskVariant(task_id, variant_id) VALUES
  (0, 0), (-1, 0), (-2, 0), (-3, 0), (-4, -4), (0, -1), (-1, -1), (-2, -1), (-3, -1);

INSERT INTO Contest.Attempt(task_id, user_id, variant_id, contest_code, attempt_id, status, count) VALUES
  (0, 0, -1, '3', '1', 'success', 1),
  (-1, 0, -1, '3', '2', 'failure', 1),
  (-2, 0, -1, '3', '3', 'testing', 1),
  (-3, 0, -1, '3', '4', 'virgin', 0);

INSERT INTO Contest.GradingDetails(attempt_id, error_msg, result_set) VALUES
  (2, E'Some error message\nSome error message', '[["col1", "col2", "col3"], {"col1": 42, "col2": "q"}, {"col1": -1, "col2": "t", "col3": 1}]');

INSERT INTO Contest.SolutionReview(attempt_id, solution_review, reviewer_id) VALUES ('1', 'review', 0);

SELECT AcceptVariant(0, 2, '3');