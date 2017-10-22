---
alias: foatho8aip
description: An overview of the Graphcool service definition file `graphcool.yml` and its YAML structure.
---

# Service definition: `graphcool.yml`

## Overview

The service definition file `graphcool.yml` has the following root properties:

- [`types`](#types): References your type definition file(s).
- [`functions`](#functions): Defines all the [functions](!alias-aiw4aimie9)  you're using in your service.
- [`permissions`](#permissions): Defines all the permission rules for your service.
- [`rootTokens`](#root-tokens): Lists all the [root token](!alias-eip7ahqu5o#root-tokens) you've configured for your service.

> The exact structure of `graphcool.yml` is defined with [JSON schema](http://json-schema.org/). You can find the corresponding schema definition [here](https://raw.githubusercontent.com/graphcool/graphcool-json-schema/master/src/schema.json). 

## Example `graphcool.yml` service definition file

Here is a simple example of a service definition file:

```yml
# Type definitions
types: ./types.graphql


# Functions
functions:

  # Resolver for authentication
  authenticateCustomer:
    handler:
      # Specify a managed function as a handler
      code:
        src: ./src/authenticate.js
        # Define environment variables to be used in function
        environment:
          SERVICE_TOKEN: aequeitahqu0iu8fae5phoh1joquiegohc9rae3ejahreeciecooz7yoowuwaph7
          STAGE: prod
    type: resolver

  # Operation-before hook to validate an email address
  validateEmail:
    handler:
      # Specify a managed function as a handler; since no environment variables
      # are specified, we don't need `src`
      code: ./src/validateEmail.js
    type: operationBefore
    operation: Customer.create

  # Subscription to pipe a new message into Slack
  sendSlackMessage:
    handler:
      # Specify a webhook as a handler
      webhook:
        url: http://example.org/sendSlackMessage
        headers:
            Content-Type: application/json
            Authorization: Bearer cha2eiheiphesash3shoofo7eceexaequeebuyaequ1reishiujuu6weisao7ohc
    type: subscription
    query: ./src/sendSlackMessage/newMessage.graphql


# Permission rules
permissions:
# Everyone can read messages
- operation: Message.read

# Only authenticated users can create messages
- operation: Message.create
  authenticated: true

# To update a message, users need to be authenticated and the
# permission query in `./permissions/updateMessage.graphql` has
# to return `true`; note that this permission only applies to the
# `text` and `attachments` fields of the `Message` type, no other
# fields may be updated
- operation: Message.update 
  authenticated: true
  fields: 
    - text
    - attachments
  query: ./permissions/updateMessage.graphql

# To delete a message, users need to be authenticated and
# the permission query in `./permissions/deleteMessage.graphql`
# has to return `true`
- operation: Message.delete
  authenticated: true
  query: ./permissions/deleteMessage.graphql

# Everyone can perform all CRUD operations for customers
- operation: Customer.read
- operation: Customer.create
- operation: Customer.update
- operation: Customer.delete


# You can edit the fields a permission is applied to
- operation: Customer.Read
- fields: 
  - firstName
  - lastName

# Only authenticated users can connect a `Message`
# and `Customer` node via the `CustomerMessages`-relation
- operation: CustomerMessages.connect
  authenticated: true

# To disconnect a `Message` from a `Customer` node in the 
# `CustomerMessages`-relation, users need to be authenticated and the 
# permission query in `./permissions/disconnectCustomerMessages.graphql`
# has to return `true`
- operation: CustomerMessages.disconnect
  authenticated: true
  query: ./permissions/disconnectCustomerMessages.graphql

# Root tokens
rootTokens:
  - rootToken1
  - RootToken2 # can also start with uppercase letters
```

This service definition expects the following file structure:

```
.
├── graphcool.yml
├── types.graphql
├── src
│   ├── authenticate.js
│   ├── validateEmail.js
│   └── sendSlackMessage
│       └── newMessage.graphql
└── permissions
    ├── updateMessage.graphql
    └── deleteMessage.graphql
```


## YAML structure

### `types`

The `types` root property accepts a **single string** or a **list of strings**. Each string references a `.graphql`-file that contains GraphQL type definitions written in the [SDL](https://medium.com/@graphcool/graphql-sdl-schema-definition-language-6755bcb9ce51).

There are two kinds of types that can be referenced:

- **Model types**: Determine the types that are to be persisted in the database. These types need to be annotated with the `@model`-directive and typically represent entities from the application domain. Read more in the [Database](!alias-viuf8uus7o) chapter.
- **Transient types**: These types are not persisted in the database but typically represent _input_ or _return_ types for certain API operations.

#### Examples

**Referring to a single type definition file**

```yml
types: ./types.graphql
```


**Referring to multiple type definition files**

```yml
types:
  - ./types.graphql
  - ./customResolver.graphql
```


### `functions`

The `functions` root property accepts a **map from string** (which specifies the function's _name_) **to [function](#definition-function)**. The key represents the _name_ of the function, the value is an object that follows the [function](#definition-function) structure and defines the precise configuration of the function to be invoked.

#### Definition: `function`

**All functions** have the following three properties:

- `type` (**required**)
  - **Type**: `string`
  - **Description**: Determines whether this function is a [resolver](!alias-su6wu3yoo2), [subcription](!alias-bboghez0go) or a [hook](!alias-pa6guruhaf).
  - **Possible values:** `resolver`, `subscription`, `operationBefore`, `operationAfter`

- `handler` (**required**)
  - **Type**: handler ([described below](#definition-handler))
  - **Description**: Specifies the details of _how_ to invoke the function. Can either contain references to a _local file_ that contains the implementation of the function or otherwise define a _webhook_ that'll be called when the function is invoked.

- `isEnabled` (**optional**, default: `false`)
  - **Type**: `boolean`
  - **Description**: The function will only be invoked if set to `true`.
  - **Possible values**: `true` or `false`

Only **resolver** functions have the following property:

- `schema` (**optional**, if not provided, the extension of `Query` or `Mutation` has to live inside a file that's referenced from the [`types`](#root-property-types) root property)
  - **Type**: `string`
  - **Description**: References a `.graphql`-file that contains the extension of the `Query` or `Mutation` type which defines the API of the resolver.
  - **Possible values**: any string that references a `.graphql`-file

Only **subscription** functions have the following property:

- `query` (**required**)
  - **Type**: `string`
  - **Description**: References a `.graphql`-file that contains the _subscription query_ which determines the event upon which the function should be invoked as well as the payload for the event.
  - **Possible values**: any string that references a `.graphql`-file

Only **hook** functions have the following property:

- `operation` (**required**)
  - **Type**: `string`
  - **Description**: Describes an operation from the Graphcool CRUD API. The value is composed of the name of a _model type_ and the name of an operation (`read`, `create`, `update` or `delete`), separated by a dot.
  - **Possible values**: `<Model Type>.<Operation>` (e.g. `Customer.create`, `Article.create`, `Image.update`, `Movie.delete`)

#### Definition: `handler`

A `handler` specifies the details of _how_ to invoke the function. It can either be a _managed function_ that references a _local file_ or otherwise define a _webhook_ that'll be called when the function is invoked.

##### Define managed function

**Managed function structure:**

```yml
code:
  # source file that contains the implementation of the function
  src: <file>
  # specify environment variables the function has access to
  environment:
    <variable1>: <value1>
    <variable2>: <value2>
```

Notice that if no environment variables are specified, you can also use the short form without explicitly spelling out `src`:

```yml
code: <file>
```

A `handler` for a managed function has the following properties:

- `code` (**required**)
  - **Type**: `map` (see _managed function structure_ above)
  - **Description**: Describes all the details about how to invoke the managed function and optionally provides environment variables that can be used inside the function at runtime.

- `src` (**required**)
  - **Type**: `string`
  - **Description**: A reference to the file that contains the implementation for the function.
  - **Possible values**: Any string that references a valid source file.


- `environment` (**optional**)
  - **Type**: `[string:string]`
  - **Description**: Specifies a number of environment variables .
  - **Possible values**: Any combination of strings that does not contain the empty string.


##### Reference webhook

**Webhook structure:**

```yml
webhook:
  # HTTP endpoint that represents the webhook
  url: <url>
  # HTTP headers to send along when invoking the webhook
  headers:
    <header1>: <value1>
    <header2>: <value2>
```

Notice that if no HTTP headers are specified, you can also use the short form without explicitly spelling out `url`:

```yml
webhook: <url>
```

A `handler` for a managed function has the following properties:

- `webhook` (**required**)
  - **Type**: `map` (see _webhook structure_ above)
  - **Description**: Describes all the details about how to invoke the webhook and optionally specify HTTP headers that will be attached to the request when the webhook is called.

- `url` (**required**)
  - **Type**: `string`
  - **Description**: The HTTP endpoint where the webhook can be invoked.
  - **Possible values**: Any string that's a valid HTTP URL and references a webhook.


- `headers` (**optional**)
  - **Type**: `[string:string]`
  - **Description**: Specifies a number of HTTP headers.
  - **Possible values**: Any combination of strings that does not contain the empty string.

#### Examples

```yml
functions:

  authenticateCustomer:
    handler:
      # Specify a managed function as a handler
      code:
        src: ./src/authenticate.js
        # Define environment variables for function
        environment:
          SERVICE_TOKEN: aequeitahqu0iu8fae5phoh1joquiegohc9rae3ejahreeciecooz7yoowuwaph7
          STAGE: prod
    type: resolver

  # Operation-before hook to validate an email address
  validateEmail:
    handler:
      # Specify a managed function as a handler; since no environment variables
      # are specified, we don't need `src`
      code: ./src/validateEmail.js
    type: operationBefore
    operation: Customer.create

  # Subscription to pipe a new message into Slack
  sendSlackMessage:
    handler:
      # Specify a webhook as a handler
      webhook:
        url: http://example.org/sendSlackMessage
        headers:
            Content-Type: application/json
            Authorization: Bearer cha2eiheiphesash3shoofo7eceexaequeebuyaequ1reishiujuu6weisao7ohc
    type: subscription
    query: ./src/sendSlackMessage/newMessage.graphql
```

### `permissions`

The `permissions` root property accepts a **list of [permissions](#definition-permission)**. To see a practical example of the Graphcool permission system, check out this [example](https://github.com/graphcool/graphcool/tree/master/examples/permissions) service.

#### Definition: `permission`

**All permissions** have the following four properties:

- `operation` (**required**)
  - **Type**: `string`
  - **Description**: Specifies for which API operation this permission holds. Refers to an operation from the Graphcool CRUD API. The value is composed of the name of a _model type_ and the name of an operation (`read`, `create`, `update` or `delete`), separated by a dot.
  - **Possible values**: `<Model Type>.<Operation>` (e.g. `Customer.create`, `Article.create`, `Image.update`, `Movie.delete`)


- `authenticated` (**optional**, default: `false`)
  - **Type**: `boolean`
  - **Description**: If set to `true`, only [authenticated](!alias-eip7ahqu5o#authenticating-a-request) users will be able to perform the associated `operation`.
  - **Possible values**: `true` or `false`


- `query` (**optional**)
  - **Type**: `string`
  - **Description**: References a file that contains a [permission query](!alias-iox3aqu0ee).
  - **Possible values**: Any string that references a `.graphql`-file containing a permission query.

- `fields` (**optional**)
  - **Type**: `[string]`
  - **Description**: References a list of fields the permission is applied to. Only applicable for type permissions.
  - **Possible values**: A list of any string that references a field of the type the permission belongs to.

#### Examples

```yml
permissions:
# Everyone can read messages
- operation: Message.read

# Only authenticated users can create messages
- operation: Message.create
  authenticated: true

# To update a message, users need to be authenticated and the
# permission query in `./permissions/updateMessage.graphql` has
# to return `true`; note that this permission only applies to the
# `text` and `attachments` fields of the `Message` type, no other
# fields may be updated
- operation: Message.update 
  authenticated: true
  fields: 
    - text
    - attachments
  query: ./permissions/updateMessage.graphql

# To delete a message, users need to be authenticated and
# the permission query in `./permissions/deleteMessage.graphql`
# has to return `true`
- operation: Message.delete
  authenticated: true
  query: ./permissions/deleteMessage.graphql

# Everyone can perform all CRUD operations for customers
- operation: Customer.read
- operation: Customer.create
- operation: Customer.update
- operation: Customer.delete


# You can edit the fields a permission is applied to
- operation: Customer.Read
- fields: 
  - firstName
  - lastName

# Only authenticated users can connect a `Message`
# and `Customer` node via the `CustomerMessages`-relation
- operation: CustomerMessages.connect
  authenticated: true

# To disconnect a `Message` from a `Customer` node in the 
# `CustomerMessages`-relation, users need to be authenticated and the 
# permission query in `./permissions/disconnectCustomerMessages.graphql`
# has to return `true`
- operation: CustomerMessages.disconnect
  authenticated: true
  query: ./permissions/disconnectCustomerMessages.graphql
```

### `rootTokens`

The `rootTokens` property accepts a **list of strings**. Each string is the name of a [root token](!alias-eip7ahqu5o#root-tokens) which will be created whenever the service deployed.

#### Examples

```yml
rootTokens:
  - rootToken1
  - RootToken2 # can also start with uppercase letters
```

<!--

### Table overview

The YAML configuration file has the following _root properties_:

| Root Property | Type | Description |
| --------- | ------------------ | --------------- |
| `types`| `string`<br>`[string]` | Type defintions ([SDL]()) for database models, relations, enums and other types. |
| `functions` | `[string:function]` | All serverless functions that belong to the current service. The key of each element in the dictionary is a unique name for the function, the value specifies details about the function to be invoked. See the `function` type below for more info on the structure. |
| `permissions` | `[permission]` | All permissions rules that belong to the current service. See the `permission` type below for more info on the structure. |
| `modules` | `[string]` | A list of filenames that refer to configuration files of other Graphcool services which are used in the current service (_modules_). |

This is what the additional YAML types look like that are used in the file:

#### Type: `function`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- |
| `isEnabled` | `boolean` | `true`<br>`false` | No (`true`) | The function will only be executed if set to true |
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

-->

## Using variables

Variables allow you to dynamically replace configuration values in your service definition file.

They are especially useful when providing _secrets_ for your service and when you have a multi-staging developer workflow.

To use variables inside `graphcool.yml`, you need to reference the values enclosed in `${}` brackets:

```yml
# graphcool.yml file
yamlKeyXYZ: ${variableSource} # see list of current variable sources below
# this is an example of providing a default value as the second parameter
otherYamlKey: ${variableSource, defaultValue}
```

A _variable source_ can be either of the following two options:

- A _recursive self-reference_ to another value inside the same service
- An _environment variable_
- An _option from the command line_

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
        src: ./src/sendWelcomeEmail.js
    type: subscription
    query: ./src/newUserSubscription.graphql
  createCRMEntry:
    handler:
      code:
        src: ./src/createCRMEntry.js
    type: subscription
    query: ${self:functions.sendWelcomeEmail.handler.query}
```


### Environment variable

You can reference [environment variables](https://en.wikipedia.org/wiki/Environment_variable) inside the service definition file.

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


### CLI options

You can reference CLI options that you passed when invoking a [`graphcool` command](!alias-aiteerae6l) inside your `graphcool.yml` service definition file.

When referencing a CLI option, the value that you put into the bracket is composed of:

- the _prefix_ `opt:`
- the _name_ of the CLI option

> Note: It is valid to use the _empty string_ as the _name_ of the CLI option. This looks like `${opt:}` and the result of declaring this in your `graphcool.yml` is to embed the complete options object (i.e. all the command line options from your `graphcool` command).

For the following example, assume the following `graphcool` command was just ran in the terminal:

```sh
graphcool deploy --stage prod
```

To reference the value of the `stage` option inside `graphcool.yml`, you can now specify the following:

```
webhook:
  url: http://myapi.${opt:stage}.com/example
```

When the command is invoked, the value of `webhook.url` will be deployed as `http://myapi.prod.com/example`.
