---
alias: pa6guruhaf
description: Functions along the request pipeline of a GraphQL mutation allow you to transform input arguments, initiate workflows and transform the payload.
---

# Request Pipeline

Every request that reaches your API goes through different stages that are collectively referred to as the *request pipeline*. Using functions, you can run **data transformation and validation** operations at different hook points along the request.

## Trigger

The trigger of the request pipeline associates the Request Pipeline function with a [create, update or delete mutation](!alias-ol0yuoz6go) of a specific [type](!alias-ij2choozae).

## Request Lifecycle

Every request to the GraphQL APIs pass several execution layers. The request pipeline allows you to **transform and validate data** as well as **prevent a request from reaching the next layer**, effectively aborting the request.

<!-- ![](./hook-points.png) -->

### Execution Layers

The different **execution layers** can be seen in the above diagram.

* First, the raw HTTP request hits your API layer.
* The embedded GraphQL mutation is validated against the [GraphQL schema](!alias-ahwoh2fohj) in the **schema validation** step.
* A valid GraphQL mutation is checked against [defined constraints](!alias-teizeit5se#field-constraints) and the [permission system](!alias-iegoo0heez) in the **data validation** step.
* If the request contained a valid mutation in terms of the GraphQL schema as well as the constraints and permissions, data is written to the database in the **data write** step.
* The mutation payload is sent back as response to the initial HTTP request.

### Hook Points

In between the execution layers, you can use functions at several **hook points**:

* The [`TRANSFORM_ARGUMENT` hook point](!alias-caich7oeph) after the schema validation allows you to **transforms the input arguments** of the GraphQL mutations and **enforce custom constraints**.
* The [`PRE_WRITE` hook point](!alias-phe1gei6io) after the data validation gives you the chance to **commmunicate with external APIs and services** before data is actually written to the database.
* After the successful extraction of the GraphQL operations from the raw request, the **data validation** layer checks predefined constraints and permissions.
* The [`TRANSFORM_PAYLOAD` hook point](!alias-ecoos0ait6) allows you **transform the payload** that is sent back as response.

> For a given trigger, only **one function** can be assigned to each hook point.

## Current Limitations

* Callbacks need to be converted to Promises. [Here's a guide](https://egghead.io/lessons/javascript-convert-a-callback-to-a-promise).
* Input arguments for nested mutations are *read-only* at the moment. Changes to these are ignored. This applies to all hook points.

## Transform Input Arguments

Functions used for the `TRANSFORM_ARGUMENT` hook point can do arbitrary transformations on input arguments or abort an incoming GraphQL mutation all together.

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

