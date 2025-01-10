[#ftl strip_whitespace=true]
{toc:printable=true}
[#outputformat "RTF"]

h2. Dependencies diagram
h3. Domain specific schema
[#if encodedDiagramGeneralDomain??]
![=encodedDiagramGeneralDomain]!
[#else]N/A[/#if]

h3.Domain components schema
[#if encodedDiagramDomain??]
![=encodedDiagramDomain]!
[#else]N/A[/#if]

h2. Components list
[#list components![]]
[@md.tableHeader ["#", "Type", "Abbreviation", "Name", "Owner", "DNS", "Used by", "Description"]/]
[#items as entry]
|[=entry?counter][#t]
|[=(entry.C.type)!"missed"][#t]
|[=(entry.C.abbreviation)!"missed"][#t]
|[@md.linkComponent domain.id, entry.C.name/][#t]
|[=(entry.C.details.owner)!"missed"][#t]
|[=(entry.C.details.dnsName)!"missed"][#t]
|[=(entry.T)!"unknown"][#t]
|[=(entry.C.details.description)!"missed"]|
[/#items]
[#else ]
Domain is empty and have no registered microservices.
[/#list]

h2. Used Tech.stack
[#list (techStack![])?sort as tech]
* [=tech]
[/#list]

h2. Used Tech. stack by services
[#list techStackByService![]]
[@md.tableHeader ["Abbreviation", "Name", "API spec published", "Used technologies"]/]
[#items as component]
|[=(component.abbreviation)!"missed"][#t]
|[@md.linkComponent domain.id, component.name/][#t]
|[#list (component.apiSpec)![] as apiSpec][@md.link apiSpec/][#sep], [/#sep][#else]not published[/#list][#t]
|[#list (component.usedTech)!["no"] as tech][=tech][#sep], [/#sep][#else ]no[/#list]|
[/#items]
[#else]
Domain is empty and have no registered microservices.
[/#list]

h2. TMF spec. support
[#list (tmfSpecs![])]
[@md.tableHeader ["TMF Spec.", "Spec. version", "Specification name", "Abbreviation", "Microservice", "Microservice Owner"]/]
[#items as entry]
|[=(entry.S.code)!"missed"][#t]
|[=(entry.S.version)!"missed"][#t]
|[@md.link entry.S.name, entry.S.url/][#t]
|[=entry.C.abbreviation][#t]
|[@md.linkComponent domain.id, entry.C.name/][#t]
|[@md.owner (entry.C.details.owner)!"unknown"/]|
[/#items]
[#else]
No TMF specifications implemented.
[/#list]

h2. Blue-green ready
[#list (blueGreenReady![])?sort_by("name")]
[@md.tableHeader ["Abbreviation", "Microservice", "Blue-Green deployment"]/]
[#items as item]
|[=(item.abbreviation)!"missed"]|[@md.linkComponent domain.id, item.name/]|not supported|
[/#items]
[#else]
All microservices already supported Blue-Green deployment or not required it.
[/#list]
[#--
[#list blueGreenReady![] as item]
[#list item]
||*Abbreviation*||*Microservice*|||*Not supported Blue-Green deployment*||
[#items as key, value]
|[=(key.abbreviation)!"missed"]|[=(key.name)!"missed"]|[#list value![] as m][=m][#sep], [/#sep][/#list]|
[/#items]
[#else]
All microservices already supported Blue-Green deployment or not required it.
[/#list]
[/#list]
--]

h2. Fault Tolerance support
[#list (faultToleranceSupport![]) as item]
[@md.tableHeader ["Abbreviation", "Microservice", "Not supported fault-tolerance features"]/]
[#list item as key, value]
|[=(key.abbreviation)!"missed"]|[@md.linkComponent domain.id, key.name/]|[#list value![] as m][=m][#sep], [/#sep][/#list]|
[/#list]
[#else]
All microservices already supported fault-tolerance feature or not required it.
[/#list]
[/#outputformat]
