[#ftl strip_whitespace=true]
{toc:printable=true}
[#outputformat "RTF"]
h2. Dependencies list
[@md.tableHeader ["groupId", "artifactId", "version", "direct", "transitive"]/]
[#list librariesList![] as item]
|[=(item.groupId)!"unknown"][#t]
|[=(item.artifactId)!"unknown"][#t]
|[=(item.version)!"unknown"][#t]
|[#list item.directDependencies![]?sort_by("id") as dd][=dd.id][#sep], [/#sep][#else] [/#list][#t]
|[#list item.transitiveDependencies![]?sort_by("id") as td][=td.id][#sep], [/#sep][#else] [/#list]|
[#else]
Dependencies not found.
[/#list]
[/#outputformat]
