[#ftl strip_whitespace=true]
{toc:printable=true}
[#outputformat "RTF"]
[#assign c = component /]
[=(c.details.description)!"missed"]
Git Repository: [=(c.repository)!"missed"]
[#if ((c.details.api.openApi)!"")?matches("yes", "i")]
Published: [#list (c.details.api.apiSpecPublished)![] as apiSpec][@md.link apiSpec/][#sep], [/#sep][#else]not published[/#list]
[/#if]

h2. Dependencies diagram
[#if encodedDiagramMicroservice??]
![=encodedDiagramMicroservice]!
[/#if]

[@md.tableHeader ["Direction", "Domain", "Abbrv", "Component", "Owner", "Dependency type"]/]
[#list inDependenciesList![]]
[#items as item]
| IN[#t]
|[@md.linkDomain (item.component.domain)!"unknown"/][#t]
|[=(item.component.id)!"unknown"][#t]
|[@md.linkComponent (item.component.domain)!"unknown", (item.component.name)!"unknown"/][#t]
|[=(item.component.owner)!"unknown"][#t]
|[=(item.type)!"unknown"]|
[/#items]
[/#list]
[#list outDependenciesList![]]
[#items as item]
| OUT[#t]
|[@md.linkDomain (item.component.domain)!"unknown"/][#t]
|[=(item.component.id)!"unknown"][#t]
|[@md.linkComponent (item.component.domain)!"unknown", (item.component.name)!"unknown"/][#t]
|[=(item.component.owner)!"unknown"][#t]
|[=(item.type)!"unknown"]|
[/#items]
[/#list]

h2. Details
||Owner|[=(c.details.owner)!"missed"] |
||Abbreviation|[=(c.details.abbreviation)!"missed"] |
||DNS|[=(c.details.dnsName)!"missed"] |
||Domain|[@md.linkDomain c.details.domain/] |
||TMF specification|[#list (c.details.tmfSpec)![] as tmf][=tmf!"missed"][#sep], [/#sep][/#list] |
||Type|[=(c.details.type)!"missed"] |
||Sticky session|[=(c.details.stickySessions)!"missed"] |
||Language|[=(language)!"missed"] |
||Framework|[=framework!"missed"] |
||Reactive|[=(c.details.reactive)!"missed"] |
||Database|[#list (c.details.database.database)![] as db][=(db.item)!"missed"][#sep], [/#sep][/#list] |
||ZK DB connection support|[#list (c.details.database.database)![] as db][=(db.viaZookeeper)!"missed"][#sep], [/#sep][/#list] |
||External index|[=(c.details.database.externalIndices.item)!"missed"] |
||External index via Zookeeper|[=(c.details.database.externalIndices.viaZookeeper)!"missed"] |
||External cache|[=(c.details.database.externalCache.item)!"missed"] |
||External cache via Zookeeper|[=(c.details.database.externalCache.viaZookeeper)!"missed"] |
||RabbitMQ Exchanges|[#list (c.details.messageQueues.rabbitMQ.producer)![] as producer][=producer][#sep], [/#sep][/#list] |
||RabbitMQ Queues|[#list (c.details.messageQueues.rabbitMQ.consumer)![] as consumer][=consumer][#sep], [/#sep][/#list] |
||Kafka Topic (producer)|[#list (c.details.messageQueues.kafka.producer)![] as producer][=producer][#sep], [/#sep][/#list] |
||Kafka Topic (consumer)|[#list (c.details.messageQueues.kafka.consumer)![] as consumer][=consumer][#sep], [/#sep][/#list] |
||Startup dependency|[#list (c.details.dependencies.startup)![] as startup][=startup][#sep], [/#sep][/#list] |
||Mandatory dependency|[#list (c.details.dependencies.mandatory)![] as mandatory][=mandatory][#sep], [/#sep][/#list] |
||Optional dependency|[#list (c.details.dependencies.optional)![] as optional][=optional][#sep], [/#sep][/#list] |
||Scheduling/cronjobs|[#list (c.details.dependencies.schedulingCronjobs)![] as schedulingCronjobs][=schedulingCronjobs][#sep], [/#sep][/#list] |
||Confluence article|[@md.link (c.details.api.confluenceArticle)!"missed"/] |
||OpenAPI/Swagger|[=(c.details.api.openApi)!"missed"] |
||API versioning|[=(c.details.api.apiVersioning)!"missed"] |

[#if (c.details.documentationLink)??]
h2. Documentation
[#list (c.details.documentationLink)![] as doc]
[@md.link doc, doc /]

[/#list]
[/#if]

h2. Error codes
[@md.tableHeader ["Error Code", "Type", "Message Text"] /]
[#list directErrorCodes![] as item]
|[@md.link item.id, item.details.describedIn /] [#t]
|direct [#t]
|[=(item.details.messageText)!"missed"] [#t]
|
[/#list]
[#list indirectErrorCodes![] as item]
|[@md.link item.id, item.details.describedIn /] [#t]
|indirect [#t]
|[=(item.details.messageText)!"missed"] [#t]
|
[/#list]
h3. Total count of error codes: [=totalErrorCodesCount]

h2. Supported gateways
[#list (gateways![])?sort as gateway]
    * [=gateway]
[/#list]

[#if (c.features.hpa)??]
h2. HPA Support
||Resource type|[=(c.features.hpa.resource)!"missed"]|
||Scale up|[=(c.features.hpa.scaleup)!"missed"]|
||Scale down|[=(c.features.hpa.scaledown)!"missed"]|
||HPA variables|[#list (c.features.hpa.variables)![] as variables][=variables][#sep], [/#sep][/#list]|
||File path|[#if (c.features.hpa.fileLink)??][link|[=c.features.hpa.fileLink]][#else]not found[/#if]|
[/#if]

h2. Features
h3. Blue-Green
||HTTP request|[=(c.features.blueGreen.httpRequest)!"missed"]|
||HTTP callback|[=(c.features.blueGreen.httpCallback)!"missed"]|
||Zeebe workers|[=(c.features.blueGreen.zeebeWorkers)!"missed"]|
||Message Queue|[=(c.features.blueGreen.messageQueueConsumers)!"missed"]|

h3. Fault Tolerance
||Error Codes & Response|[=(c.features.faultTolerance.errorCodes)!"missed"]|
||HTTP Retry Policy|[=(c.features.faultTolerance.httpRetryPolicy)!"missed"]|
||Custom Health Probes|[=(c.features.faultTolerance.customHealthProbes)!"missed"]|

h2. Artifacts
[#list artifacts![]]
[@md.tableHeader ["#", "Package", "GroupId", "ArtifactId", "Version"]/]
[#items as item]
|[=item?counter][#t]
|[=item.package!"unknown"][#t]
|[=item.groupId!"unknown"][#t]
|[=item.artifactId!"unknown"][#t]
|[=item.version!"unknown"]|
[/#items]
[#else ]
No artifact found in the component repository.
[/#list]

h2. Direct dependency
[#list directDependencies![]]
[@md.tableHeader ["#", "GroupId", "ArtifactId", "Version"]/]
[#items as item]
|[=item?counter][#t]
|[=item.groupId!"unknown"][#t]
|[=item.artifactId!"unknown"][#t]
|[=item.version!"unknown"]|
[/#items]
[#else ]
No direct dependencies found.
[/#list]

h2. Transitive dependency
[#list transitiveDependencies![]]
[@md.tableHeader ["#", "GroupId", "ArtifactId", "Version"]/]
[#items as item]
|[=item?counter][#t]
|[=item.groupId!"unknown"][#t]
|[=item.artifactId!"unknown"][#t]
|[=item.version!"unknown"]|
[/#items]
[#else ]
No transitive dependencies found.
[/#list]

h2. Duplicated GroupId's
[#list groupIdDuplicated![]]
[@md.tableHeader ["#", "GroupId", "Versions"]/]
[#items as item]
|[=item?counter][#t]
|[#list item as key, value][=key]|[#list value as version][=version][#sep], [/#sep][/#list][/#list]|
[/#items]
[#else ]
No duplicates found.
[/#list]

[/#outputformat]
