---
alias: pa6guruhaf
description: Functions along the request pipeline of a GraphQL mutation allow you to transform input arguments, initiate workflows and transform the payload.
---

# Hooks

Every request that's sent to your API goes through different stages. Graphcool allows to intercept the request and invoke serverless functions when it reaches one of the dedicated _hook points_. This allows to perform data transformation and validation operations on the request payload or synchronously call out to 3rd-party APIs.

Graphcool offers two of these hook points:

- `operationBefore`: Invoked right _before_ a write to the database
- `operationAfter`: Invoked right _after_ a write to the database

Functions invoked through these hooks are executed _synchronously_.

## Operation

Each hook is associated with a particular _write_-operation on one of your model types, for example _creating_ a new `Post` node or _updating_ a `User` node.

## Adding a Hook function

When you want to create a hook function in your Graphcool project, you need to add it to the project configuration file in the `functions` section. Here is an example:

```yaml
functions:
  validateEmail:
    type: operationBefore
    operation: User.create
    handler:
      webhook: http://example.org/email-validator # this could also be a reference to a local function
  reloadProfilePicture:
    type: operationAfter
    operation: Photo.update
    handler:
      code:
        src: ./code/reloadProfile.js
```

When adding a hook function to your project, you have two ways of specifying _how_ it should be invoked:

- Using a **webhook**
- Using a **managed function**



## Request Lifecycle

Every request to the GraphQL APIs passes several execution layers. The request pipeline allows you to **transform and validate data** as well as **prevent a request from reaching the next layer**, effectively aborting the request.

### Execution Layers

The different **execution layers** can be seen in the above diagram.

* First, the raw HTTP request hits your API layer.
* The embedded GraphQL mutation is validated against the [GraphQL schema](!alias-ahwoh2fohj) in the **schema validation** step.
* A valid GraphQL mutation is checked against [defined constraints](!alias-teizeit5se#field-constraints) and the [permission system](!alias-iegoo0heez) in the **data validation** step.
* If the request contained a valid mutation in terms of the GraphQL schema as well as the constraints and permissions, data is written to the database in the **data write** step.
* The mutation payload is sent back as response to the initial HTTP request.

### Hook Points

In between the execution layers, you can use functions at several **hook points**:

* The [`operationBefore `](!alias-caich7oeph) hook after the schema validation allows you to **transforms the input arguments** of the GraphQL mutations and **enforce custom constraints**.
* After the successful extraction of the GraphQL operations from the raw request, the **data validation** layer checks predefined constraints and permissions.
* If the data validation succeeds, the **data is written to the database**.
* The [`operationAfter`](!alias-ecoos0ait6)  hook allows you **transform the payload** that is sent back as response.

> For a given trigger, only **one function** can be assigned to each hook point.

## Current Limitations

* Callbacks need to be converted to Promises. [Here's a guide](https://egghead.io/lessons/javascript-convert-a-callback-to-a-promise).
* Input arguments for nested mutations are *read-only* at the moment. Changes to these are ignored. This applies to all hook points.

## Transform Input Arguments

Functions used for the `operationBefore` hook point can do arbitrary transformations on input arguments or abort an incoming GraphQL mutation all together.

### Examples

#### No Transformation

> The request is not modified at all.

```js
module.exports = function (event) {
  console.log(`event: ${event}`)

  return {data: event.data}
}
```

#### Computed Fields

> Some of the input arguments are used to compute a different input argument.

```js
module.exports = function (event) {
  console.log('Compute area')
  event.data.area = event.data.width * event.data.length

  return event
}
```

#### Input Validation

> Reject further processing of the incoming GraphQL mutation by [throwing an error](!alias-quawa7aed0).

```js
module.exports = function (event) {
  if (event.data.length < 0 || event.data.width < 0) {
    return {
      error: 'Length and width must be greater than 0!'
    }
  }

  return event
}
```

## Transform Mutation Payloads

Functions used for the `TRANSFORM_PAYLOAD` hook point can do arbitrary transformations on the [mutation payload](!alias-gahth9quoo) after a mutation has been successfully executed.

### Examples

#### No Transformation

> The request is not modified at all.

```js
module.exports = function (event) {
  console.log(`event: ${event}`)

  return event
}
```

#### Flip a boolean

> A boolean contained in the input arguments is flipped.

```js
module.exports = function (event) {
  console.log(`event: ${event}`)

  event.data.isPaid = !event.data.isPaid

  return event
}
```

## Current Limitations

Currently, **only fields that are already part of the mutation payload can be modified**. No fields can be added or removed.

