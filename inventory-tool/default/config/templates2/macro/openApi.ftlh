[#ftl output_format="RTF"]
h3. Rest endpoints
[#if openApi??]

    [#list (openApi.paths!{})?keys as path]
[@tablePath openApi.paths, path /]
    [/#list]
[#else ]
Specification not found.
[/#if]

h3. Data model schema
[#if openApi??]
{plantuml}
@startuml
scale 0.7
[#list openApi.components.schemas?keys as key]
[@diagramComponent openApi.components, key /]
[/#list]

[#list openApi.components.schemas?keys as key]
[@diagramRelation openApi.components, key /]
[/#list]
@enduml
{plantuml}
[#else]
Specification not found.
[/#if]

h3. Data model
[#if openApi??]
[#list openApi.components.schemas?keys as key]
[@tableComponent openApi.components, key /]
[/#list]
[#else]
Specification not found.
[/#if]

[#macro refValue value]
[= (value.canonicalRef!"")?keep_after_last("#")] [#t]
[/#macro]
[#macro refSchemaName value]
[= (value.canonicalRef!"")?keep_after_last("/")] [#t]
[/#macro]

[#macro refLink value={}]
[#if value.type! == "array" && value.itemsSchema??]
[#assign ref = (value.itemsSchema.canonicalRef)!?keep_after_last("/")/]
[[=ref]|#[=ref]] [#t]
[#elseif value.canonicalRef??]
[#assign ref = value.canonicalRef?keep_after_last("/")/]
[[=ref]|#[=ref]] [#t]
[/#if]
[/#macro]

[#macro printType value = "", asHref=false]
[#if value == ""]Object[#t][#return /][/#if][#t]
    [#if value.type! == "array"]
        [#if value.itemsSchema??]
            [#if value.itemsSchema.type??]
List&lt;[= value.itemsSchema.type?cap_first]&gt;[#t]
            [#else]
List&lt;[#if asHref][@refLink value.itemsSchema /][#else][@refSchemaName value.itemsSchema /][/#if]&gt;[#t]
            [/#if]
        [/#if]
    [#elseif !value.type?? && value.ref]
[#if asHref][@refLink value /][#else][@refSchemaName value /][/#if][#t]
    [#elseif value.type! == "object"]
Map&lt;String, [@printType value.additionalProperties /]&gt;[#t]
    [#else]
[= value.type!?cap_first][#t]
    [/#if]
[/#macro]

[#macro tableComponent components, name]
[#assign component = components.schemas[name] /]
h4. [=name]
{anchor:[=name]}
[= component.description!"No description"]
||Name||Type||Format||Description||
[#list component.properties!{} as prop, value]
|[=prop][#t]
|[@printType value, true /][#t]
|[= value.format!" "][#t]
|[= value.description!" "]|
[/#list]

[/#macro]


[#macro diagramComponent components, name]
class  [=name] {
    [#assign component = components.schemas[name] /]
    [#list component.properties!{} as prop, value]
    [= prop ] : [@printType value /]
[#--
        type: [@printType value /]
        format: [= value.format!]
        description: [= value.description!]
        ref: [#t]
        [#if value.type! == "array"]
            [@refValue value.itemsSchema /]
        [#else]
            [@refValue value /]
        [/#if]
        enum: [#list (value.enums![]) as enum][=enum][#sep], [/#sep][/#list]
--]
    [/#list]
}
[/#macro]

[#macro diagramRelation components, name]
    [#assign component = components.schemas[name] /]
    [#list component.properties!{} as prop, value]
        [#if value.canonicalRef??]
[= name ] --> [@refSchemaName value /]
        [#elseif value.type! == "array" && value.itemsSchema?? && !value.itemsSchema.type??]
[= name ] --> [@refSchemaName value.itemsSchema /]
        [#elseif value.type! == "object"
        && (value.additionalProperties.type)! == "array"
        && (value.additionalProperties.itemsSchema)??
        && !(value.additionalProperties.itemsSchema.type)??]
            [= name ] --> [@refSchemaName value.additionalProperties /]
        [/#if]
    [/#list]
[/#macro]

[#macro tablePath paths, path]
    [#list paths[path].operations!{} as method, operation]
{expand:title=[=method?upper_case] [=path]|atlassian-macro-output-type=BLOCK}
Description: [=(operation.description!operation.summary)!"No description"]
        [#list operation.parameters![]]
Parameters:
||Name||Where||Type||Required||Description||
        [#items as parameter]
|[=parameter.name][#t]
|[=parameter.in][#t]
|[=(parameter.schema.type)!][#t]
|[=parameter.required?c][#t]
|[=(parameter.schema.description)!" "]|
        [/#items]
        [/#list]
        [#if operation.requestBody??]

Content body:
required: [=operation.requestBody.required?c]
            [#list operation.requestBody.contentMediaTypes!{}]
||Media type||Type||Item||
            [#items as type, media]
|[=type][#t]
|[=media.schema.type!"Object"][#t]
|[#if media.schema??][@refLink media.schema /][/#if]|
            [/#items]
            [/#list]
        [/#if]

Responses:
        [#list operation.responses!{} ]
||HTTP Code||Description||Media type||Type||Item||
        [#items as code, response]
|[=code][#t]
|[=response.description!][#t]
[#list response.contentMediaTypes!{} ]
[#items as type, media]
|[=type][#t]
|[=(media.schema.type)!"Object"][#t]
|[@refLink media.schema /] |
[#break ]
[/#items]
[#else]
||||
[/#list]
        [/#items]
        [/#list]
{expand}

    [/#list]
[/#macro]
