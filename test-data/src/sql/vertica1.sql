select count(*) from public.Rpt where AdFormatId = 18 and year(dt) = 2016 and month(dt)=2 and day(dt)=2;
select distinct PublisherId,advertiserid from public.Rpt where AdFormatId = 18 and year(dt) = 2016 and month(dt)=2 and day(dt)=2;
select * from public.Rpt where publisherId=558545 and year(dt)=2016;

select sum(serverimpression), sum(clientimpression) from public.Rpt where publisherId=558360 and year(dt)=2016;

select sum(impressions), sum(serverimpression), sum(clientimpression) from public.Rpt where publisherId=558360 and year(dt)=2015 and month(dt)=12;


select impressionsource,serverimpression,clientimpression,count(*) from public.WideCompact where publisherId=558360 and year(dt)=2016 and month(dt)=2 group by impressionsource,serverimpression,clientimpression;

select publisherid, deflevelid, sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where adformatid=18 and year(dt)=2016 and month(dt)=3 and day(dt)=9 and hour(dt) in (0,1,2,3,4,5)
group by publisherid,deflevelid;


select publisherid,deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where adformatid=18 and year(dt)=2016 and month(dt)=3 and day(dt)=9
group by publisherid,deflevelid;

-- Check totals
select publisherid,deflevelid,impsrcid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where adformatid=18 and year(dt)=2016 and month(dt)=3 and day(dt)=10
group by publisherid,deflevelid,impsrcid;


-- Check connatix
select deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where adformatid=18 and year(dt)=2016 and month(dt)=3 and day(dt)=10 and hour(dt)=0 and publisherid=558753
group by publisherid,deflevelid;


-- Check revenue - 558620
-- Check my6sense
select case impsrcid when 3 then 'ntf' when 2 then 'rts' else 'unknown' end,deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where publisherid=558602 and year(dt)=2016 and month(dt)=3 and day(dt)=10
group by impsrcid,deflevelid
order by 1,2;


-- Native stats
select  coalesce(ntf.dt, rts.dt) as dt,
        coalesce(ntf.publisherid, rts.publisherid) as pubid,
        ma.accountname,
        coalesce(rts.total,0) as rts_total,
        coalesce(ntf.total,0) as ntf_total,
       cast(coalesce(ntf.bid_rate, 0) as decimal(5,2)) as ntf_bid_rate,
       cast(coalesce(rts.bid_rate, 0) as decimal(5,2)) as rts_bid_rate,
       cast(coalesce(ntf.win_rate, 0) as decimal(5,2)) as ntf_win_rate,
       cast(coalesce(rts.win_rate, 0) as decimal(5,2)) as rts_win_rate,
       cast(coalesce(ntf.rev, 0) as decimal(10,2)) as ntf_rev,
       cast(coalesce(rts.rev, 0) as decimal(10,2)) as rts_rev,
       cast((coalesce(rts.rev, 0)/(coalesce(ntf.rev, 0)+coalesce(rts.rev, 0))) * 100 as decimal(5,2)) as rts_rev_percentage,
       cast(coalesce((rts.total/(rts.total+ntf.total))*100, 0) as decimal(5,2)) as rts_supply_percentage
from 
(select date(dt) as dt,
        (case when publisherid = 559955 then 558355 else publisherid end) as publisherid,
        sum(case when deflevelid = 23 then serverimpression when deflevelid = 22 and deflevelreason = 120 then serverimpression else 0 end) as total,
        sum(case when deflevelid = 22 and deflevelreason = 120 then serverimpression else 0 end) passbacks,
        sum(case when deflevelid = 22 and not deflevelreason = 120 then serverimpression else 0 end) filtered,
        sum(case when deflevelid = 23 then serverimpression else 0 end) bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
        sum(impressions*winprice)/1000 as rev
from rpt
where adformatid = 18
and impsrcid = 2
group by 1,2
) rts
full join
(select date(dt) as dt, 
        publisherid,
        sum(serverimpression) as total,
        sum(case when deflevelid = 22 then serverimpression else 0 end) passbacks,
        sum(case when deflevelid = 22 and deflevelreason = 120 then serverimpression else 0 end) exchange_passbacks,
        sum(case when deflevelid = 22 and not deflevelreason = 120 then serverimpression else 0 end) filtered,
        sum(case when deflevelid = 23 then serverimpression else 0 end) bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
        sum(impressions*winprice)/1000 as rev
from rpt
where adformatid = 18
and impsrcid = 3
group by 1,2
) ntf
on rts.publisherid = ntf.publisherid
and rts.dt = ntf.dt
join reference.masteraccount ma
on ma.accountid = coalesce(ntf.publisherid, rts.publisherid)
--where ntf.win_rate > 0  or rts.win_rate > 0
order by 1,2 desc
;

---- Floor 6
--select joined.rpt.*, joined.ma.accountname from
--(
select * from
(
select publisherid, deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where year(dt)=2016 and month(dt)=3 and advertiserid=558225
group by publisherid, deflevelid
) rpt
full outer join (
select accountid,accountname
from reference.masteraccount 
)ma
on ma.accountid = rpt.publisherid
--) joined
;

select publisherid, deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where year(dt)=2016 and month(dt)=3 and advertiserid=558225
group by publisherid, deflevelid
;

select deflevelid,impsrcid,sum(impressions) impressions, sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where year(dt)=2016 and month(dt)=3 and publisherid=558602
group by deflevelid,impsrcid
;

