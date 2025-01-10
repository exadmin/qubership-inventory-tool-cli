[#ftl strip_whitespace=true]

[#outputformat "RTF"]
[@md.tableHeader ["Component", "Type", "message"]/]
[#list errors![] as item]
|[=item.component][#t]
|[=item.type][#t]
|[=item.message?replace("([\\|\\*])", "\\\\$1", "r")?replace("[\n]{2,}", "\n", "r")]|
[/#list]
[/#outputformat]
