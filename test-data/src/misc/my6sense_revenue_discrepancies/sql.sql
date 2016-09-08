-- Check all hourly
select accountname,publisherid, time_slice(dt, 1, 'HOUR'), 
    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(clientimpression*costcpm)/1000 rev_all,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 rev_ntf
from widecompact
join reference.masteraccount on masteraccount.accountid=widecompact.publisherid
where dt>=datetime '2016-06-02 10:00:00'
and publisherid=558360 -- Powerlinks
--and adformatid=18
--and publisherid=558602 -- My6sense
group by 1,2,3
order by 1,3
;



-- check revenue
select date(dt) date, advertiserid,
/*
    sum(serverimpression) bid_requests,
    sum(case when adformatid=18 then serverimpression else 0 end) requests_native,
    sum(case when impsrcid=2 then serverimpression else 0 end) requests_rts,
    sum(case when impsrcid=3 then serverimpression else 0 end) requests_ntf,
    sum(case when deflevelid=23 then serverimpression else 0 end) responses,
    sum(case when deflevelid=23 and adformatid=18 then serverimpression else 0 end) responses_native,
    sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end) responses_rts,
    sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end) responses_ntf,
    sum(case when deflevelid=22 then serverimpression else 0 end) passbacks,
    sum(case when deflevelid=8 then clientimpression else 0 end) win,
    sum(case when deflevelid=8 and adformatid=18 then clientimpression else 0 end) win_native,
    sum(case when deflevelid=8 and impsrcid=2 then clientimpression else 0 end) win_rts,
    sum(case when deflevelid=8 and impsrcid=3 then clientimpression else 0 end) win_ntf,
    sum(case when deflevelid=8 then clientimpression*costcpm else 0 end)/1000 pub_rev
*/    

    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(clientimpression*costcpm)/1000 rev_all,
    sum(case when deflevelid=8 then clientimpression*costcpm else 0 end)/1000 rev,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 cost_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 cost_ntf,
    sum(case when impsrcid=2 then clientimpression*revcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*revcpm else 0 end)/1000 rev_ntf
from public.Rpt
--join reference.masteraccount on masteraccount.accountid=rpt.advertiserid
where month(dt) = '04'
--dt = date('2016-04-21')
--and publisherid=558530 -- NativeAds
--and publisherid=558620 -- Revenue
and publisherid=558602 -- My6sense
--and adformatid=18
group by 1,2
order by 1
;




select * from rpt
where
dt=date('2016-06-05')
and publisherid=558602
and costcpm > 0
--limit 10
;




-- Check all
select accountname,publisherid,
    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(clientimpression*costcpm)/1000 rev_all,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 rev_ntf
from public.rpt
join reference.masteraccount on masteraccount.accountid=rpt.publisherid
where month(dt)='04'
and adformatid=18
group by 1,2
;


-- Check all hourly
select accountname,publisherid, time_slice(dt, 1, 'HOUR'), 
    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(clientimpression*costcpm)/1000 rev_all,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 rev_ntf
from widecompact
join reference.masteraccount on masteraccount.accountid=widecompact.publisherid
where dt>=datetime '2016-06-02 17:00:00'
and adformatid=18
--and publisherid=558602 -- My6sense
group by 1,2,3
order by 1,3
;


-- Check all hourly
select accountname,publisherid, time_slice(dt, 1, 'HOUR'), 
    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(clientimpression*costcpm)/1000 rev_all,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 rev_ntf
from widecompact
join reference.masteraccount on masteraccount.accountid=widecompact.publisherid
where dt>=datetime '2016-06-03 14:00:00'
--and publisherid=558360
and adformatid=18
--and publisherid=558602 -- My6sense
group by 1,2,3
order by 1,3
;

select distinct winPriceToken from RTSAccountDetails;


select date(dt) date, advertiserid,
/*
    sum(serverimpression) bid_requests,
    sum(case when adformatid=18 then serverimpression else 0 end) requests_native,
    sum(case when impsrcid=2 then serverimpression else 0 end) requests_rts,
    sum(case when impsrcid=3 then serverimpression else 0 end) requests_ntf,
    sum(case when deflevelid=23 then serverimpression else 0 end) responses,
    sum(case when deflevelid=23 and adformatid=18 then serverimpression else 0 end) responses_native,
    sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end) responses_rts,
    sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end) responses_ntf,
    sum(case when deflevelid=22 then serverimpression else 0 end) passbacks,
    sum(case when deflevelid=8 then clientimpression else 0 end) win,
    sum(case when deflevelid=8 and adformatid=18 then clientimpression else 0 end) win_native,
    sum(case when deflevelid=8 and impsrcid=2 then clientimpression else 0 end) win_rts,
    sum(case when deflevelid=8 and impsrcid=3 then clientimpression else 0 end) win_ntf,
    sum(case when deflevelid=8 then clientimpression*costcpm else 0 end)/1000 pub_rev
*/    

    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(clientimpression*costcpm)/1000 rev_all,
    sum(case when deflevelid=8 then clientimpression*costcpm else 0 end)/1000 rev,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 cost_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 cost_ntf,
    sum(case when impsrcid=2 then clientimpression*revcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*revcpm else 0 end)/1000 rev_ntf
from public.Rpt
--join reference.masteraccount on masteraccount.accountid=rpt.advertiserid
where month(dt) = '04'
--dt = date('2016-04-21')
--and publisherid=558530 -- NativeAds
--and publisherid=558620 -- Revenue
and publisherid=558602 -- My6sense
--and adformatid=18
group by 1,2
order by 1
;

select
month(date), advertiserid,publisherid,
sum(cost_ntf) cost_ntf,
sum(cost_ntf*percent_rts/percent_ntf) cost_rts,
sum(rev_ntf) rev_ntf,
sum(rev_ntf*percent_rts/percent_ntf) rev_rts
from
(
select date(dt) date, publisherid, advertiserid,
    100*sum(case when deflevelid=23 and impsrcid=2 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_rts,
    100*sum(case when deflevelid=23 and impsrcid=3 then serverimpression else 0 end)/sum(case when deflevelid=23 then serverimpression else 0 end) percent_ntf,
    sum(case when impsrcid=2 then clientimpression*costcpm else 0 end)/1000 cost_rts,
    sum(case when impsrcid=3 then clientimpression*costcpm else 0 end)/1000 cost_ntf,
    sum(case when impsrcid=2 then clientimpression*revcpm else 0 end)/1000 rev_rts,
    sum(case when impsrcid=3 then clientimpression*revcpm else 0 end)/1000 rev_ntf
from public.Rpt
--join reference.masteraccount on masteraccount.accountid=rpt.advertiserid
where month(dt) in ('06')
--dt = date('2016-04-21')
--and publisherid=558530 -- NativeAds
--and publisherid=558620 -- Revenue
--and publisherid=558602 -- My6sense
and publisherid in (558620,558602)
--and adformatid=18
group by 1,2,3
) src
where
percent_rts>0 and cost_ntf>0 and cost_rts=0
group by 1,2,3
;

