select deflevelid,adformatid, sum(impressions), sum(clientimpression), cast(sum(impressions)/sum(clientimpression)*100 as decimal(5,2))
from rpt
where dt>date(now()-7) 
and publisherid=559922 -- Adknowledge
--and deflevelid=23
and clientimpression > 0
--and adformatid = 18
group by 1,2
order by 1,2
;

select timestampadd('HOUR', 5, now())