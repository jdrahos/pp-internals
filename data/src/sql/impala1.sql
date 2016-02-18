select count(*) from event.logevent where day='2016-02-03';


select * from event.logevent where publisherid=558545 and day='2016-02-03';

select * from event.rtblogevent where publisherid=558545 and day='2016-02-03';


select count(*) from event.logevent where publisherid=558360 and day='2016-02-03';


select servername,count(*) from event.logevent where publisherid=558360 and day='2016-02-05' group by servername;

select deflevelid,deflevelreason,clientimpression,serverimpression,count(*) from event.logevent where publisherid=558360 and day='2016-02-05' and servername='LGA-RTS00' group by deflevelid,deflevelreason,clientimpression,serverimpression;

select servername,serverchain,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) from event.logevent where servername='LGA-RTB00' and publisherid=558360 and day='2016-02-05' group by servername,serverchain,deflevelid,deflevelreason,clientimpression,serverimpression;
