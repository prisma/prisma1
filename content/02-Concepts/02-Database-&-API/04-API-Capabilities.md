# API Capabilities

## CRUD Operations

The foundation for the API are CRUD operations for your Model Types. For each Model Type that you specify, Graphcool will add the corresponding fields to the schema's Root Types, thus enabling you to send queries and mutations to actually perform the CRUD operations.
[Image: https://quip.com/-/blob/VPQAAABNvRV/a3GWFNJOl6lM-W-toepggQ]

## Schema Extensions

Graphcool further allows you to extend the Root Types in your project's GraphQL schema and thus write custom queries and mutations. Each field that you're adding to the `Query` or `Mutation` type needs to backed by a serverless function that implements the expected functionality for that field. 

## Realtime Subscriptions API

As mentioned before, subscriptions allow developer to implement event-based realtime functionality in their apps.

The Graphcool Subscription API can be used for the following three events:

* *created*-mutations
* *updated*-mutations
* *deleted*-mutations

Depending on the event, the data that a client might be interested in widely differs. For example, when a new instance is created, all that can be asked is information about that new instance. When an existing instance is updated, it might be interesting to the client what fields of the instance were changed and what the previous as well as the new values for these fields are. In the case where an instance is deleted, there are only previous values that can be asked as the instance doesn't exist any more.

These points are reflected in the design of the Subscriptions API. When sending a subscription, a client will be able to ask for all the data that's relevant to the event.

As an example, here is a subscription query that specifies a client's interested to be notified about *created*- and *updated*-mutations:

```graphql
subscription {
  Person(filter: {
    mutation_in: [CREATED, UPDATED]
  }) {
    node {
      name
    }
    updatedFields
    previousValues {
      name
    }
  }
}
```

`updatedFields` in the subscription's payload will return an array with the names of the fields that were updated in the case of an *updated*-mutation. `previousValues` contains the old values of the `name` field in case it was changed.

## Simple vs Relay

In general, every Graphcool project will expose two different APIs:

* Simple API: Intuitive CRUD operations and data modelling
* Relay API: Corresponds to the schema requirements of [Relay](https://facebook.github.io/relay/)

Effectively, this means that there are *two* GraphQL Schemas that are backing one Graphcool project.

Relay is Facebook's homegrown GraphQL client that can be used in Javascript applications. The reason why Graphcool creates a dedicated API for it is that Relay makes a couple of assumptions about *how* the GraphQL schema is structured. The Relay API adheres to these assumptions and thus allows you to build a frontend application with Relay. For all other use cases, the Simple API will be the better choice. 

### File Management

Graphcool also has a file management API 😑
