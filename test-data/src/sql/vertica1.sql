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
select date(dt), deflevelid,deflevelreason,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where adformatid=18 and publisherid=558753
group by 1,2,3
order by 1,2,3;


-- Check revenue - 558620
-- Check my6sense
select case impsrcid when 3 then 'ntf' when 2 then 'rts' else 'unknown' end,deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where publisherid=558602 and year(dt)=2016 and month(dt)=3 and day(dt)=10
group by impsrcid,deflevelid
order by 1,2;



-- Native stats
select  rpt.*,
       ma.accountname,
       cast(case when rpt.rts_total+rpt.rts_filtered > 0 then 100*rpt.rts_total/(rpt.rts_total+rpt.rts_filtered) else 0 end as decimal(5,2)) as rts_filter_rate,
       cast(case when rpt.ntf_total > 0 then 100*rpt.ntf_bids/rpt.ntf_total else 0 end as decimal(5,2)) as ntf_bid_rate,
       cast(case when rpt.rts_total > 0 then 100*rpt.rts_bids/rpt.rts_total else 0 end as decimal(5,2)) as rts_bid_rate,
       cast(case when rpt.ntf_bids > 0 then rpt.ntf_wins/rpt.ntf_bids else 0 end as decimal(5,2)) as ntf_win_rate,
       cast(case when rpt.rts_bids > 0 then rpt.rts_wins/rpt.rts_bids else 0 end as decimal(5,2)) as rts_win_rate,
       cast(100*rpt.rts_rev/(rpt.ntf_rev+rpt.rts_rev) as decimal(5,2)) as rts_rev_percentage,
       cast(100*rpt.rts_total/(rpt.rts_total+rpt.ntf_total) as decimal(5,2)) as rts_supply_percentage
from 
(select date(dt) as dt,
        publisherid,
        sum(case when impsrcid = 2 and (deflevelid = 23 or (deflevelid = 22 and deflevelreason = 120)) then serverimpression else 0 end) as rts_total,
        sum(case when impsrcid = 2 and deflevelid = 22 and deflevelreason=120 then serverimpression else 0 end) as rts_passbacks,
        sum(case when impsrcid = 2 and deflevelid = 22 and not deflevelreason = 120 then serverimpression else 0 end) as rts_filtered,
        sum(case when impsrcid = 2 and deflevelid = 23 then serverimpression else 0 end) as rts_bids,
        sum(case when impsrcid = 2 and deflevelid = 8 then clientimpression else 0 end) as rts_wins,
        sum(case when impsrcid = 2 then impressions*winprice/1000 else 0 end) as rts_rev,
        sum(case when impsrcid = 3 then serverimpression else 0 end) as ntf_total,
        sum(case when impsrcid = 3 and deflevelid = 22 then serverimpression else 0 end) as ntf_passbacks,
        sum(case when impsrcid = 3 and deflevelid = 23 then serverimpression else 0 end) as ntf_bids,
        sum(case when impsrcid = 3 and deflevelid = 8 then clientimpression else 0 end) as ntf_wins,
        sum(case when impsrcid = 3 then impressions*winprice/1000 else 0 end) as ntf_rev
from rpt
where dt = date('2016-03-29')
and adformatid = 18
and advertiserid not in (558734) -- floor6
group by 1,2
) rpt
join reference.masteraccount ma
on ma.accountid = rpt.publisherid
where ntf_rev>0
order by dt asc, ntf_rev desc
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


-- Check publisher statistics
-- 558356 - TripleLift
-- AdYouLike/559921
select date(dt),
        sum(serverimpression) as bid_requests,
        sum(case when deflevelid = 23 then serverimpression else 0 end) as bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins
from public.Rpt
where dt >= date('2016-03-15') and publisherid=559921
group by 1
order by 1
;

select deflevelid,,sum(impressions) impressions, sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
from public.Rpt
where year(dt)=2016 and month(dt)=3 and impsrcid=2 
and publisherid=558362
and deflevelreason=2600
group by deflevelid,deflevelreason
;


select publisherdomain,
sum(serverimpression) impression
from public.Rpt
where year(dt)=2016 and month(dt)=3 and impsrcid=2 
and publisherid=558362
and deflevelreason=2600
group by publisherdomain
order by 2
;


-- Check who bid and who win on m6s
select dt,impsrcid,advertiserid,deflevelid,impression,accountname from (
select * from (
select date(dt) dt,impsrcid,advertiserid,deflevelid,sum(serverimpression+clientimpression) impression
from public.rpt
where publisherid=559921
and dt > date('2016-03-15')
--and adformatid=18
group by 1,2,3,4
) rpt
join reference.masteraccount
on rpt.advertiserid=masteraccount.accountid
) rpt
order by dt,impsrcid

;


-- check advertiser
select deflevelid,sum(impressions) impressions, sum(clientimpression) clientimpression, sum(serverimpression) serverimpression, sum(impressions*winprice) cost,
    sum(case when impsrcid = 2 then serverimpression else 0 end) serv_rts,
    sum(case when impsrcid = 3 then serverimpression else 0 end) serv_ntf,
    sum(case when impsrcid = 2 and not deflevelreason = 120 then 0 else serverimpression end) serv_filtr
from public.Rpt
where month(dt) = '03'
--dt = date('2016-03-15')
and advertiserid=558734
--and adformatid=18
group by 1
;


-- check publisher
select date(dt) date,
    sum(serverimpression) bid_requests,
    sum(case when deflevelid=23 then serverimpression else 0 end) bid_responses,
    sum(case when deflevelid=22 then serverimpression else 0 end) passbacks,
    sum(case when deflevelid=8 then clientimpression else 0 end) win
from public.Rpt
where --month(dt) = '03'
dt >= date('2016-03-20')
and publisherid=559922 -- Adknowledge
and adformatid=18
group by 1
order by 1
;


-- Check advertiser bids
select * from
(
select date(dt) date, buyerid,dealid,reqsrctype,
        sum(impressions)
from rtb.rtbsummarydaily
where --month(dt) = '03'
day = date('2016-03-16')
--and advertiserid=558734
and adformatid=18
group by 1,2,3,4
order by 1,2
) rtb
join (select accountname, accountid from reference.masteraccount) ma
on rtb.buyerid=ma.accountid
;

select * 
from rtb.rtbsummarydaily
--where --month(dt) = '03'
--day = date('2016-03-16')
--limit 100
;

select coalesce(publisherdomain, '-'), coalesce(rootdomain, '-'), sum(clientimpression+serverimpression)
from public.rpt
where publisherid=558362
and dt=date('2016-03-23')
group by 1,2
;

rollback;
SET SESSION AUTOCOMMIT TO ON;

-- Check tags for Adknowledge (559922)
select dt,sum(serverimpression) offered_bids, 
        sum(case when deflevelid = 22 then serverimpression else 0 end) passbacks,
        sum(case when deflevelid = 23 then serverimpression else 0 end) bids,
        sum(case when deflevelid=8 then clientimpression else 0 end) wins,
        sum(clientimpression) wins_all
from rpt
where publisherid=559922
and dt>=date('2016-03-15') 
group by 1
order by 1;

