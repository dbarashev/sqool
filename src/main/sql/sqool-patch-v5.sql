/*
 * Copyright (c) BarD Software s.r.o 2020
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

DROP FUNCTION contest.getorcreatecontestuser;
CREATE FUNCTION contest.getorcreatecontestuser(argname text, argpass text, argemail text, generatenick boolean) RETURNS TABLE(id integer, nick text, name text, passwd text, is_admin boolean, email text, code integer)
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
  FROM ContestUser WHERE ContestUser.email=argemail;
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
      INSERT INTO ContestUser (name, nick, passwd, is_admin, email) VALUES (argName, _nick, md5(argPass), COALESCE(_is_admin, FALSE), argemail) RETURNING ContestUser.id
  )
  SELECT T.id INTO _id FROM T;
  INSERT INTO UserContest(user_id, contest_code) SELECT _id, Contest.code FROM Contest;
  RETURN QUERY SELECT U.id, U.nick, U.name, U.passwd, U.is_admin, U.email, 0 AS code FROM ContestUser U WHERE U.name = argName;
  RETURN;
END;
$$;
