[#ftl strip_whitespace=true]
{toc:printable=true}
[#outputformat "RTF"]
h2. Libraries list
[#list librariesList![]]
[@md.tableHeader ["Department", "Domain", "Owner", "Abbreviation", "Name", "Used by", "Description"]/]
[#items as item]
|[@md.linkDepartment (item.D.department)!"unknown"/][#t]
|[@md.linkDomain (item.D.id)!"unknown"/][#t]
|[@md.owner (item.D["technical-manager"])!"unknown"/][#t]
|[=(item.C.abbreviation)!"unknown"][#t]
|[@md.linkComponent (item.D.id)!"unknown", (item.C.name)!"unknown"/][#t]
|[=(item.T)!"unknown"][#t]
|[=(item.C.details.description)!"no"]|
[/#items]
[#else ]
Cloud Libraries not registered.
[/#list]
[/#outputformat]
