select count(*) from event.logevent where day='2016-02-03';


select * from event.logevent where publisherid=558545 and day='2016-02-03';

select * from event.rtblogevent where publisherid=558545 and day='2016-02-03';


select count(*) from event.logevent where publisherid=558360 and day='2016-02-03';


select servername,count(*) from event.logevent where publisherid=558360 and day='2016-02-05' group by servername;

select deflevelid,deflevelreason,clientimpression,serverimpression,count(*) from event.logevent where publisherid=558360 and day='2016-02-05' and servername='LGA-RTS00' group by deflevelid,deflevelreason,clientimpression,serverimpression;

select servername,serverchain,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where servername='LGA-RTB00' and publisherid=558360 and day='2016-02-05' 
group by servername,serverchain,deflevelid,deflevelreason,clientimpression,serverimpression;


select deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where adformatid=18 and day='2016-03-01' 
group by deflevelid,deflevelreason,clientimpression,serverimpression;


select publisherid,servername,serverchain,deflevelid,clientimpression,serverimpression,count(*) 
from event.logevent where adformatid=18 and day='2016-03-02' and hour in('01','02','03','04','05') and serverchain like '%LGA-RTS00%'
and servername like 'LGA-RTS00%'
group by publisherid,servername,serverchain,deflevelid,clientimpression,serverimpression;


-- Bids
select publisherid,servername,serverchain,deflevelid,clientimpression,serverimpression,count(*) 
from event.logevent where adformatid=18 and day='2016-03-02' and serverchain like '%LGA-RTS00%'
group by publisherid,servername,serverchain,deflevelid,clientimpression,serverimpression;

-- Won bids
select publisherid,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where adformatid=18 and day='2016-03-09'
and serverchain like 'LGA-TR%' and serverchain like '%LGA-RTS00%'
group by publisherid,deflevelid,deflevelreason,clientimpression,serverimpression;

-- Won bids from the whole cluster
select publisherid,servername,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where adformatid=18 and day='2016-03-02'
--and serverchain like 'LGA-TR%'
and deflevelid in (22,8)
group by publisherid,servername,deflevelid,deflevelreason,clientimpression,serverimpression;

-- Bids from the whole cluster
select publisherid,servername,deflevelid,sum(impressions) impressions, sum(serverimpression) serverimpression
, sum(clientimpression) clientimpression, sum(actions) actions, sum(clicks) clicks, count(*) count
from event.logevent where adformatid=18 and day='2016-03-09'
group by publisherid,servername,deflevelid;


-- Check totals
select publisherid,deflevelid,coalesce(if(locate('RTS',serverchain) > 0,'RTS','NTF'),'NTF')
, sum(impressions) impressions, sum(serverimpression) serverimpression
, sum(clientimpression) clientimpression, sum(actions) actions, sum(clicks) clicks, count(*) count
from event.logevent 
where adformatid=18 and day='2016-03-10' and deflevelid in (8,23)
group by publisherid,deflevelid,coalesce(if(locate('RTS',serverchain) > 0,'RTS','NTF'),'NTF');

-- Check totals no server
select publisherid, count(*) count, sum(impressions) impressions, sum(serverimpression) serverimpression
, sum(clientimpression) clientimpression, sum(actions) actions, sum(clicks) clicks
from event.logevent where adformatid=18 and day='2016-03-10'
group by publisherid
order by publisherid;


-- Passback
select publisherid,count(*) as count
from event.logevent where deflevelid=22 and adformatid=18 and day='2016-03-03'
--and serverchain like 'LGA-RTS00%'
and servername like 'LGA-RTS00%'
group by publisherid;

-- Passback reasons
select deflevelreason,count(*) as count
from event.logevent where deflevelid=22 and adformatid=18 and day='2016-03-02' and
--and serverchain like 'LGA-RTS00%'
and servername like 'LGA-RTS00%'
group by deflevelreason;

-- GumGum Bids
select servername,serverchain,deflevelid,deflevelreason,clientimpression,serverimpression,count(*) 
from event.logevent where adformatid=18 and day='2016-03-04' and serverchain like '%LGA-RTS00%' and publisherid=558355
group by servername,serverchain,deflevelid,deflevelreason,clientimpression,serverimpression;

-- GumGum native Won bids
select count(*) count
from event.logevent where adformatid=18 and day='2016-03-10'
--and serverchain like 'LGA-TR%' and serverchain like '%LGA-RTS00%'
and publisherid=559955 and deflevelid=8
;

/*
8/300/c1s0 - won bids
23/300/c0s1 - offered bids
22 - passback
*/

describe reference.publisher;


-- Check and compare data with vertica
select publisherid,deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
        from event.logevent
        where adformatid=18 and day ='2016-03-10' and hour='01'
        group by publisherid,deflevelid
;


select deflevelid,sum(clientimpression) clientimpression, sum(serverimpression) serverimpression
        from event.logevent
        where adformatid=18 and day ='2016-03-15' and hour='01'
        and advertiserid=558734
        group by 1
;




select day,impsrcid,
        sum(case when rtbeventid='BUYER_ERROR' or rtbeventid='BUYER_PASSBACK' then impressions else 0 end) as ppBidRequests,
        sum(case when rtbeventid='BUYER_BID' then impressions else 0 end) as dspBids
from event.rtblogevent 
where 
        day = '2016-03-18' and hour = '09'
        and buyerid = '558734' -- floor6
        and adformatid=18
        and rtbeventid in (
                'BUYER_ERROR', -- DSP->PP response error
                'BUYER_PASSBACK', -- DSP->PP response with no-bid
                'BUYER_BID' -- DSP->PP response
        )
group by 1,2
order by 1,2
;


