select inventoryType, sum(serverimpression), sum(clientimpression)
from event.logevent
where day='2016-03-31' and hour='01' 
and publisherid=559922 -- Adknowledge
group by 1
order by 2,3
;

select * from event.logevent
where day='2016-03-31' and hour='01' 
and publisherid=559922 -- Adknowledge
and inventorytype=0
;