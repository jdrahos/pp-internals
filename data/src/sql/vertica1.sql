select count(*) from public.Rpt where AdFormatId = 18 and year(dt) = 2016 and month(dt)=2 and day(dt)=2;
select distinct PublisherId,advertiserid from public.Rpt where AdFormatId = 18 and year(dt) = 2016 and month(dt)=2 and day(dt)=2;
select * from public.Rpt where publisherId=558545 and year(dt)=2016;

select sum(serverimpression), sum(clientimpression) from public.Rpt where publisherId=558360 and year(dt)=2016;

select sum(impressions), sum(serverimpression), sum(clientimpression) from public.Rpt where publisherId=558360 and year(dt)=2015 and month(dt)=12;


select impressionsource,serverimpression,clientimpression,count(*) from public.WideCompact where publisherId=558360 and year(dt)=2016 and month(dt)=2 group by impressionsource,serverimpression,clientimpression
