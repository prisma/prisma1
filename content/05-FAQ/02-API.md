---
alias: eep0ugh1wa
description: Frequently asked questions about the API that's provided by Graphcool.
---

# API

### What is the difference between the Simple API and the Relay API?

* Relay API adheres to Relay specification
* Simple API offers intuitive CRUD capabilities (+ supports Subscriptions)

How does Error Handling work with Graphcool?

* Graphcool implements error-handling according to the GraphQL specification, i.e. if an operation went wrong a dedicated errors array will be returned with information about the failure
* HTTP status codes are not revelant for error handling - will always be 200

### Does Graphcool support realtime functionality?

* yes, with GraphQL subscriptions

### Does Graphcool support offline apps?

* Graphcool is only a server-side framework, offline capabilities is primarily a problem that needs to be solved on the frontend
* some approaches with Apollo Client 

### Can I Integrate 3rd party apis or my existing microservices with graphcool?

* yes, Graphcool allows to easily call out to other systems with the following mechanisms:
    * custom resolvers
    * event-based subscription system

### Can I change the auto-generated GraphQL schema that defines my aPI?

* not directly, but you can define a custom API (subset) with a proxy layer

### What is the difference between the db schema and the graphql schema?

* DB schema is the foundation for GraphQL schema (GraphQL schema is generated from DB schema)
* GraphQL schema defines the actual API

### Some graphql tooling I'm using requires the full graphql schema, How Can I get access to it?

* with the graphql-cli

