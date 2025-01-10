[#ftl strip_whitespace=true]
{toc:printable=true}
[#outputformat "RTF"]
h2. Cloud Tech. stack per domain
[#list techStacks![]]
[#assign header = ["Domain"] + techNames + ["Other tech"]/]
[@md.tableHeader header/]
[#items as item]
|[@md.linkDomain (item.domain)!"unknown"/][#t]
|[#list techNames![] as v][=(item.techsList[v])!"-"]|[/#list][#t]
[#list (item.unknownTechs)![] as v][=v][#sep], [/#sep][/#list][#if (item.unknownTechs)??][#else]-[/#if]|
[/#items]
[#else]
No data.
[/#list]
[/#outputformat]
