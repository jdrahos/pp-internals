select publisherid,servername,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where tagformatid=18 and day='2016-03-01' and hour='01'
group by publisherid,servername,deflevelid,deflevelreason,clientimpression,serverimpression;


select deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where tagformatid=18 and day='2016-03-01'  and hour='13'
group by deflevelid,deflevelreason,clientimpression,serverimpression;


-- Won bids from the whole cluster
select publisherid,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where tagformatid=18 and day='2016-03-09'
and serverchain like 'LGA-TR%'
group by publisherid,deflevelid,deflevelreason,clientimpression,serverimpression;