select day,impsrcid,adformatid,rtbeventid,sum(impressions) impressions
from event.rtblogevent 
where 
        day = '2016-03-18' and hour = '00'
        and buyerid = '558734' -- floor6
        --and adformatid=18
group by 1,2,3,4
order by 1,2,3,4
;


select ,sum(impressions) impressions
from event.rtblogevent 
where 
        day = '2016-03-18' and hour = '00'
        and buyerid = '558734' -- floor6
        --and adformatid=18
group by 1,2,3,4
order by 1,2,3,4
;


describe event.rtblogevent ;

-- Check bundles
select AppBundle,count(*)
from raw.logevent
where publisherid=558362
and day = '2016-03-23' and hour = '01'
and ImpSrcId=2
group by 1
order by 2;


select
count(*) total, 
sum(case when AppBundle='' then 0 else 1 end) appbundle_p,
sum(case when AppBundle='' then 1 else 0 end) no_appbundle,
sum(case when AppStoreUrl='' then 0 else 1 end) appurl,
sum(case when AppStoreUrl='' then 1 else 0 end) no_appurl,
sum(case when PublisherDomain='' then 0 else 1 end) domain,
sum(case when PublisherDomain='' then 1 else 0 end) no_domain
from raw.logevent
where publisherid=558362
and day = '2016-03-23' and hour='01'
and ImpSrcId=2
order by 2;


select ContextualURL,count(*) from (
select distinct ContextualURL, thirdpartytagid
from event.logevent
where publisherid=559922
and day = '2016-03-23' and hour='01'
and ImpSrcId=2
) log
group by 1
order by 2 desc;


select thirdpartytagid, sum(serverimpression)
from event.logevent
where publisherid=559922
and day = '2016-03-23' and hour='01'
and ImpSrcId=2
group by 1;


-- floor6 stats hourly
select hour,sum(serverimpression) bids, sum(clientimpression) wins, 
sum(case when deflevelid = 8 then impressions else 0 end) wins_2,
cast(sum(impressions*winprice)/1000 as decimal(5,2)) cost,
cast(sum((case when deflevelid = 8 then impressions else 0 end)*winprice)/1000 as decimal(5,2)) cost_2
from event.logevent
where advertiserid=558734
and day = '2016-03-23'
--and ImpSrcId=2
and adformatid=18
group by 1
order by 1;




-- floor6 stats hourly
select hour,sum(serverimpression) bids, sum(clientimpression) wins, 
sum(case when deflevelid = 8 then impressions else 0 end) wins_2,
cast(sum(impressions*winprice)/1000 as decimal(5,2)) cost,
cast(sum((case when deflevelid = 8 then impressions else 0 end)*winprice)/1000 as decimal(5,2)) cost_2
from event.logevent
where advertiserid=558734
and day = '2016-03-23'
--and ImpSrcId=2
and adformatid=18
group by 1
order by 1;



-- Experiment on day functions
select cast(day as timestamp) + interval cast(hour as int) hour, cast(day as timestamp), cast(hour as timestamp), serverimpression, clientimpression, impressions, deflevelid
from event.logevent
where advertiserid=558734
and day = '2016-03-23' and hour='01'
--and ImpSrcId=2
and adformatid=18
limit 10;



-- Native stats compare
select  coalesce(ntf.dt, rts.dt) as dt,
        coalesce(ntf.publisherid, rts.publisherid) as pubid,
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
       cast(case when rts.total > 0 then coalesce(rts.filtered/rts.total, 0)*100 else -1 end as decimal(5,2)) as rts_filtered,
       cast((coalesce(rts.rev, 0)/(coalesce(ntf.rev, 0)+coalesce(rts.rev, 0))) * 100 as decimal(5,2)) as rts_rev_percentage,
       cast(coalesce((rts.total/(rts.total+ntf.total))*100, 0) as decimal(5,2)) as rts_supply_percentage,
        ma.accountname
from 
(select cast(day as timestamp) + interval cast(hour as int) hour as dt,
        publisherid,
        sum(serverimpression) as total,
        sum(case when deflevelid = 23 then serverimpression when deflevelid = 22 and deflevelreason = 120 then serverimpression else 0 end) as total_unfiltered,
        sum(case when deflevelid = 22 then serverimpression else 0 end) as passbacks,
        sum(case when deflevelid = 22 and not deflevelreason = 120 then serverimpression else 0 end) as filtered,
        sum(case when deflevelid = 23 then serverimpression else 0 end) as bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
        sum(impressions*winprice)/1000 as rev
from event.logevent
where day = '2016-03-28' and hour in ('10','11','12','13','14','15')
and adformatid = 18
and impsrcid = 2
-- RTS-triplelift, RTS-sharethrough, RTS-Adknowledge
and publisherid not in (558356,558357,559922)
group by 1,2
) rts
full join
(select cast(day as timestamp) + interval cast(hour as int) hour as dt, 
        publisherid,
        sum(serverimpression) as total,
        sum(case when deflevelid = 22 then serverimpression else 0 end) passbacks,
        sum(case when deflevelid = 23 then serverimpression else 0 end) bids,
        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
        sum(impressions*winprice)/1000 as rev
from event.logevent
where day = '2016-03-28' and hour in ('12','13','14')
and adformatid = 18
and impsrcid = 3
and publisherid not in (558356,558357,559922)
group by 1,2
) ntf
on rts.publisherid = ntf.publisherid
and rts.dt = ntf.dt
join reference.masteraccount ma
on ma.accountid = coalesce(ntf.publisherid, rts.publisherid)
--where ntf.win_rate > 0  or rts.win_rate > 0
order by 1,2 desc
;

