select time_slice(dt, 1, 'HOUR'), 
sum(clientimpression),
sum(case when deflevelid=8 then clientimpression else 0 end) imp_ok
from widecompact
where advertiserid=558734 -- Floor6
and date(dt)=date('2016-03-31')
and adformatid=18
and clientimpression>0
group by 1
order by 1
;

-- Check deflevelreason for win
select deflevelreason,publisherid, 
sum(clientimpression)
from widecompact
where advertiserid=558734 -- Floor6
and date(dt)=date('2016-03-31')
and adformatid=18
and clientimpression>0
and deflevelid = 8
group by 1,2
order by 1,2
;

-- Check mobile for 559922 RTS-Adknowledge
