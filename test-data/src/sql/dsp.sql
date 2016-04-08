-- Native stats
select  rpt.*,
       ma.accountname,
       cast(case when rpt.ntf_bids > 0 then rpt.ntf_wins/rpt.ntf_bids else 0 end as decimal(5,2)) as ntf_win_rate,
       cast(case when rpt.rts_bids > 0 then rpt.rts_wins/rpt.rts_bids else 0 end as decimal(5,2)) as rts_win_rate,
       cast(100*rpt.rts_rev/(rpt.ntf_rev+rpt.rts_rev) as decimal(5,2)) as rts_rev_percentage,
       cast(100*rpt.rts_total/(rpt.rts_total+rpt.ntf_total) as decimal(5,2)) as rts_supply_percentage
from 
(select date(dt) as dt,
        advertiserid,
        sum(case when impsrcid = 2 then serverimpression else 0 end) as rts_total,
        sum(case when impsrcid = 2 and deflevelid = 23 then serverimpression else 0 end) as rts_bids,
        sum(case when impsrcid = 2 and deflevelid = 8 then clientimpression else 0 end) as rts_wins,
        sum(case when impsrcid = 2 then impressions*winprice/1000 else 0 end) as rts_rev,
        sum(case when impsrcid = 3 then serverimpression else 0 end) as ntf_total,
        sum(case when impsrcid = 3 and deflevelid = 23 then serverimpression else 0 end) as ntf_bids,
        sum(case when impsrcid = 3 and deflevelid = 8 then clientimpression else 0 end) as ntf_wins,
        sum(case when impsrcid = 3 then impressions*winprice/1000 else 0 end) as ntf_rev
from rpt
where dt = date('2016-03-30')
and adformatid = 18
and advertiserid not in (558734) -- floor6
and publisherid not in (558753, 559921, 559922, 558356, 558357) -- Connatix, AdYouLike, Adknowledge, TripleLift, ShareThrough, 
group by 1,2
) rpt
join reference.masteraccount ma
on ma.accountid = rpt.advertiserid
where ntf_rev>0
order by dt asc, ntf_rev desc
;
