---
alias: mu3iar0eiz 
description: An overview of the three different function types that can be used on the Graphcool platform and how to use them.
---

# Resolvers

As was discussed in the [Database & API]() chapter, Graphcool generates a GraphQL schema based on the data model that you define for your project. It also generates the resolvers that implement the functionality defined in the schema. However, the auto-generated functionality is limited to CRUD operations along with filtering, ordering and pagination capabilities.  

Sometimes you might want to add more functionality to your API that's not covered by the above mentioned CRUD capabilities. In these cases, you can extend your GraphQL schema (meaning you can add new fields to existing types) manually and implement the corresponding resolver functions yourself.

You can do this for the schema's root types as well as regular model types! There generally are three major use cases for these resolvers:

- Authentication
- "Shortcuts" to the GraphQL Engine
- Integrating external systems


<InfoBox type="info">

All the functionality that you can implement with resolvers can also be implemented through the Proxy Layer. It is up to you where you want to put certain functionality. The Proxy Layer should generally be considered for more advanced use cases though - if your use case is rather simple and you only have a few resolvers to be implemented, the Proxy Layer might be an overkill.  

</InfoBox>


## Authentication

With Graphcool, different authentication mechanisms can be implemented using resolver functions as well. The [next chapter]() explains the idea behind this approach in more detail.

## Shortcuts to the GraphQL Engine

Consider the following model type:

```graphql
type PersonQueryPayload {
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

This is just a simple example and already rather verbose. With a resolver, you could now add a new field to the schema's `Query` type that hides the filter (which you are going to implement yourself in the corresponding resolver function):

```graphql
extend type Query {
  allPersonsUnder18: [PersonQueryPayload!]!
}
```

Notice that inside your serverless function, you can use the [`graphcool-lib`](https://github.com/graphcool/graphcool-lib) which provides you with a lot of convenience when accessing the GraphQL Engine.  

> **Note**: It's currently not possible to return `@model` types from resolver functions. See [this](https://github.com/graphcool/framework/issues/743) GitHub issue for more info.

## Integrating external systems

Another very powerful use case for resolvers is the integration of external systems like 3rd-party APIs or existing microservices.

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

This new field `capital` now needs to be backed by a resolver function that is able to retrieve the capital of a country from some external source.
