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

<InfoBox type=warning>

Notice that for **nested mutations**, the `operationBefore` and `operationAfter` hooks are invoked for each _individual_ write operation. If you're creating a `User` and a new `Post` in the (nested) same mutation, this means `operationBefore` and `operationAfter` are each invoked twice. 

</InfoBox>


## Adding a Hook function to the project

When you want to create a hook function in your Graphcool project, you need to add it to the project configuration file under the `functions` section. 

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

`validateEmail` is invoked _before_ a `User` node is created and is defined as a _webhook_.

`reloadProfilePicture` is invoked _after_ a `Photo` node is updated and is defined as a _managed function_.

### Properties

Each function that's specified in the project configuration file needs to have the `type` and `handler` properties.

For hook functions, you additionally need to specify the concrete `operation` which consists of a model type and the specific database write (create, update or delete), e.g. `User.create` or `Post.delete`.


## Input Type

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

## Current Limitations

* Callbacks need to be converted to Promises. [Here's a guide](https://egghead.io/lessons/javascript-convert-a-callback-to-a-promise).
* Input arguments for nested mutations are *read-only* at the moment. Changes to these are ignored. This applies to all hook points.


## Examples

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

## Current Limitations

Currently, **only fields that are already part of the mutation payload can be modified**. No fields can be added or removed.

