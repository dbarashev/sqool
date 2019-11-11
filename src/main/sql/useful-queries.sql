select attempt_id, user_id, U.name, testing_start_ts 
from attempthistory h join contestuser u on h.user_id=u.id 
where contest_code='cw_20191029' and u.uni='HSE' and variant_id=12 order by 
testing_start_ts;
