[#ftl strip_whitespace=true]
{toc:printable=true}
[#outputformat "RTF"]

h2. Dependencies diagram
[#if encodedDiagramDepartment??]
![=encodedDiagramDepartment]!
[#else]N/A[/#if]

h2. Domains list
[#list domainsList![]]
[@md.tableHeader ["#", "Abbreviation", "Name", "TM", "Services (count)", "UI backend (count)", "UI (count)", "Libraries (count)", "Others (count)"]/]
[#items as entry]
|[=entry?counter][#t]
|[@md.linkDomain entry.D.id/][#t]
|[=(entry.D.name)!"missed"][#t]
|[@md.owner entry.D["technical-manager"]/][#t]
|[=(entry.C["backend"])!0][#t]
|[=(entry.C["ui backend"])!0][#t]
|[=(entry.C["ui"])!0][#t]
|[=(entry.C["library"])!0][#t]
|[=(entry.T!0) - (entry.C["backend"]!0) - (entry.C["ui backend"]!0) - (entry.C["ui"]!0) - (entry.C["library"]!0)]|
[/#items]
[#else]
Department is empty and not have registered Domains.
[/#list]

h2. Used Tech.stack
[@md.tableHeader ["Name", "Domains"] /]
[#list techStackList![] as map]
[#list map?keys?sort as key]
|[=key]|[#list map[key] as domain][@md.linkDomain domain.id/][#sep], [/#sep][/#list]|
[/#list]
[/#list]

h2. TMF specification support
[#list tmfSpecsList![]]
[@md.tableHeader ["TMF Spec.", "Spec. version", "Specification name", "Domain", "Abbreviation", "Microservice", "Microservice Owner"]/]
[#items as entry]
|[=(entry.S.code)!"missed"][#t]
|[=(entry.S.version)!"missed"][#t]
|[@md.link entry.S.name, entry.S.url/][#t]
|[@md.linkDomain entry.D.id/][#t]
|[=entry.C.abbreviation!"missed"][#t]
|[@md.linkComponent entry.D.id, entry.C.name/][#t]
|[@md.owner (entry.C.details.owner)!"unknown"/]|
[/#items]
[#else ]
No TMF specifications implemented.
[/#list]

h2. Features
TBD

# *Blue-Green*: HTTP requests, HTTP callbacks (websockets), Zeebe workers, Message Queue consumers
# *Fault Tolerance*: Error Codes & Response, HTTP Retry Policy, Custom Health Probes
[/#outputformat]
