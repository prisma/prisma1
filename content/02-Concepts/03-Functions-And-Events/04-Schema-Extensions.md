---
alias: mie4aihe7u 
description: An overview of how schema extensions work with Graphcool.
---

# Schema Extensions

## Overview

As explained in the previous chapter, the GraphQL schema very clearly defines the API of a GraphQL server. Only those queries and mutations that are specified in the schema will be processable at all by the server. When using Graphcool, the API-defining schema is automatically generated for you and specifies a CRUD GraphQL API that is based on the model types that you set up for your project.

In the previous sections, we covered how you can call out to 3rd party APIs in response to an event. These are primarily use cases to take *actions* in another system, e.g. sending an email with Mailchimp or charging a user on Stripe. However, sometimes you'll also want to provide your users with the ability to retrieve data from external systems.  With Graphcool, you can easily do so by *integrating* another system in your own API! The mechanism that Graphcool provides for this use case is called *schema extensions*. 


> If you're already familiar with how a GraphQL server works, you can think of a schema extension as a custom resolver function. This resolver is  implemented as a serverless function.


More use cases for schema extensions include adding *computed fields* to your model types or implementing custom authentication mechanisms.

## Extending Your Schema with Custom Queries and Mutations

A GraphQL schema has three special types: `Query`, `Mutation` and `Subscription`. These types define the *entry points* to the API and specify what queries, mutations and subscriptions a GraphQL server is able to process. As an example, consider the following GraphQL query:

```graphql
query {
  allUsers {
    name
  }
}
```

A GraphQL server can only process this query if its schema looks (at least) as follows:

```graphql
type Query {
  allUsers: [User!]! # this enables the allUsers query from above
}

type User {
  name: String!
}
```

Now, instead of exposing only CRUD operations, you can define your *own* queries and mutations to be included in your API. When doing so, you can implement the *resolver* yourself, meaning you retain full power over the actual functionality that should be provided.

For example, you might want to enrich your API and include some weather forecast data by retrieving it from an external weather service. In this case, you could simply add a new field to the existing schema's `Query` type: 

```graphql
type Query {
  weatherForecast(days: Int): [Weather!]!
}

type Weather {
  temperature: Int
}
```

You would then need to implement a serverless function that's associated with the new `weatherForecast` field in which you access the external weather service to retrieve the data and return it to the client. Assume a client sends the following query:

```graphql
query {
  weatherForecast(days: 7) {
    temperature
  }
}
```

When the server receives this query, it will invoke your custom resolver which then fetches the data from the weather services and returns the data in the expected format.

### Implementing Custom Authentication 

With Graphcool, different authentication mechanisms can easily be implemented with serverless functions as well. When doing so, you can use Schema Extensions to add the required functionality. The next chapter explains the idea behind this approach in more details.

> [Here](https://github.com/graphcool-examples/functions/tree/master/authentication/facebook-authentication) is an example that demonstrates how you can implement a Facebook login in your app. 







