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
select  coalesce(ntf.dt, rts.dt) as dt,
        coalesce(ntf.publisherid, rts.publisherid) as pubid,
        ma.accountname,
        coalesce(rts.total,0) as rts_total,
        coalesce(ntf.total,0) as ntf_total,
        coalesce(rts.filtered,0) as rts_filtered,
        coalesce(rts.passbacks,0) as rts_passbacks,
        coalesce(ntf.passbacks,0) as ntf_passbacks,
        coalesce(rts.total_unfiltered,0) as rts_unfiltered_total,
        coalesce(rts.bids,0) as rts_bids,
        coalesce(ntf.bids,0) as ntf_bids,
        coalesce(rts.wins,0) as rts_wins,
        coalesce(ntf.wins,0) as ntf_wins,
       cast(case when ntf.total > 0 then coalesce(ntf.bids/ntf.total, 0) else -1 end as decimal(5,2)) as ntf_bid_rate,
       cast(case when rts.total > 0 then coalesce(rts.bids/rts.total, 0) else -1 end as decimal(5,2)) as rts_bid_rate,
       cast(case when rts.total_unfiltered > 0 then coalesce(rts.bids/rts.total_unfiltered, 0) else -1 end as decimal(5,2)) as rts_unfiltered_bid_rate,
       cast(case when ntf.bids > 0 then coalesce(ntf.wins/ntf.bids, 0) else -1 end as decimal(5,2)) as ntf_win_rate,
       cast(case when rts.bids > 0 then coalesce(rts.wins/rts.bids, 0) else -1 end as decimal(5,2)) as rts_win_rate,
       cast(coalesce(ntf.rev, 0) as decimal(10,2)) as ntf_rev,
       cast(coalesce(rts.rev, 0) as decimal(10,2)) as rts_rev,
       cast((coalesce(rts.rev, 0)/(coalesce(ntf.rev, 0)+coalesce(rts.rev, 0))) * 100 as decimal(5,2)) as rts_rev_percentage,
       cast(coalesce((rts.total/(rts.total+ntf.total))*100, 0) as decimal(5,2)) as rts_supply_percentage
from 
(select date(dt) as dt,
        (case when publisherid = 559955 then 558355 else publisherid end) as publisherid,
        sum(serverimpression) as total,
        sum(case when deflevelid = 23 then serverimpression when deflevelid = 22 and deflevelreason = 120 then serverimpression else 0 end) as total_unfiltered,
        sum(case when deflevelid = 22 then serverimpression else 0 end) as passbacks,
        sum(case when deflevelid = 22 and not deflevelreason = 120 then serverimpression else 0 end) as filtered,
        sum(case when deflevelid = 23 then serverimpression else 0 end) as bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
        sum(impressions*winprice)/1000 as rev
from rpt
where dt > date('2016-02-01')
and adformatid = 18
and impsrcid = 2
-- RTS-triplelift, RTS-sharethrough, RTS-Adknowledge
and publisherid not in (558356,558357,559922)
-- My6Sens
and publisherid = 558602
group by 1,2
) rts
full join
(select date(dt) as dt, 
        publisherid,
        sum(serverimpression) as total,
        sum(case when deflevelid = 22 then serverimpression else 0 end) passbacks,
        sum(case when deflevelid = 23 then serverimpression else 0 end) bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
        sum(impressions*winprice)/1000 as rev
from rpt
where dt > date('2016-02-01')
and adformatid = 18
and impsrcid = 3
and publisherid not in (558356,558357,559922)
-- My6Sens
and publisherid = 558602
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


-- Check publisher statistics
select date(dt),
        sum(serverimpression) as bid_requests,
        sum(case when deflevelid = 23 then serverimpression else 0 end) as bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins
from public.Rpt
where dt >= date('2016-03-16') and publisherid=558356
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


-- check advertiser
select * from
(
select date(dt) date, advertiserid,
    sum(case when deflevelid=8 then clientimpression else 0 end) win,
    sum(case when deflevelid=23 then serverimpression else 0 end) offer,
    sum(case when deflevelid=22 then serverimpression else 0 end) passbacks
from public.Rpt
where --month(dt) = '03'
dt = date('2016-03-16')
--and advertiserid=558734
and adformatid=18
group by 1,2
order by 1,2
) rpt
join (select accountname, accountid from reference.masteraccount) ma
on rpt.advertiserid=ma.accountid
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