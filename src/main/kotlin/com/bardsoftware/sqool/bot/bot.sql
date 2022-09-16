create table Student(tg_username text primary key, name text);
create table Team(
    sprint_num int check(sprint_num>=0) not null,
    team_num int check (team_num>0) not null,
    tg_username text not null references Student on update cascade,
    ord int check(ord>0) not null,
    unique (tg_username, sprint_num),
    unique (team_num, sprint_num, ord)
);
create table Score(
    tg_username_from text references Student,
    tg_username_to text references Student,
    score numeric check (score between 0 and 1),
    sprint_num int,
    foreign key(tg_username_to, sprint_num) references Team(tg_username, sprint_num) on update cascade,
    primary key(tg_username_from, tg_username_to, sprint_num)
);
create table DialogState(tg_id bigint primary key, state_id int, data text);


select sum(score), count(score), tg_username_to, name, array_agg(tg_username_from || ': ' || score)
from score join student on tg_username_to=tg_username group by sprint_num, tg_username_to, name order by name;

CREATE OR REPLACE VIEW LastSprint AS
WITH T1 AS (
SELECT T.team_num / 100 AS uni, T.sprint_num
FROM Team T
)
SELECT uni, MAX(sprint_num) AS sprint_num FROM T1 GROUP BY uni;

create view PeerScores AS
select T1.sprint_num, t1.team_num, t1.ord AS ord_to, t1.tg_username AS tg_username_to, score, t.tg_username as tg_username_from, t.ord AS ord_from
from team t1 LEFT JOIN Score s on (tg_username_to=t1.tg_username and t1.sprint_num=s.sprint_num)
LEFT JOIN team t on (tg_username_from=t.tg_username and t.sprint_num=s.sprint_num)
where t1.tg_username != t.tg_username OR t.tg_username is null;
--order by t1.sprint_num, t1.team_num, ord_to, ord_from;

-- select *
-- from team t1 LEFT JOIN Score s on (tg_username_to=t1.tg_username and t1.sprint_num=s.sprint_num)
-- LEFT JOIN team t on (tg_username_from=t.tg_username and t.sprint_num=s.sprint_num)
-- where t1.tg_username != IFNULL(t.tg_username, "")
--
--
-- select team_num, name from Team T JOIN Student S USING (tg_username) WHERE sprint_num = 0 ORDER BY team_num, ord
