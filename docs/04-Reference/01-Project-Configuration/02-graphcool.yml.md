---
alias: foatho8aip
description: An overview of the Graphcool project definition file `graphcool.yml` and its YAML structure.
---

# Project Definition (YAML)

## Overview

The project definition has the following root properties:

- `types`: References your type definition file.
- `functions`: Defines all the functions you're using in your project.
- `permissions`: Defines all the permission rules for your project.
- `rootTokens`: Lists all the [root token](!alias-eip7ahqu5o#root-tokens) you've configured for your project.

See below for the concrete [YAML structure](#yaml-structure).

## Example

Here is a simple example of a project definition file:

```yml
# Type Definitions
types: ./types.graphql


# Serverless Functions
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


# Permission Rules
permissions:
- operation: Message.read
- operation: Message.create
  authenticated: true
  query: ./permissions/message.graphql


# Modules
modules:
- ./modules/facebookLogin/fb.yml


# Root tokens
rootTokens:
- authenticate
```

This project definition expects the following project structure:

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


## YAML structure

The YAML configuration file has the following _root properties_:

| Root Property | Type | Description | 
| --------- | ------------------ | --------------- | 
| `types`| `string`<br>`[string]` | Type defintions ([SDL]()) for database models, relations, enums and other types. |
| `functions` | `[string:function]` | All serverless functions that belong to the current project. The key of each element in the dictionary is a unique name for the function, the value specifies details about the function to be invoked. See the `function` type below for more info on the structure. |
| `permissions` | `[permission]` | All permissions rules that belong to the current project. See the `permission` type below for more info on the structure. |
| `modules` | `[string]` | A list of filenames that refer to configuration files of other Graphcool projects which are used in the current project (_modules_). |

This is what the additional YAML types look like that are used in the file:

### Type: `function`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `isEnabled` | `boolean` | `true`<br>`false` | No (`true`) | The function will only be executed if set to true |
| `handler` | `string`<br>`[string:string]`<br>`[string:webhook]` | any<br>`["webhook":any]`<br>`["webhook":any]` | Yes | Defines how this function should be invoked (i.e. where it lives). Either as a webhook or a local Graphcool function. | 
| `type` | `string` | `subscription`<br>`resolver`<br>`operationBefore`<br>`operationAfter` | Yes | Defines what kind of serverless function this is. |
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `operationBefore` or `operationAfter` | If the function is set up in the context of an operation, this specifies the concrete operation. |
| `query` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `subscription` | If the function is set up as a subscription, this specifies the subscription query (which determines the input type of the function). |
| `schema` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `resolver` | If the function is set up as a resolver, this specifies the necessary extensions on the `Query` or `Mutation` type (and potentially additional types that represent the input or return types of the new field).


### Type: `permission`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Yes | Specifies the operation for which this permission rule should apply. |
| `authenticated` | `boolean` | `true`<br>`false` | No (`false)` | Specifies any HTTP headers for the request that invokes the function. | Defines whether a request needs to be authenticated to perform the corresponding operation (if true, the `id` of the authenticated node is available as an argument inside the permission query. |
| `query` | `string` | any | No | Specifies the permission query that belongs to this permission rule. |
| `fields` | `[string]` | any | all fields of the model type | Specifies to which fields this permission rule should apply to.

 
### Type: `webhook`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `url` | `string` | any | Yes | Specifies the URL where the function can be invoked. |
| `headers` | `[string:string]` | any | No | Specifies any HTTP headers for the request that invokes the function. |


## Using variables

Variables allow you to dynamically replace configuration values in your project definition file.

They are especially useful when providing _secrets_ for your service and when you have a multi-staging developer workflow.

To use variables inside `graphcool.yml`, you need to reference the values enclosed in `${}` brackets:

```yml
# graphcool.yml file
yamlKeyXYZ: ${variableSource} # see list of current variable sources below
# this is an example of providing a default value as the second parameter
otherYamlKey: ${variableSource, defaultValue}
```

A _variable source_ can be either of the following two options:

- A _recursive self-reference_ to another value inside the same project
- An _environment variable_
- The name of the currently active [environment](!alias-zoug8seen4) from [`.graphcoolrc`](!alias-zoug8seen4#.graphcoolrc)

> Note that you can only use variables in property **values** - not in property keys. So you can't use variables to generate dynamic logical IDs in the custom resources section for example.

### Recursive self-reference

You can recursively reference other property values that live inside the same `graphcool.yml` file.

When using a recursive self-reference as a variable, the value that you put into the bracket is composed of:

- the _prefix_ `self:`
- (optional) the _path_ to the referenced property

> If no path is specified, the value of the variable will be the full YAML file.

In the following example, the `createCRMEntry` function uses the same subscription query as the `sendWelcomeEmail` function:

```yml
functions:
  sendWelcomeEmail:
    handler:
      code:
        src: ./code/sendWelcomeEmail.js
    type: subscription
    query: ./code/newUserSubscription.graphql
  createCRMEntry:
    handler:
      code:
        src: ./code/createCRMEntry.js
    type: subscription
    query: ${self:functions.sendWelcomeEmail.handler.query}
```


### Environment variable

You can reference [environment variables](https://en.wikipedia.org/wiki/Environment_variable) inside the project definition file.

When using an environment variable, the value that you put into the bracket is composed of:

- the _prefix_ `env:`
- the _name_ of the environment variable

In the following example, an environment variable is referenced to specify the URL and the authentication token for a webhook:

```yml
functions:
  initiatePayment:
    handler:
      webhook:
        url: ${env:PAYMENT_URL}
        headers:
            Content-Type: application/json
            Authorization: Bearer ${env:AUTH_TOKEN}
    type: subscription
```

<InfoBox type=warning>

Note that you can not use the name `GRAPHCOOL_ENV` for your environment variables. This will automatically refer to the currently active environment (see the section below).

</InfoBox>

### Name of currently active environment

You can reference the name of the currently active [environment](!alias-zoug8seen4) inside the project definition file.

The syntax is similar to the one for referencing environment variables, except that the _name_ of the environment variable is replaced with `GRAPHCOOL_ENV`:

```yml
${env:GRAPHCOOL_ENV}
```

> Note: Unlike you might guess from the syntax, `GRAPHCOOL_ENV` is not actually set as an environment variable. 



