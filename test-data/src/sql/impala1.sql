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