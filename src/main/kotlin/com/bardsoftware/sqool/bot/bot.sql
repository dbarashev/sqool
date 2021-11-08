create table Student(tg_username text primary key, name text);
create table Team(
    sprint_num int check(sprint_num>=0) not null,
    team_num int check (team_num>0) not null,
    tg_username text not null references Student on update cascade,
    unique (tg_username, sprint_num)
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

