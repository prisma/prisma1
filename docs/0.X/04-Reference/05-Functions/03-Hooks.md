---
alias: pa6guruhaf
description: Functions along the request pipeline of a GraphQL mutation allow you to transform input arguments, initiate workflows and transform the payload.
---

# Hooks

## Overview

Graphcool allows to intercept the requests that are sent to your [GraphQL API](!alias-abogasd0go) and invoke functions when it reaches one of the dedicated _hook points_. This allows to perform data transformation and validation operations on the request payload or synchronously call out to 3rd-party APIs.

Graphcool offers two of these hook points:

- `operationBefore`: Invoked right _before_ a write to the database
- `operationAfter`: Invoked right _after_ a write to the database

Functions invoked through these hooks are executed _synchronously_. 

<InfoBox type=info>

Notice that for [nested mutations](!alias-ol0yuoz6go#nested-mutations), the `operationBefore` and `operationAfter` hooks are invoked for each _individual_ write operation. If you're creating a `User` and a new `Post` in the same (nested) mutation, this means `operationBefore` and `operationAfter` are each invoked twice. 

</InfoBox>

## Adding a Hook function to a service

When you want to create a hook function in your Graphcool service, you need to add it to the service configuration file under the `functions` section. 

### Example

Here is an example of two hook functions:

```yaml
functions:
  validateEmail:
    type: operationBefore
    operation: User.create
    handler:
      webhook: http://example.org/email-validator
  reloadProfilePicture:
    type: operationAfter
    operation: Photo.update
    handler:
      code:
        src: ./code/reloadProfile.js
```

- `validateEmail` is invoked _before_ a `User` node is created and is defined as a _webhook_.
- `reloadProfilePicture` is invoked _after_ a `Photo` node is updated and is defined as a _managed function_.

### Properties

Each function that's specified in the service definition file needs to have the `type` and `handler` properties.

For hook functions, you additionally need to specify the concrete `operation` which consists of a model type and the specific database write (create, update or delete), e.g. `User.create` or `Post.delete`.


## Input type

The input type for these hook functions is determined by the input arguments of the mutation that invokes the function.

Consider the following mutation:

```graphql
updateUser(id: ID!, name: String, email: String): User
```

The input type for the `operationBefore` and `operationAfter` functions can is the following:

```graphql
type UpdateUserInput {
  id: ID!
  name: String
  email: String
}
```

## Current limitations

* Input arguments for nested mutations are *read-only* at the moment. Changes to these are ignored. This applies to all hook points.


## Examples

#### No transformation

The request is not modified at all.

```js
export default event => {
  console.log(`event: ${event}`)

  return {data: event.data}
}
```

#### Computed fields

Some of the input arguments are used to compute a different input argument.

```js
export default event => {
  console.log('Compute area')
  event.data.area = event.data.width * event.data.length

  return event
}
```

#### Input validation

Reject further processing of the incoming GraphQL mutation by [throwing an error](!alias-geihakoh4e).

```js
export default event => {
  if (event.data.length < 0 || event.data.width < 0) {
    return {
      error: 'Length and width must be greater than 0!'
    }
  }

  return event
}
```

## Current limitations

Currently, **only fields that are already part of the mutation payload can be modified**. No fields can be added or removed.

