---
alias: ai9onos0fu 
description: An overview of the three different function types that can be used on the Graphcool platform and how to use them.
---

# 3 Types of Functions

## Overview

In this chapter, we discuss the three different types of serverless functions that you can implement on the Graphcool platform.


## Hooks

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

### Before writing to the database: `operationBefore`

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


### After writing to the database: `operationBefore`

Analogously to the `operationBefore` hook, a function that's associated with `operationAfter` is invoked right after the data from the mutation was written to the database.

The primary use case for `operationAfter` is to synchronously perform tasks that only make sense _after_ a database write actually happened. Imagine you're app uses the initials of a user to generate a profile picture on which the initials are displayed. When a user changes their name, you'll want to generate a new profile picture with the new intitiails. Notice that more advanced use cases should be handled using the _state machine pattern_ discussed a bit later in this chapter.

The input type of the function is identical to the for `operationBefore`, meaning it gets passed the arguments from the mutation. 


## Resolvers

As was discussed in the [Database & API]() chapter, Graphcool generates a GraphQL schema based on the data model that you define for your project. It also generates the resolvers that implement the functionality defined in the schema. However, the auto-generated functionality is limited to CRUD operations along with filtering, ordering and pagination capabilities.  

Sometimes you might want to add more custom functionality to your API that's not covered by the above mentioned CRUD capabilities. In these cases, you can extend your GraphQL schema (meaning you can add new fields to existing types) manually and implement the corresponding resolver functions yourself.

You can do this for the schema's root types as well as regular model types! There generally are two major use cases for these custom resolvers:

- "Shortcuts" to the GraphQL Engine
- Integrating external systems


<InfoBox type="info">

All the functionality that you can implement with custom resolvers can also be implemented through the Proxy Layer. It is up to you where you want to put certain functionality. The Proxy Layer should generally be considered for more advanced use cases though - if your use case is rather simple and you only have a few custom resolvers to be implemented, the Proxy Layer might be an overkill.  

</InfoBox>


### Shortcuts to the GraphQL Engine

Consider the following model type:

```graphql
type Person {
  name: String!
  age: Int!
}
```

If you had an application that frequently needed to load the names of all persons that are under 18 years old, the app would have to send the following query every time:

```graphql
query {
  allPersons(filter: {
    age_lt: 18
  }) {
    name
  }
}
```

This is just a simple example and already rather verbose. With a custom resolver, you could now add a new field to the schema's `Query` type that hides the filter (which you are going to implement yourself in the corresponding resolver function):

```graphql
extend type Query {
  allPersonsUnder18: [Person!]!
}
```

Notice that inside your serverless function, you can use the [`graphcool-lib`](https://github.com/graphcool/graphcool-lib) which provides you with a lot of convenience when accessing the GraphQL Engine.  


### Integrating external systems

Another very powerful use case for custom resolvers is the integration of external systems like 3rd-party APIs or existing microservices.

Consider this simple model type:

```graphql
type Country {
  name: String!
}
```

By adding a custom field to it and implementing a resolver you're effectively able to augment the capabilities of this type. You could for example add new fields to represent the capital of a country:

```graphql
type Country {
  name: String!
  capital: String!
}
```

This new field `capital` now needs to be backed by a custom resolver function that is able to retrieve the capital of a country from some external source.


## Server-side Subscriptions & Events

The last kind of function are server-side subscriptions. In contrast to hooks and custom resolvers, subscriptions are executed _asynchronously_ and triggered by (typed) _events_.






