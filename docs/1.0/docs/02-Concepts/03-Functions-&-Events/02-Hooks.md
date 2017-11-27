---
alias: ai9onos0fu 
description: An overview of the three different function types that can be used on the Graphcool platform and how to use them.
---

# Hooks

When the GraphQL Engine processes a mutation that it receives from a client, there are two different _hooks_ that you can each associate with a serverless function. One hook right _before_ the write to the database happens (called `operationBefore`), the other one right _after_ (called `operationAfter`). When the mutation reaches these hooks, the GraphQL Engine invokes the associated function.

This allows to implement custom business logic for data validation transformation of mutation input arguments or calling out to 3rd-party APIs in a synchronous fashion.

It's important to mention, that a _nested mutation_ will trigger _multiple_ operations. For example, the following mutation to create a `Person` as well as a `House` that this person owns, will invoke the `operationBefore` and `operationAfter` hooks two times each:

```graphql
mutation {
  createHouse(owner: {
    name: "Alice"
  }) {
    id
  }
}
```

<InfoBox type="info">

The `operationBefore` and `operationAfter` hooks offer relatively simple ways to inject custom functionality into the GraphQL Engine. For more advanced use cases, the Proxy Layer should be used.

</InfoBox>

## Before writing to the database: `operationBefore`

A function that is associated with the `operationBefore` hook gets invoked right before data from a single operation is written to the database.

This hook is commonly used for _input validation_ and _transformation_ of the input arguments provided for the operation. For example, you could make sure that email addresses are valid before they get written into the database, otherwise you can return a custom error and let the request fail. Another use case it to transform the input arguments of the mutation, for example to make sure that all credit card numbers to be stored in the database are formatted in the same way.

Notice that the input type of the function that's associated with `operationBefore` corresponds to the input arguments of the mutation.

Considering the following mutation:

```graphql
createUser(username: String!, email: String!): Person
```

In this case, `username` and `email` would get bundled into the same object which is passed into the function.

Let's assume the mutation would be called as follows:

```graphql
mutation {
  createUser(username: "Alice", email: "alice@example.org") {
    id
  }
}
```

The input for the function would now be the following object:

```json
{
  "data": {
    "username": "Alice",
    "email": "alice@example.org",
  }
}  
```


## After writing to the database: `operationAfter`

Analogously to the `operationBefore` hook, a function that's associated with `operationAfter` is invoked right after the data from the mutation was written to the database.

The primary use case for `operationAfter` is to synchronously perform tasks that only make sense _after_ a database write actually happened. Imagine you're app uses the initials of a user to generate a profile picture on which the initials are displayed. When a user changes their name, you'll want to generate a new profile picture with the new intitiails. Notice that more advanced use cases should be handled using the _state machine pattern_ discussed a bit later in this chapter.

The input type of the function is identical to the for `operationBefore`, meaning it gets passed the arguments from the mutation. 
