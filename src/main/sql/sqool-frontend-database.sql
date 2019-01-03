DROP SCHEMA Contest CASCADE;
CREATE SCHEMA Contest;

SET search_path=Contest,public;

-----------------------------------------------------------------------------------------------------------------------
-- Tables for storing contest tasks, users and their submission attempts.
CREATE TABLE Contest.ContestUser(
  id SERIAL PRIMARY KEY,
  name TEXT UNIQUE,
  nick TEXT,
  passwd TEXT
);

CREATE TABLE Contest.Task(
    id INT PRIMARY KEY,
    name TEXT,
    real_name TEXT UNIQUE,
    signature TEXT,
    description TEXT,
    score INT CHECK(score BETWEEN 1 AND 10),
    difficulty INT CHECK(difficulty BETWEEN 1 AND 3),
    author_id INT REFERENCES Contest.ContestUser
);

CREATE TYPE AttemptStatus AS ENUM('success', 'failure', 'testing', 'virgin');
CREATE TABLE Contest.Attempt(
    task_id INT REFERENCES Contest.Task ON UPDATE CASCADE ON DELETE CASCADE,
    user_id INT REFERENCES Contest.ContestUser,
    attempt_id TEXT UNIQUE,
    status AttemptStatus DEFAULT 'failure',
    count INT DEFAULT 0,
    testing_start_ts TIMESTAMP,
    PRIMARY KEY(task_id, user_id));
CREATE TABLE Contest.GradingDetails(
    attempt_id TEXT REFERENCES Contest.Attempt(attempt_id) ON UPDATE CASCADE ON DELETE CASCADE,
    error_msg TEXT,
    result_set TEXT
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
RETURNS TABLE(id INT, nick TEXT, name TEXT, passwd TEXT, code INT) AS $$
DECLARE
    _id INT;
    _name TEXT;
    _passwd TEXT;
    _nick TEXT;
BEGIN
  SELECT ContestUser.id, ContestUser.name, ContestUser.nick, ContestUser.passwd INTO _id, _name, _nick, _passwd
  FROM ContestUser WHERE ContestUser.name=argName;
  IF FOUND THEN
      IF md5(argPass) <> _passwd THEN
        RETURN QUERY SELECT NULL::INT, NULL::TEXT, NULL::TEXT, NULL::TEXT, 1;
        RETURN;
      END IF;

      RETURN QUERY SELECT _id, _nick, _name, _passwd, 0;
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

  INSERT INTO ContestUser(name, nick, passwd) VALUES (argName, _nick, md5(argPass));
  RETURN QUERY SELECT U.id, U.nick, U.name, U.passwd, 0 AS code FROM ContestUser U WHERE U.name = argName;
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

-----------------------------------------------------------------------------------------------------------------------
-- Changes the status of the given _task_id to "testing" for the given user and records attempt identifier which
-- is opaque value.
CREATE OR REPLACE FUNCTION StartAttemptTesting(_user_id INT, _task_id INT, _attempt_id TEXT)
RETURNS VOID AS $$
  DELETE FROM Contest.GradingDetails WHERE attempt_id IN (
    SELECT attempt_id
    FROM Contest.Attempt
    WHERE user_id = _user_id AND task_id = _task_id
  );

  UPDATE Contest.Attempt SET status = 'testing', testing_start_ts = NOW(), attempt_id = _attempt_id
  WHERE user_id = _user_id AND task_id = _task_id;
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
        T.author_id,
        U.nick,
        A.attempt_id,
        A.user_id,
        A.status::TEXT,
        A.count,
        A.testing_start_ts,
        D.error_msg,
        D.result_set
FROM Contest.Task T
JOIN Contest.ContestUser U ON T.author_id = U.id
JOIN Contest.Attempt A ON A.task_id = T.id
LEFT JOIN Contest.GradingDetails D ON A.attempt_id = D.attempt_id;


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
    SELECT
        T.id, T.author_id, T.difficulty, T.score,
        SUM(CASE WHEN A.status = 'success' THEN 1 ELSE 0 END) AS solved, SUM(CASE WHEN A.count > 0 THEN 1 ELSE 0 END) AS attempted
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

create or replace view DisclosedLeaderboard AS
select  u.name,
        max(l.total_gain) as total_gain,
        max(solver_gain) as solver_gain,
        max(author_gain) as author_gain,
        array_agg(T.name)
from attempt a join task t on a.task_id=t.id
join contestuser u on u.id=a.user_id
join leaderboardview l on l.nick=u.nick
where status = 'success' group by u.id;

