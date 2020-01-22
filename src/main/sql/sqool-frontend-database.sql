--
-- PostgreSQL database dump
--

-- Dumped from database version 11.5 (Debian 11.5-1.pgdg90+1)
-- Dumped by pg_dump version 12.1 (Debian 12.1-1.pgdg100+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: contest; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA contest;


ALTER SCHEMA contest OWNER TO postgres;

--
-- Name: attemptstatus; Type: TYPE; Schema: contest; Owner: postgres
--

CREATE TYPE contest.attemptstatus AS ENUM (
    'success',
    'failure',
    'testing',
    'virgin'
);


ALTER TYPE contest.attemptstatus OWNER TO postgres;

--
-- Name: variantchoice; Type: TYPE; Schema: contest; Owner: postgres
--

CREATE TYPE contest.variantchoice AS ENUM (
    'RANDOM',
    'ALL',
    'ANY'
);


ALTER TYPE contest.variantchoice OWNER TO postgres;

--
-- Name: acceptrandomauthor(integer); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.acceptrandomauthor(_user_id integer) RETURNS void
    LANGUAGE sql
    AS $$
-- INSERT INTO Contest.Attempt(user_id, task_id, status)
-- SELECT _user_id, T.id, 'virgin'::AttemptStatus
-- FROM Contest.Task T JOIN (
--   SELECT (0.5 + random()*2)::INT + (SELECT MIN(id) FROM ContestUser)- 1
-- ) AS R(author_id) using (author_id)
$$;


ALTER FUNCTION contest.acceptrandomauthor(_user_id integer) OWNER TO postgres;

--
-- Name: acceptvariant(integer, integer); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.acceptvariant(_user_id integer, _variant_id integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  INSERT INTO Contest.Attempt(user_id, task_id, variant_id, status)
  SELECT _user_id, task_id, _variant_id, 'virgin' FROM Contest.TaskVariant
  WHERE variant_id = _variant_id
  ON CONFLICT DO NOTHING;
END;
$$;


ALTER FUNCTION contest.acceptvariant(_user_id integer, _variant_id integer) OWNER TO postgres;

--
-- Name: acceptvariant(integer, integer, text); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.acceptvariant(_user_id integer, _variant_id integer, _contest_code text) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  INSERT INTO Contest.Attempt(user_id, task_id, variant_id, contest_code, status)
  SELECT _user_id, task_id, _variant_id, _contest_code, 'virgin'
  FROM Contest.TaskVariant
  WHERE variant_id = _variant_id
  ON CONFLICT DO NOTHING;
END;
$$;


ALTER FUNCTION contest.acceptvariant(_user_id integer, _variant_id integer, _contest_code text) OWNER TO postgres;

--
-- Name: addtask(integer, text, text, text, text, integer, integer, text); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.addtask(_id integer, _name text, _real_name text, _args text, _description text, _score integer, _difficulty integer, _author_name text) RETURNS void
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION contest.addtask(_id integer, _name text, _real_name text, _args text, _description text, _score integer, _difficulty integer, _author_name text) OWNER TO postgres;

--
-- Name: assignvariant(integer, text, integer); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.assignvariant(_user_id integer, _contest_code text, _variant_id integer) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  INSERT INTO Contest.UserContest(user_id, contest_code, variant_id) VALUES (_user_id, _contest_code, _variant_id)
  ON CONFLICT (user_id, contest_code)
  DO UPDATE SET variant_id = _variant_id;
  PERFORM AcceptVariant(_user_id, _variant_id, _contest_code);
END;
$$;


ALTER FUNCTION contest.assignvariant(_user_id integer, _contest_code text, _variant_id integer) OWNER TO postgres;

--
-- Name: contestdto_insert(); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.contestdto_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION contest.contestdto_insert() OWNER TO postgres;

--
-- Name: contestdto_update(); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.contestdto_update() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION contest.contestdto_update() OWNER TO postgres;

--
-- Name: gennickname(); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.gennickname() RETURNS text
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION contest.gennickname() OWNER TO postgres;

--
-- Name: getorcreatecontestuser(text, text, boolean); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.getorcreatecontestuser(argname text, argpass text, generatenick boolean) RETURNS TABLE(id integer, nick text, name text, passwd text, is_admin boolean, email text, code integer)
    LANGUAGE plpgsql
    AS $$
DECLARE
  _id INT;
  _name TEXT;
  _passwd TEXT;
  _nick TEXT;
  _is_admin BOOLEAN;
  _email TEXT;
BEGIN
  SELECT ContestUser.id, ContestUser.name, ContestUser.nick, ContestUser.passwd, ContestUser.is_admin, ContestUser.email
  INTO _id, _name, _nick, _passwd, _is_admin, _email
  FROM ContestUser WHERE ContestUser.name=argName;
  IF FOUND THEN
    IF md5(argPass) <> _passwd THEN
      RETURN QUERY SELECT NULL::INT, NULL::TEXT, NULL::TEXT, NULL::TEXT, NULL::BOOLEAN, NULL::TEXT, 1;
      RETURN;
    END IF;

    RETURN QUERY SELECT _id, _nick, _name, _passwd, _is_admin, _email, 0;
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
  RETURN QUERY SELECT U.id, U.nick, U.name, U.passwd, U.is_admin, U.email, 0 AS code FROM ContestUser U WHERE U.name = argName;
  RETURN;
END;
$$;


ALTER FUNCTION contest.getorcreatecontestuser(argname text, argpass text, generatenick boolean) OWNER TO postgres;

--
-- Name: makeattempt(integer, integer); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.makeattempt(_user_id integer, _task_id integer) RETURNS void
    LANGUAGE sql
    AS $$
  INSERT INTO Contest.Attempt(user_id, task_id, status) VALUES (_user_id, _task_id, 'virgin');
$$;


ALTER FUNCTION contest.makeattempt(_user_id integer, _task_id integer) OWNER TO postgres;

--
-- Name: recordattemptresult(text, boolean, text, text); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.recordattemptresult(_attemptid text, _success boolean, _errormsg text, _resultlines text) RETURNS void
    LANGUAGE sql
    AS $$
    UPDATE Contest.Attempt
    SET status = (CASE _success WHEN true THEN 'success'::AttemptStatus ELSE 'failure'::AttemptStatus END),
        count = count+1
    WHERE attempt_id = _attemptId;

    INSERT INTO AttemptHistory(attempt_id, task_id, variant_id, contest_code, user_id, attempt_text, testing_start_ts)
    SELECT attempt_id, task_id, variant_id, contest_code, user_id, attempt_text, testing_start_ts
    FROM Attempt
    WHERE attempt_id = _attemptId;

    INSERT INTO Contest.GradingDetails(attempt_id, error_msg, result_set) VALUES (_attemptId, _errorMsg, _resultLines);
$$;


ALTER FUNCTION contest.recordattemptresult(_attemptid text, _success boolean, _errormsg text, _resultlines text) OWNER TO postgres;

--
-- Name: startattempttesting(integer, integer, integer, text, text, text); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.startattempttesting(_user_id integer, _task_id integer, _variant_id integer, _contest_code text, _attempt_id text, _attempt_text text) RETURNS void
    LANGUAGE sql
    AS $$
  DELETE FROM Contest.GradingDetails WHERE attempt_id IN (
    SELECT attempt_id
    FROM Contest.Attempt
    WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id AND contest_code = _contest_code
  );

  UPDATE Contest.Attempt SET status = 'testing', testing_start_ts = NOW(), attempt_id = _attempt_id, attempt_text = _attempt_text
  WHERE user_id = _user_id AND task_id = _task_id AND variant_id = _variant_id AND contest_code = _contest_code;
$$;


ALTER FUNCTION contest.startattempttesting(_user_id integer, _task_id integer, _variant_id integer, _contest_code text, _attempt_id text, _attempt_text text) OWNER TO postgres;

--
-- Name: taskdto_insert(); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.taskdto_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  new_task_id INT;
BEGIN
  IF NEW.id IS NULL THEN
    WITH T AS (
      INSERT INTO Contest.Task(name, real_name, description, has_result, solution, script_id, author_id)
        VALUES (NEW.name, NEW.name, NEW.description, NEW.has_result, NEW.solution, NEW.script_id, NEW.author_id)
        RETURNING id
    )
    SELECT id INTO new_task_id
    FROM T;
  ELSE
    UPDATE Contest.Task
    SET name = NEW.name,
        real_name = NEW.name,
        description = NEW.description,
        has_result = NEW.has_result,
        solution = NEW.solution,
        script_id = NEW.script_id
    WHERE id = NEW.id;
    SELECT NEW.id INTO new_task_id;
  END IF;

  DELETE FROM Contest.TaskResult WHERE task_id = new_task_id;
  IF NEW.has_result THEN
      WITH T AS (
        SELECT new_task_id AS task_id, X.*
        FROM json_to_recordset(NEW.result_json::JSON) AS X(col_num INT, col_name TEXT, col_type TEXT)
      )
      INSERT INTO Contest.TaskResult(task_id, col_num, col_name, col_type)
      SELECT task_id, col_num, col_name, col_type
      FROM T;
  END IF;
RETURN NEW;
end;
$$;


ALTER FUNCTION contest.taskdto_insert() OWNER TO postgres;

--
-- Name: variantdto_insertupdate(); Type: FUNCTION; Schema: contest; Owner: postgres
--

CREATE FUNCTION contest.variantdto_insertupdate() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION contest.variantdto_insertupdate() OWNER TO postgres;

SET default_tablespace = '';

--
-- Name: attempt; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.attempt (
    task_id integer NOT NULL,
    variant_id integer NOT NULL,
    user_id integer NOT NULL,
    attempt_id text,
    status contest.attemptstatus DEFAULT 'failure'::contest.attemptstatus,
    attempt_text text,
    count integer DEFAULT 0,
    testing_start_ts timestamp without time zone,
    contest_code text NOT NULL
);


ALTER TABLE contest.attempt OWNER TO postgres;

--
-- Name: attempthistory; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.attempthistory (
    attempt_id text NOT NULL,
    task_id integer,
    variant_id integer,
    contest_code text,
    user_id integer,
    status contest.attemptstatus DEFAULT 'failure'::contest.attemptstatus,
    attempt_text text,
    testing_start_ts timestamp without time zone
);


ALTER TABLE contest.attempthistory OWNER TO postgres;

--
-- Name: availablecontests; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.availablecontests AS
SELECT
    NULL::integer AS user_id,
    NULL::text AS contest_code,
    NULL::text AS contest_name,
    NULL::contest.variantchoice AS variant_choice,
    NULL::integer AS assigned_variant_id,
    NULL::text AS variants_json_array;


ALTER TABLE contest.availablecontests OWNER TO postgres;

--
-- Name: contest; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.contest (
    code text NOT NULL,
    name text NOT NULL,
    dates tstzrange DEFAULT tstzrange(now(), (now() + '01:00:00'::interval)) NOT NULL,
    variant_choice contest.variantchoice DEFAULT 'ANY'::contest.variantchoice NOT NULL
);


ALTER TABLE contest.contest OWNER TO postgres;

--
-- Name: contestdto; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.contestdto AS
SELECT
    NULL::text AS code,
    NULL::text AS name,
    NULL::timestamp with time zone AS start_ts,
    NULL::timestamp with time zone AS end_ts,
    NULL::contest.variantchoice AS variant_choice,
    NULL::text AS variants_id_json_array;


ALTER TABLE contest.contestdto OWNER TO postgres;

--
-- Name: contestuser; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.contestuser (
    id integer NOT NULL,
    name text,
    nick text,
    passwd text,
    is_admin boolean DEFAULT false NOT NULL,
    uni text,
    email text
);


ALTER TABLE contest.contestuser OWNER TO postgres;

--
-- Name: contestuser_id_seq; Type: SEQUENCE; Schema: contest; Owner: postgres
--

CREATE SEQUENCE contest.contestuser_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE contest.contestuser_id_seq OWNER TO postgres;

--
-- Name: contestuser_id_seq; Type: SEQUENCE OWNED BY; Schema: contest; Owner: postgres
--

ALTER SEQUENCE contest.contestuser_id_seq OWNED BY contest.contestuser.id;


--
-- Name: disclosedleaderboard; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.disclosedleaderboard AS
SELECT
    NULL::text AS name,
    NULL::numeric AS total_gain,
    NULL::numeric AS solver_gain,
    NULL::numeric AS author_gain,
    NULL::text[] AS array_agg;


ALTER TABLE contest.disclosedleaderboard OWNER TO postgres;

--
-- Name: gainpertask; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.gainpertask AS
SELECT
    NULL::integer AS id,
    NULL::integer AS author_id,
    NULL::integer AS difficulty,
    NULL::integer AS score,
    NULL::bigint AS solved,
    NULL::bigint AS attempted,
    NULL::numeric AS gain;


ALTER TABLE contest.gainpertask OWNER TO postgres;

--
-- Name: gradingdetails; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.gradingdetails (
    attempt_id text NOT NULL,
    error_msg text,
    result_set text
);


ALTER TABLE contest.gradingdetails OWNER TO postgres;

--
-- Name: leaderboardview; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.leaderboardview AS
SELECT
    NULL::text AS nick,
    NULL::numeric AS total_gain,
    NULL::numeric AS solver_gain,
    NULL::numeric AS author_gain;


ALTER TABLE contest.leaderboardview OWNER TO postgres;

--
-- Name: myattempts; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.myattempts AS
SELECT
    NULL::integer AS task_id,
    NULL::integer AS schema_id,
    NULL::text AS name,
    NULL::integer AS difficulty,
    NULL::integer AS score,
    NULL::text AS description,
    NULL::text AS signature,
    NULL::integer AS author_id,
    NULL::text AS author_nick,
    NULL::text AS attempt_id,
    NULL::integer AS user_id,
    NULL::integer AS variant_id,
    NULL::text AS contest_code,
    NULL::text AS user_nick,
    NULL::text AS user_name,
    NULL::text AS user_uni,
    NULL::text AS status,
    NULL::integer AS count,
    NULL::timestamp without time zone AS testing_start_ts,
    NULL::text AS error_msg,
    NULL::text AS result_set;


ALTER TABLE contest.myattempts OWNER TO postgres;

--
-- Name: solutionreview; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.solutionreview (
    attempt_id text NOT NULL,
    reviewer_id integer NOT NULL,
    solution_review text
);


ALTER TABLE contest.solutionreview OWNER TO postgres;

--
-- Name: task; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.task (
    id integer NOT NULL,
    name text,
    real_name text,
    description text,
    solution text,
    score integer DEFAULT 1,
    difficulty integer DEFAULT 1,
    author_id integer,
    script_id integer,
    has_result boolean DEFAULT true NOT NULL,
    CONSTRAINT task_difficulty_check CHECK (((difficulty >= 1) AND (difficulty <= 3))),
    CONSTRAINT task_score_check CHECK (((score >= 1) AND (score <= 10)))
);


ALTER TABLE contest.task OWNER TO postgres;

--
-- Name: reviewbyuser; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.reviewbyuser AS
 SELECT s.attempt_id,
    s.solution_review,
    a.user_id,
    a.contest_code,
    a.variant_id,
    a.task_id,
    t.name AS task_name,
    s.reviewer_id,
    ur.name AS reviewer_name
   FROM (((contest.solutionreview s
     JOIN contest.attempt a ON ((s.attempt_id = a.attempt_id)))
     JOIN contest.task t ON ((t.id = a.task_id)))
     JOIN contest.contestuser ur ON ((s.reviewer_id = ur.id)));


ALTER TABLE contest.reviewbyuser OWNER TO postgres;

--
-- Name: script; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.script (
    id integer NOT NULL,
    description text,
    body text
);


ALTER TABLE contest.script OWNER TO postgres;

--
-- Name: script_id_seq; Type: SEQUENCE; Schema: contest; Owner: postgres
--

ALTER TABLE contest.script ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME contest.script_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: scriptdto; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.scriptdto AS
 SELECT script.id,
    script.description,
    script.body
   FROM contest.script;


ALTER TABLE contest.scriptdto OWNER TO postgres;

--
-- Name: task_id_seq; Type: SEQUENCE; Schema: contest; Owner: postgres
--

ALTER TABLE contest.task ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME contest.task_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: taskvariant; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.taskvariant (
    task_id integer NOT NULL,
    variant_id integer NOT NULL
);


ALTER TABLE contest.taskvariant OWNER TO postgres;

--
-- Name: variantcontest; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.variantcontest (
    variant_id integer NOT NULL,
    contest_code text NOT NULL
);


ALTER TABLE contest.variantcontest OWNER TO postgres;

--
-- Name: taskcontest; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.taskcontest AS
 SELECT c.code AS contest_code,
    v.variant_id,
    t.task_id
   FROM ((contest.contest c
     JOIN contest.variantcontest v ON ((c.code = v.contest_code)))
     JOIN contest.taskvariant t ON ((v.variant_id = t.variant_id)));


ALTER TABLE contest.taskcontest OWNER TO postgres;

--
-- Name: taskdto; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.taskdto AS
SELECT
    NULL::integer AS id,
    NULL::text AS name,
    NULL::integer AS script_id,
    NULL::integer AS author_id,
    NULL::text AS description,
    NULL::text AS solution,
    NULL::boolean AS has_result,
    NULL::text AS result_json;


ALTER TABLE contest.taskdto OWNER TO postgres;

--
-- Name: taskresult; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.taskresult (
    task_id integer NOT NULL,
    col_num integer NOT NULL,
    col_name text NOT NULL,
    col_type text DEFAULT 'TEXT'::text NOT NULL,
    CONSTRAINT taskresult_check CHECK ((((col_num > 0) AND (col_name <> ''::text)) OR ((col_num = 0) AND (col_name = ''::text))))
);


ALTER TABLE contest.taskresult OWNER TO postgres;

--
-- Name: tasksbyauthor; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.tasksbyauthor AS
SELECT
    NULL::integer AS id,
    NULL::text AS nick,
    NULL::bigint AS count1,
    NULL::numeric AS gain1,
    NULL::bigint AS count2,
    NULL::numeric AS gain2,
    NULL::bigint AS count3,
    NULL::numeric AS gain3,
    NULL::numeric AS total_gain;


ALTER TABLE contest.tasksbyauthor OWNER TO postgres;

--
-- Name: tasksubmissionsstats; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.tasksubmissionsstats AS
SELECT
    NULL::integer AS task_id,
    NULL::text AS task_name,
    NULL::text AS contest_code,
    NULL::bigint AS solved,
    NULL::bigint AS attempted;


ALTER TABLE contest.tasksubmissionsstats OWNER TO postgres;

--
-- Name: usercontest; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.usercontest (
    user_id integer NOT NULL,
    contest_code text NOT NULL,
    variant_id integer
);


ALTER TABLE contest.usercontest OWNER TO postgres;

--
-- Name: variant; Type: TABLE; Schema: contest; Owner: postgres
--

CREATE TABLE contest.variant (
    id integer NOT NULL,
    name text NOT NULL
);


ALTER TABLE contest.variant OWNER TO postgres;

--
-- Name: variant_id_seq; Type: SEQUENCE; Schema: contest; Owner: postgres
--

ALTER TABLE contest.variant ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME contest.variant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: variantdto; Type: VIEW; Schema: contest; Owner: postgres
--

CREATE VIEW contest.variantdto AS
SELECT
    NULL::integer AS id,
    NULL::text AS name,
    NULL::text AS tasks_id_json_array,
    NULL::text AS scripts_id_json_array;


ALTER TABLE contest.variantdto OWNER TO postgres;

--
-- Name: contestuser id; Type: DEFAULT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.contestuser ALTER COLUMN id SET DEFAULT nextval('contest.contestuser_id_seq'::regclass);


--
-- Name: attempt attempt_attempt_id_key; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_attempt_id_key UNIQUE (attempt_id);


--
-- Name: attempt attempt_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_pkey PRIMARY KEY (task_id, user_id, variant_id, contest_code);


--
-- Name: attempthistory attempthistory_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempthistory
    ADD CONSTRAINT attempthistory_pkey PRIMARY KEY (attempt_id);


--
-- Name: contest contest_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.contest
    ADD CONSTRAINT contest_pkey PRIMARY KEY (code);


--
-- Name: contestuser contestuser_email_key; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.contestuser
    ADD CONSTRAINT contestuser_email_key UNIQUE (email);


--
-- Name: contestuser contestuser_name_key; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.contestuser
    ADD CONSTRAINT contestuser_name_key UNIQUE (name);


--
-- Name: contestuser contestuser_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.contestuser
    ADD CONSTRAINT contestuser_pkey PRIMARY KEY (id);


--
-- Name: script script_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.script
    ADD CONSTRAINT script_pkey PRIMARY KEY (id);


--
-- Name: solutionreview solutionreview_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.solutionreview
    ADD CONSTRAINT solutionreview_pkey PRIMARY KEY (attempt_id, reviewer_id);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- Name: task task_real_name_key; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.task
    ADD CONSTRAINT task_real_name_key UNIQUE (real_name);


--
-- Name: taskresult taskresult_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.taskresult
    ADD CONSTRAINT taskresult_pkey PRIMARY KEY (task_id, col_num);


--
-- Name: taskvariant taskvariant_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.taskvariant
    ADD CONSTRAINT taskvariant_pkey PRIMARY KEY (task_id, variant_id);


--
-- Name: usercontest usercontest_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.usercontest
    ADD CONSTRAINT usercontest_pkey PRIMARY KEY (user_id, contest_code);


--
-- Name: variant variant_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.variant
    ADD CONSTRAINT variant_pkey PRIMARY KEY (id);


--
-- Name: variantcontest variantcontest_pkey; Type: CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.variantcontest
    ADD CONSTRAINT variantcontest_pkey PRIMARY KEY (variant_id, contest_code);


--
-- Name: contestdto _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.contestdto AS
 SELECT c.code,
    c.name,
    lower(c.dates) AS start_ts,
    upper(c.dates) AS end_ts,
    c.variant_choice,
    (array_to_json(array_agg(v.variant_id)))::text AS variants_id_json_array
   FROM (contest.contest c
     JOIN contest.variantcontest v ON ((c.code = v.contest_code)))
  GROUP BY c.code
UNION ALL
 SELECT c.code,
    c.name,
    lower(c.dates) AS start_ts,
    upper(c.dates) AS end_ts,
    c.variant_choice,
    '[]'::text AS variants_id_json_array
   FROM (contest.contest c
     LEFT JOIN contest.variantcontest v ON ((c.code = v.contest_code)))
  WHERE (v.contest_code IS NULL)
  GROUP BY c.code;


--
-- Name: variantdto _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.variantdto AS
 SELECT v.id,
    v.name,
    (array_to_json(array_agg(tv.task_id)))::text AS tasks_id_json_array,
    (COALESCE(json_agg(DISTINCT t.script_id) FILTER (WHERE (t.script_id IS NOT NULL)), '[]'::json))::text AS scripts_id_json_array
   FROM (((contest.variant v
     JOIN contest.taskvariant tv ON ((v.id = tv.variant_id)))
     JOIN contest.task t ON ((tv.task_id = t.id)))
     LEFT JOIN contest.script s ON ((t.script_id = s.id)))
  GROUP BY v.id
UNION ALL
 SELECT v.id,
    v.name,
    '[]'::text AS tasks_id_json_array,
    '[]'::text AS scripts_id_json_array
   FROM (contest.variant v
     LEFT JOIN contest.taskvariant t ON ((v.id = t.variant_id)))
  WHERE (t.variant_id IS NULL)
  GROUP BY v.id;


--
-- Name: availablecontests _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.availablecontests AS
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
  GROUP BY c.code, uc.user_id, uc.contest_code;


--
-- Name: tasksubmissionsstats _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.tasksubmissionsstats AS
 SELECT t.id AS task_id,
    t.name AS task_name,
    a.contest_code,
    sum(
        CASE
            WHEN (a.status = 'success'::contest.attemptstatus) THEN 1
            ELSE 0
        END) AS solved,
    sum(
        CASE
            WHEN (a.count > 0) THEN 1
            ELSE 0
        END) AS attempted
   FROM (contest.attempt a
     JOIN contest.task t ON ((a.task_id = t.id)))
  GROUP BY t.id, a.contest_code;


--
-- Name: gainpertask _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.gainpertask AS
 WITH solvedcounts AS (
         SELECT t.id,
            t.author_id,
            t.difficulty,
            t.score,
            sum(
                CASE
                    WHEN (a.status = 'success'::contest.attemptstatus) THEN 1
                    ELSE 0
                END) AS solved,
            sum(
                CASE
                    WHEN (a.count > 0) THEN 1
                    ELSE 0
                END) AS attempted
           FROM (contest.task t
             LEFT JOIN contest.attempt a ON ((a.task_id = t.id)))
          GROUP BY t.id
        )
 SELECT solvedcounts.id,
    solvedcounts.author_id,
    solvedcounts.difficulty,
    solvedcounts.score,
    solvedcounts.solved,
    solvedcounts.attempted,
        CASE
            WHEN (solvedcounts.attempted > 0) THEN ((solvedcounts.score)::numeric(4,2) / ((solvedcounts.solved + 1))::numeric)
            ELSE (0)::numeric
        END AS gain
   FROM solvedcounts;


--
-- Name: tasksbyauthor _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.tasksbyauthor AS
 SELECT a.id,
    a.nick,
    sum(
        CASE
            WHEN (t.difficulty = 1) THEN 1
            ELSE 0
        END) AS count1,
    sum(
        CASE
            WHEN (t.difficulty = 1) THEN t.gain
            ELSE (0)::numeric
        END) AS gain1,
    sum(
        CASE
            WHEN (t.difficulty = 2) THEN 1
            ELSE 0
        END) AS count2,
    sum(
        CASE
            WHEN (t.difficulty = 2) THEN t.gain
            ELSE (0)::numeric
        END) AS gain2,
    sum(
        CASE
            WHEN (t.difficulty = 3) THEN 1
            ELSE 0
        END) AS count3,
    sum(
        CASE
            WHEN (t.difficulty = 3) THEN t.gain
            ELSE (0)::numeric
        END) AS gain3,
    sum(t.gain) AS total_gain
   FROM (contest.contestuser a
     JOIN contest.gainpertask t ON ((t.author_id = a.id)))
  GROUP BY a.id;


--
-- Name: myattempts _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.myattempts AS
 SELECT t.id AS task_id,
    t.script_id AS schema_id,
    t.name,
    t.difficulty,
    t.score,
    t.description,
        CASE
            WHEN (tr.task_id IS NULL) THEN '[]'::text
            ELSE (json_agg(json_object('{name,type}'::text[], ARRAY[tr.col_name, tr.col_type])))::text
        END AS signature,
    t.author_id,
    u.nick AS author_nick,
    a.attempt_id,
    a.user_id,
    a.variant_id,
    a.contest_code,
    s.nick AS user_nick,
    s.name AS user_name,
    s.uni AS user_uni,
    (a.status)::text AS status,
    a.count,
    a.testing_start_ts,
    d.error_msg,
    d.result_set
   FROM (((((contest.task t
     JOIN contest.contestuser u ON ((t.author_id = u.id)))
     JOIN contest.attempt a ON ((a.task_id = t.id)))
     JOIN contest.contestuser s ON ((a.user_id = s.id)))
     LEFT JOIN contest.gradingdetails d ON ((a.attempt_id = d.attempt_id)))
     LEFT JOIN contest.taskresult tr ON ((tr.task_id = t.id)))
  GROUP BY t.id, tr.task_id, a.user_id, a.task_id, a.variant_id, a.contest_code, s.id, d.error_msg, d.result_set, u.id;


--
-- Name: leaderboardview _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.leaderboardview AS
 WITH solvedcounts AS (
         SELECT t.id,
            t.author_id,
            t.difficulty,
            t.score,
            sum(
                CASE
                    WHEN (a.status = 'success'::contest.attemptstatus) THEN 1
                    ELSE 0
                END) AS solved,
            sum(
                CASE
                    WHEN (a.count > 0) THEN 1
                    ELSE 0
                END) AS attempted
           FROM (contest.task t
             JOIN contest.attempt a ON ((a.task_id = t.id)))
          GROUP BY t.id
        ), authoredtaskgain AS (
         SELECT solvedcounts.id,
            solvedcounts.author_id,
            solvedcounts.difficulty,
            solvedcounts.score,
            solvedcounts.solved,
            solvedcounts.attempted,
                CASE
                    WHEN (solvedcounts.attempted > 0) THEN ((solvedcounts.score)::numeric(4,2) / ((solvedcounts.solved + 1))::numeric)
                    ELSE (0)::numeric
                END AS total_gain
           FROM solvedcounts
        ), solvedtaskgain AS (
         SELECT a.user_id,
            sum(g_1.total_gain) AS total_gain
           FROM (contest.myattempts a
             JOIN authoredtaskgain g_1 ON ((a.task_id = g_1.id)))
          WHERE (a.status = 'success'::text)
          GROUP BY a.user_id
        ), sumgain AS (
         SELECT COALESCE(s.user_id, a.author_id) AS user_id,
            COALESCE(s.total_gain, (0)::numeric) AS solver_gain,
            COALESCE(a.total_gain, (0)::numeric) AS author_gain,
            (COALESCE(s.total_gain, (0)::numeric) + COALESCE(a.total_gain, (0)::numeric)) AS total_gain
           FROM (solvedtaskgain s
             FULL JOIN authoredtaskgain a ON ((s.user_id = a.author_id)))
        )
 SELECT u.nick,
    sum(COALESCE(g.total_gain, (0)::numeric)) AS total_gain,
    sum(COALESCE(g.solver_gain, (0)::numeric)) AS solver_gain,
    sum(COALESCE(g.author_gain, (0)::numeric)) AS author_gain
   FROM (contest.contestuser u
     LEFT JOIN sumgain g ON ((u.id = g.user_id)))
  GROUP BY u.id;


--
-- Name: disclosedleaderboard _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.disclosedleaderboard AS
 SELECT u.name,
    max(l.total_gain) AS total_gain,
    max(l.solver_gain) AS solver_gain,
    max(l.author_gain) AS author_gain,
    array_agg(t.name) AS array_agg
   FROM (((contest.attempt a
     JOIN contest.task t ON ((a.task_id = t.id)))
     JOIN contest.contestuser u ON ((u.id = a.user_id)))
     JOIN contest.leaderboardview l ON ((l.nick = u.nick)))
  WHERE (a.status = 'success'::contest.attemptstatus)
  GROUP BY u.id;


--
-- Name: taskdto _RETURN; Type: RULE; Schema: contest; Owner: postgres
--

CREATE OR REPLACE VIEW contest.taskdto AS
 SELECT t.id,
    t.name,
    t.script_id,
    t.author_id,
    COALESCE(t.description, ''::text) AS description,
    COALESCE(t.solution, ''::text) AS solution,
    t.has_result,
    (array_to_json(array_agg(json_object('{name,type,num}'::text[], ARRAY[r.col_name, r.col_type, (r.col_num)::text]))))::text AS result_json
   FROM (contest.task t
     JOIN contest.taskresult r ON ((t.id = r.task_id)))
  GROUP BY t.id
UNION ALL
 SELECT t.id,
    t.name,
    t.script_id,
    t.author_id,
    COALESCE(t.description, ''::text) AS description,
    COALESCE(t.solution, ''::text) AS solution,
    t.has_result,
    '[]'::text AS result_json
   FROM (contest.task t
     LEFT JOIN contest.taskresult r ON ((t.id = r.task_id)))
  WHERE (r.task_id IS NULL)
  GROUP BY t.id;


--
-- Name: contestdto contestdto_insert_trigger; Type: TRIGGER; Schema: contest; Owner: postgres
--

CREATE TRIGGER contestdto_insert_trigger INSTEAD OF INSERT ON contest.contestdto FOR EACH ROW EXECUTE PROCEDURE contest.contestdto_insert();


--
-- Name: contestdto contestdto_update_trigger; Type: TRIGGER; Schema: contest; Owner: postgres
--

CREATE TRIGGER contestdto_update_trigger INSTEAD OF UPDATE ON contest.contestdto FOR EACH ROW EXECUTE PROCEDURE contest.contestdto_update();


--
-- Name: taskdto taskdto_insert_trigger; Type: TRIGGER; Schema: contest; Owner: postgres
--

CREATE TRIGGER taskdto_insert_trigger INSTEAD OF INSERT ON contest.taskdto FOR EACH ROW EXECUTE PROCEDURE contest.taskdto_insert();


--
-- Name: taskdto taskdto_update_trigger; Type: TRIGGER; Schema: contest; Owner: postgres
--

CREATE TRIGGER taskdto_update_trigger INSTEAD OF UPDATE ON contest.taskdto FOR EACH ROW EXECUTE PROCEDURE contest.taskdto_insert();


--
-- Name: variantdto variantdto_insert_trigger; Type: TRIGGER; Schema: contest; Owner: postgres
--

CREATE TRIGGER variantdto_insert_trigger INSTEAD OF INSERT ON contest.variantdto FOR EACH ROW EXECUTE PROCEDURE contest.variantdto_insertupdate();


--
-- Name: variantdto variantdto_update_trigger; Type: TRIGGER; Schema: contest; Owner: postgres
--

CREATE TRIGGER variantdto_update_trigger INSTEAD OF UPDATE ON contest.variantdto FOR EACH ROW EXECUTE PROCEDURE contest.variantdto_insertupdate();


--
-- Name: attempt attempt_contest_code_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_contest_code_fkey FOREIGN KEY (contest_code) REFERENCES contest.contest(code) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: attempt attempt_task_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_task_id_fkey FOREIGN KEY (task_id) REFERENCES contest.task(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: attempt attempt_task_id_fkey1; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_task_id_fkey1 FOREIGN KEY (task_id, variant_id) REFERENCES contest.taskvariant(task_id, variant_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: attempt attempt_user_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_user_id_fkey FOREIGN KEY (user_id) REFERENCES contest.contestuser(id);


--
-- Name: attempt attempt_variant_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.attempt
    ADD CONSTRAINT attempt_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES contest.variant(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: gradingdetails gradingdetails_attempt_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.gradingdetails
    ADD CONSTRAINT gradingdetails_attempt_id_fkey FOREIGN KEY (attempt_id) REFERENCES contest.attempt(attempt_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: solutionreview solutionreview_attempt_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.solutionreview
    ADD CONSTRAINT solutionreview_attempt_id_fkey FOREIGN KEY (attempt_id) REFERENCES contest.attempt(attempt_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: solutionreview solutionreview_reviewer_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.solutionreview
    ADD CONSTRAINT solutionreview_reviewer_id_fkey FOREIGN KEY (reviewer_id) REFERENCES contest.contestuser(id);


--
-- Name: task task_author_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.task
    ADD CONSTRAINT task_author_id_fkey FOREIGN KEY (author_id) REFERENCES contest.contestuser(id);


--
-- Name: task task_script_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.task
    ADD CONSTRAINT task_script_id_fkey FOREIGN KEY (script_id) REFERENCES contest.script(id);


--
-- Name: taskresult taskresult_task_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.taskresult
    ADD CONSTRAINT taskresult_task_id_fkey FOREIGN KEY (task_id) REFERENCES contest.task(id);


--
-- Name: taskvariant taskvariant_task_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.taskvariant
    ADD CONSTRAINT taskvariant_task_id_fkey FOREIGN KEY (task_id) REFERENCES contest.task(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: taskvariant taskvariant_variant_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.taskvariant
    ADD CONSTRAINT taskvariant_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES contest.variant(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: usercontest usercontest_contest_code_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.usercontest
    ADD CONSTRAINT usercontest_contest_code_fkey FOREIGN KEY (contest_code) REFERENCES contest.contest(code) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: usercontest usercontest_contest_code_fkey1; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.usercontest
    ADD CONSTRAINT usercontest_contest_code_fkey1 FOREIGN KEY (contest_code, variant_id) REFERENCES contest.variantcontest(contest_code, variant_id);


--
-- Name: usercontest usercontest_user_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.usercontest
    ADD CONSTRAINT usercontest_user_id_fkey FOREIGN KEY (user_id) REFERENCES contest.contestuser(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: usercontest usercontest_variant_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.usercontest
    ADD CONSTRAINT usercontest_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES contest.variant(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: variantcontest variantcontest_contest_code_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.variantcontest
    ADD CONSTRAINT variantcontest_contest_code_fkey FOREIGN KEY (contest_code) REFERENCES contest.contest(code) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: variantcontest variantcontest_variant_id_fkey; Type: FK CONSTRAINT; Schema: contest; Owner: postgres
--

ALTER TABLE ONLY contest.variantcontest
    ADD CONSTRAINT variantcontest_variant_id_fkey FOREIGN KEY (variant_id) REFERENCES contest.variant(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

