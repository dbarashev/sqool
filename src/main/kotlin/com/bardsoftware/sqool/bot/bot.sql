create table Student(tg_username text primary key, name text, id int generated by default as identity unique);
create table Team(
    sprint_num int check(sprint_num>=0) not null,
    team_num int check (team_num>0) not null,
    tg_username text not null references Student on update cascade,
    ord int check(ord>0) not null,
    unique (tg_username, sprint_num),
    unique (team_num, sprint_num, ord)
);
create view TeamDetails AS
    SELECT tg_username, id, name, sprint_num, team_num, ord
    FROM Student JOIN Team T using(tg_username);

create table Score(
    student_id int references Student(id),
    score numeric check (score between 0 and 10),
    sprint_num int,
    primary key(student_id, sprint_num)
);
create table ScoreDetails(
    tg_username_from text references Student,
    tg_username_to text references Student,
    sprint_num int,
    scoring_pos int,
    score numeric,
    foreign key(tg_username_to, sprint_num) references Team(tg_username, sprint_num) on update cascade,
    foreign key(tg_username_from, sprint_num) references Team(tg_username, sprint_num) on update cascade,
    primary key (tg_username_from, tg_username_to, sprint_num, scoring_pos)
);

alter table student add constraint unq_id unique(id);
create table TeacherScores(
  student_id int references Student(id),
  sprint_num int,
  score numeric,

  primary key(student_id, sprint_num)
);
create table DialogState(tg_id bigint primary key, state_id int, data text);

create view ScoreSummary as
    select id, name, tg_username, coalesce(sum(score), 0) as sum_score, coalesce(count(score), 1) as scored_sprints
    from student left join score on id=student_id
    group by id, name, tg_username;

-- select sum(score), count(score), tg_username_to, name, array_agg(tg_username_from || ': ' || score)
-- from score join student on score.tg_username_to=tg_username group by sprint_num, tg_username_to, name order by name;

CREATE OR REPLACE VIEW LastSprint AS
WITH T1 AS (
SELECT T.team_num / 100 AS uni, T.sprint_num
FROM Team T
)
SELECT uni, MAX(sprint_num) AS sprint_num FROM T1 GROUP BY uni;

-- create view PeerScores AS
-- select T1.sprint_num, t1.team_num, t1.ord AS ord_to, t1.tg_username AS tg_username_to, score, t.tg_username as tg_username_from, t.ord AS ord_from
-- from team t1 LEFT JOIN Score s on (tg_username_to=t1.tg_username and t1.sprint_num=s.sprint_num)
-- LEFT JOIN team t on (tg_username_from=t.tg_username and t.sprint_num=s.sprint_num)
-- where t1.tg_username != t.tg_username OR t.tg_username is null;


create or replace view TeamAndPeerScores AS
    select s.sprint_num, t.team_num, s.tg_username_from, t.ord, st.id, s.tg_username_to, t.name, s.scoring_pos, s.score
    from teamdetails t join scoredetails s on (tg_username=tg_username_to and t.sprint_num=s.sprint_num)
    join student st on (tg_username_to = st.tg_username)
    order by team_num, ord, scoring_pos;

--order by t1.sprint_num, t1.team_num, ord_to, ord_from;

-- select *
-- from team t1 LEFT JOIN Score s on (tg_username_to=t1.tg_username and t1.sprint_num=s.sprint_num)
-- LEFT JOIN team t on (tg_username_from=t.tg_username and t.sprint_num=s.sprint_num)
-- where t1.tg_username != IFNULL(t.tg_username, "")
--
--
-- select team_num, name from Team T JOIN Student S USING (tg_username) WHERE sprint_num = 0 ORDER BY team_num, ord
