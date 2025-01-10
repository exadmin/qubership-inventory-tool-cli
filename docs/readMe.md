# How to fill in the inventory.json file

Component is an entity normally located in its own repository. It can be a library or microservice, or have any other __type__.

In order to provide minimum information about specific component please create the file "[inventory.json](inventory.json)" in the root of your component repository and fill in the data according to the structure below. To validate your inventory.json please use [this schema](inventory_schema.json)   

In case of having several components in the same repository please also add related inventory.json files to the roots of the nested components and don't forget to provide the "component" part in the root inventory.json file. 

## Json structure

Field | Type | Mandatory | Contract Values | Description
--- | --- | --- | --- | ---
**id** | String | yes | | Component id. A unique abbreviation of the component (may be concatenation of department ID or domain ID with component abbreviation, or a standalone unique abbreviation. |
**name** | String | yes | | Human-readable name of the component
**owner** | String | no | | Component owner
**dnsName** | String | yes | | The actual DNS name in cloud in case of microservice. If it is a library or other kind of component it should be a name of the repository. DNS name shall be used without stating any namespace/application/composite build relations. |
**altDnsNames** | String[] | no | | Alternative DNS names in case if microservice can be deployed with several various DNS names.
**domain** | String | no | | Component domain
**description** | String | no | | Component description
**type** | String | yes | backend, library, ui, ui app bundle, ui backend, ui cdn, config, job | Component type
**framework** | String | no | Angular, Flask, Quarkus, Vertx, SpringBoot, Drupal | Framework used. SpringBoot and Quarkus can automatically be detected for Java components. 
**documentation** | String[] | no | | URLs to the documentation (Confluence or Git)
**openAPI** | boolean | no | | True if OpenAPI specification is provided
**openAPIpublished** | String[] | no | | URL to the Git repository where component OpenAPI specs are published
||
**tmfSpec** | Object | no | Example:  "tmfSpec": {"622": {"version": ["19.0.1"]}, "666": {"version": ["18.0.3"]}} | TMF specifications numbers
**tmfSpec._key** | String | yes | | TMF specification number
**tmfSpec._value**| Object | yes | |
**tmfSpec.version** | String | yes | | TMF specification version
||
**language** | String[] | no | C++, GoLang, Rust, Groovy, HTML, Java, JavaScript, Lua, Shell, TypeScript, Python, PHP | Used languages. Automatically calculated for Java and Go based components.
||
**thirdparty** | Object | no | Example: "Mongo": {"version": "<version>"}, "Redis": {"mandatory": false}} | Third party apps required for the component, anything that has its own deployment unit. Following third party apps can be automatically detected in **Java** components: PostgreSQL, Opensearch, Cassandra, Redis, Kafka, RabbitMQ, Zookeeper. For non-Java projects those dependencies should be provided here. 
**thirdparty._key** | String | yes | | App name
**thirdparty._value**| Object | yes | | 
**thirdparty.version** | String | no | | App version.
**thirdparty.mandatory** | Boolean | no | | Is it optional or not. Mandatory by default.
||
**dependency** | Object | no | Example: "dependency": {"startup": ["component-3-dns-name", "component-4-dns-name"]} | Only **HTTP** dependencies on other components should be defined here. DNS names are used to link with other components via HTTP.
**dependency.startup** | String[] | no | | Dependencies required for the component startup
**dependency.mandatory** | String[] | no | | Mandatory dependencies for the component
**dependency.optional** | String[] | no | | Optional dependencies for the component
||
**queue** | Object[] | no | Queues this component consumes from or produces to.
**queue.name** | String | yes | | Queue/Topic name
**queue.type** | String | yes | Kafka, RabbitMq | MQ provider. Values are case-insensitive.
**queue.role** | String | yes | producer, consumer | Role for the queue. Case-sensitive!
||
**multitenancy** | Object | no | | 
**multitenancy.defaultTenantId** | boolean | yes | | Default tenant ID support

# Multi-component repositories 

In order to support several components within repository please use "[inventory-components.json](inventory-components.json)" with following structure. 

Field | Type | Mandatory | Values | Description
--- | --- | --- | --- | ---
**domain** | String | yes | | Domain id to put all listed components to
**components** | Object[] | no | Example: [{"id": "component-id", "directory": "path/component"}] | Shall be used in case if there are several components nested inside one repository
**components.id** | String | yes | | Component id
**components.directory** | String | yes | | Repository directory where component can be found relatively to repository root
