---
alias: opheidaix3
description: Graphcool uses a dedicated YAML format for configuration.
---

# Project Configuration (YAML)

## Overview

Every Graphcool project consists of several components that developers can provide:

- **Database model**: Determines the types that are to be persisted in the database. These types typically represent the entities from the application domain.
- **Permission rules**: Define which users are allowed to perform what operations in the API. 
- **Serverless functions**: Used to implement data validation and transformation, GraphQL resolvers functions and other business logic.
- **Modules**: Other Graphcool projects that provide additional functionality to the current project.
- **Root tokens**: The root tokens that provide access to all API operations.

To manage each of these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML). The file can be altered manually or through dedicated commands of the CLI.

## Example

Here is what a simple example of a project configuration file looks like:

```yml
types: ./types.graphql

functions:
  authenticateCustomer:
    handler:
      code:
        src: ./code/authenticate.js
    type: resolver
    schema: ./code/authenticate.graphql
  sendSlackMessage:
    handler:
      webhook:
        url: http://example.org/sendSlackMessage
        headers:
            Content-Type: application/json
            Authorization: Bearer saiquiegh1wohb7shie9phai
    type: subscription
    query: ./code/sendSlackMessage/newMessage.graphql

permissions:
- operation: Message.read
  query: ./permissions/message.graphql
  authenticated: true

modules:
- ./modules/facebookLogin/fb.yml

rootTokens:
- authenticate
```

This project configuration expects the following project structure:

```
.
├── code
│   ├── authenticate.graphql
│   └── authenticate.js
│   └── sendSlackMessage
│       └── newMessage.graphql
├── modules
│   └── facebookLogin
│       ├── ...
│       └── fb.yml
└── permissions
    └── message.graphql
```


## YAML Structure

The YAML configuration file has the following _root properties_:

| Root Property | Type | Description | 
| --------- | ------------------ | --------------- | 
| `types`| `string`<br>`[string]` | Type defintions ([SDL]()) for database models, relations, enums and other types. |
| `functions` | `[string:function]` | All serverless functions that belong to the current project. The key of each element in the dictionary is a unique name for the function, the value specifies details about the function to be invoked. See the `function` type below for more info on the structure. |
| `permissions` | `[permission]` | All permissions rules that belong to the current project. See the `permission` type below for more info on the structure. |
| `modules` | `[string]` | A list of filenames that refer to configuration files of other Graphcool projects which are used in the current project (_modules_). |

This is what the additional YAML types look like that are used in the file:

#### Type: `function`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `isEnabled` | `boolean` | `true`<br>`false` | No (`true`) | The function will only be executed if set to true | `true`|
| `handler` | `string`<br>`[string:string]`<br>`[string:webhook]` | any<br>`["webhook":any]`<br>`["webhook":any]` | Yes | Defines how this function should be invoked (i.e. where it lives). Either as a webhook or a local Graphcool function. | 
| `type` | `string` | `subscription`<br>`resolver`<br>`operationBefore`<br>`operationAfter` | Yes | Defines what kind of serverless function this is. |
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `operationBefore` or `operationAfter` | If the function is set up in the context of an operation, this specifies the concrete operation. |
| `query` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `subscription` | If the function is set up as a subscription, this specifies the subscription query (which determines the input type of the function). |
| `schema` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `resolver` | If the function is set up as a resolver, this specifies the necessary extensions on the `Query` or `Mutation` type (and potentially additional types that represent the input or return types of the new field).


#### Type: `permission`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Yes | Specifies the operation for which this permission rule should apply. |
| `authenticated` | `boolean` | `true`<br>`false` | No (`false)` | Specifies any HTTP headers for the request that invokes the function. | Defines whether a request needs to be authenticated to perform the corresponding operation (if true, the `id` of the authenticated node is available as an argument inside the permission query. |
| `query` | `string` | any | No | Specifies the permission query that belongs to this permission rule. |
| `fields` | `[string]` | any | all fields of the model type | Specifies to which fields this permission rule should apply to.

 
#### Type: `webhook`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `url` | `string` | any | Yes | Specifies the URL where the function can be invoked. |
| `headers` | `[string:string]` | any | No | Specifies any HTTP headers for the request that invokes the function. |