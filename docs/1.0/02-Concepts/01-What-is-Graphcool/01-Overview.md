---
alias: apohpae9ju 
description: An overview of Graphcool.
---

# What is Graphcool

## Overview

### tldr

Graphcool is a _GraphQL database abstraction layer_ that turns your database into a GraphQL API. Rather than directly writing SQL or using a NoSQL API (like MongoDB), you get to interact with your data using GraphQL:

* **Powerful CRUD GraphQL API**: Reading and writing data is done with GraphQL queries and mutations (including out-of-the-box realtime support with GraphQL subscriptions)
* **Flexible data modelling & migrations**: Define your data model and schema migrations using GraphQL's expressive [schema definition language](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51) (SDL)
* **Rich ecosystem and helpful tooling**: The GraphQL ecosystem is growing rapidly and open source tools like [GraphQL Playground](https://github.com/graphcool/graphql-playground) and [schema stitching](https://blog.graph.cool/graphql-schema-stitching-explained-schema-delegation-4c6caf468405) are notably improving workflows and overall developer experience

### What does a Graphcool-powered backend look like?

Building a GraphQL server with Graphcool involves four major steps:

1. Define the **data model** of your application in GraphQL SDL
1. Deploy your GraphQL database → Graphcool generates the **database schema** (CRUD)
1. Define your **application schema** that determines the API exposed to your client applications
1. Implement API using resolvers & **Graphcool binding**

The application schema can be thought of as a “mask” for the database schema. It is used to *tailor* an API that matches your application's needs rather than exposing full CRUD capabilities to all clients.

### GraphQL databases and schema bindings for building idiomatic GraphQL servers

Graphcool is often, somewhat inaccurately, referred to as a “GraphQL database”. Note that at a high-level, a database has two major roles:

* enable persistence through some kind of **data store**
* enable **data access** by providing an API to the store

In that sense, Graphcool is not a database because it only provides a data access layer - but does not have a built-in data store! The term “GraphQL database” therefore refers more to the _architectural role_ of Graphcool in a server-side setup.

### How does Graphcool compare to Databases, like MongoDB, MySQL and DynamoDB?

As mentioned, Graphcool is not a database but a _data access layer_ that works with _any_ database. It is thus database agnostic.

The key distinction between Graphcool and databases in general is that Graphcool does not implement the persistence layer. Data is still stored in a conventional database (like MySQL, MongoDB or DynamoDB) under the hood - only the way how it is managed, retrieved and updated is simpler and comes along with all the benefits of GraphQL.

<InfoBox type=info>

As of today, Graphcool in fact runs on a relational database powered my MySQL - in the future it will be possible to use Graphcool with any database as a data store.

</InfoBox>

### Open source & Graphcool Cloud

Graphcool is free and entirely [open source](https://github.com/graphcool/graphcool). You can run Graphcool in production on custom infrastructure or using your preferred IaaS provider (such as AWS, Microsoft Azure and Google Cloud).

To make it easier to operate Graphcool in production, **Graphcool Cloud** offers a set of features to save your time in areas like the following:

* infrastructure management and cloud control
* automatic backups
* analytics
* team collaboration
* devops and hosting (even on private infrastructure, for example with your own AWS account).

Note that the **Graphcool Cloud also has a free development cluster** allowing you to instantly deploy your Graphcool services to the web during development or for testing and demo purposes.