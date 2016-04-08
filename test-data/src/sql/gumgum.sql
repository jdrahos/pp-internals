select dt,publisherdomain,
sum(impressions) impr,
/*
sum(case when impsrcid = 2 then impressions else 0 end) impr_rts,
sum(case when impsrcid = 3 then impressions else 0 end) impr_ntf,
*/
sum(case when adformatid = 18 then impressions else 0 end) impr_native,
sum(case when adformatid = 18 and impsrcid = 2 then impressions else 0 end) impr_rts_native,
sum(case when adformatid = 18 and impsrcid = 3 then impressions else 0 end) impr_ntf_native
/*
,
sum(impressions*winprice/1000) rev,
sum(case when impsrcid = 2 then impressions*winprice/1000 else 0 end) rev_rts,
sum(case when impsrcid = 3 then impressions*winprice/1000 else 0 end) rev_ntf,
sum(case when adformatid = 18 then impressions*winprice/1000 else 0 end) rev_native,
sum(case when adformatid = 18 and impsrcid = 2 then impressions*winprice/1000 else 0 end) rev_rts_native,
sum(case when adformatid = 18 and impsrcid = 3 then impressions*winprice/1000 else 0 end) rev_ntf_native
*/
from rpt
where publisherid=558355
and adformatid=18
and date(dt)='2016-03-23'
group by 1,2
having sum(impressions)>1
order by 6
;


select dt,advertiserid, 
sum(impressions) impr,
sum(case when impsrcid = 2 then impressions else 0 end) impr_rts,
sum(case when impsrcid = 3 then impressions else 0 end) impr_ntf,
sum(case when adformatid = 18 then impressions else 0 end) impr_native,
sum(case when adformatid = 18 and impsrcid = 2 then impressions else 0 end) impr_rts_native,
sum(case when adformatid = 18 and impsrcid = 3 then impressions else 0 end) impr_ntf_native
/*
,
sum(impressions*winprice/1000) rev,
sum(case when impsrcid = 2 then impressions*winprice/1000 else 0 end) rev_rts,
sum(case when impsrcid = 3 then impressions*winprice/1000 else 0 end) rev_ntf,
sum(case when adformatid = 18 then impressions*winprice/1000 else 0 end) rev_native,
sum(case when adformatid = 18 and impsrcid = 2 then impressions*winprice/1000 else 0 end) rev_rts_native,
sum(case when adformatid = 18 and impsrcid = 3 then impressions*winprice/1000 else 0 end) rev_ntf_native
*/
from rpt
where publisherid=558355
and date(dt)='2016-03-02'
group by 1,2
order by 3;


select dt, 
sum(impressions) impr,
sum(case when adformatid = 18 then impressions else 0 end) impr_native,
cast(sum(impressions*CostCPM/1000) as decimal(7,2)) rev,
cast(sum(case when adformatid = 18 then impressions*CostCPM/1000 else 0 end) as decimal(7,2)) rev_native
from rpt
where publisherid=558355
and date(dt)>date('2016-03-02')
group by 1
order by 1;

