---
alias: opheidaix3
description: Graphcool uses a dedicated YAML format for configuration.
---

# Project Configuration (YAML)

## Overview

Every Graphcool project consists of several pieces that developers can provide:

- Database model: Determines the types that are to be persisted in the database. These types typically represent the entities from the application domain.
- Permission rules: Define which users are allowed to perform what operations in the API. 
- Serverless functions: Used to implement business logic.
- Modules: Other Graphcool projects that provide additional functionality.

To manage each of these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML). 

The file can be altered manually or through the CLI.

## YAML Format

The YAML configuration file has the following _root properties_:

| Root Property | Type | Description | 
| --------- | ------------------ | --------------- | 
| `types`| `string`<br>`[string]` | Type defintions ([SDL]()) for database models, relations, enums and other types. |
| `functions` | `[string:function]` | All serverless functions that belong to the current project. |
| `permissions` | `[permission]` | All permissions rules that belong to the current project. |
| `modules` | `[string]` | A list of filenames that refer to configuration files of other Graphcool projects which are used in the current project (_modules_). |

Here are the missing type definitions:

##### Type: `function`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `isEnabled` | `boolean` | `true`<br>`false` | No (`true`) | The function will only be executed if set to true | `true`|
| `handler` | `string`<br>`[string:string]`<br>`[string:webhook]` | any<br>`["webhook":any]`<br>`["webhook":any]` | Yes | Defines how this function should be invoked (i.e. where it lives). Either as a webhook or a local Graphcool function. | 
| `type` | `string` | `subscription`<br>`resolver`<br>`operationBefore`<br>`operationAfter` | Yes | Defines what kind of serverless function this is. |
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `operationBefore` or `operationAfter` | If the function is set up in the context of an operation, this specifies the concrete operation. |
| `query` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `subscription` | If the function is set up as a subscription, this specifies the subscription query (which determines the input type of the function). |
| `schema` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `resolver` | If the function is set up as a resolver, this specifies the necessary extensions on the `Query` or `Mutation` type (and potentially additional types that represent the input or return types of the new field).


##### Type: `permission`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Yes | Specifies the operation for which this permission rule should apply. |
| `authenticated` | `boolean` | `true`<br>`false` | No (`false)` | Specifies any HTTP headers for the request that invokes the function. | Defines whether a request needs to be authenticated to perform the corresponding operation (if true, the `id` of the authenticated node is available as an argument inside the permission query. |
| `query` | `string` | any | No | Specifies the permission query that belongs to this permission rule. |
| `fields` | `[string]` | any | all fields of the model type | Specifies to which fields this permission rule should apply to.

 
##### Type: `webhook`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `url` | `string` | any | Yes | Specifies the URL where the function can be invoked. |
| `headers` | `[string:string]` | any | No | Specifies any HTTP headers for the request that invokes the function. |