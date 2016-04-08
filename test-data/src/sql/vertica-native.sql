select ev.*,ma1.accountname pub,ma2.accountname adv
from (
        select  coalesce(ntf.dt, rts.dt) as dt,
                coalesce(ntf.publisherid, rts.publisherid) as pubid,
                coalesce(ntf.advertiserid, rts.advertiserid) as advid,
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
                        publisherid,
                        advertiserid,
                        sum(serverimpression) as total,
                        sum(case when deflevelid = 23 then serverimpression when deflevelid = 22 and deflevelreason = 120 then serverimpression else 0 end) as total_unfiltered,
                        sum(case when deflevelid = 22 then serverimpression else 0 end) as passbacks,
                        sum(case when deflevelid = 22 and not deflevelreason = 120 then serverimpression else 0 end) as filtered,
                        sum(case when deflevelid = 23 then serverimpression else 0 end) as bids,
                        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
                        sum(impressions*winprice)/1000 as rev
                from rpt
                where dt > date('2016-03-24')
                and adformatid = 18
                and impsrcid = 2
                group by 1,2,3
                ) rts
        full join
                (select date(dt) as dt,
                        publisherid,
                        advertiserid,
                        sum(serverimpression) as total,
                        sum(case when deflevelid = 22 then serverimpression else 0 end) passbacks,
                        sum(case when deflevelid = 23 then serverimpression else 0 end) bids,
                        sum(case when deflevelid = 8 then impressions else 0 end) as wins,
                        sum(impressions*winprice)/1000 as rev
                from rpt
                where dt > date('2016-03-24')
                and adformatid = 18
                and impsrcid = 3
                group by 1,2,3
                ) ntf
        on rts.publisherid = ntf.publisherid
        and rts.dt = ntf.dt
        and rts.advertiserid=ntf.advertiserid
) ev
join reference.masteraccount ma1
on ma1.accountid = ev.pubid
join reference.masteraccount ma2
on ma2.accountid = ev.advid
where ntf_rev>1
order by 1,2,3 desc
