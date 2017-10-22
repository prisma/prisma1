---
alias: abogasd0go
description: Graphcool provides an automatically generated CRUD API for your data model. It also offers a realtime API using GraphQL subscriptions and a dedicated API for file management.
---

# Overview

The basic idea of Graphcool is to provide an automatically generated CRUD API based on a data model that you specify for your service. This API further contains capabilities for filtering, ordering and pagination.

Each Graphcool service further comes with a realtime API that's based on GraphQL subscriptions and allows to react to _events_ that are happening in the system.

Notice that every Graphcool service by default comes with two different APIs:

- **`Simple API`**: Intuitive CRUD operations and data modelling
- **`Relay API`**: Adheres to the schema requirements of [Relay](https://facebook.github.io/relay/)

Notice that both APIs are still accessing the same underlying database!

> Unless you are using Relay for as a GraphQL client, we highly recommend you to always use the `Simple API`. 
