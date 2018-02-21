---
alias: apohpae9ju 
description: An overview of Prisma.
---

# What is Prisma

## Overview

### tldr

Prisma is a _GraphQL database abstraction layer_ that turns your database into a GraphQL API. Rather than directly writing SQL or using a NoSQL API (like MongoDB), you get to interact with your data using GraphQL:

* **Powerful CRUD GraphQL API**: Reading and writing data is done with GraphQL queries and mutations (including out-of-the-box realtime support with GraphQL subscriptions)
* **Flexible data modeling & migrations**: Define your data model and schema migrations using GraphQL's expressive [schema definition language](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51) (SDL)
* **Rich ecosystem and helpful tooling**: The GraphQL ecosystem is growing rapidly and open source tools like [GraphQL Playground](https://github.com/Prisma/graphql-playground) and [GraphQL bindings](https://blog.graph.cool/graphql-schema-stitching-explained-schema-delegation-4c6caf468405) are notably improving workflows and overall developer experience

### What does a Prisma-powered GraphQL server look like?

<InfoBox>

If you have never built a GraphQL server before, please read [this tutorial](https://blog.graph.cool/6da86f346e68) before moving on.
  
</InfoBox>

When building a GraphQL server with Prisma, you are dealing with two GraphQL APIs that represent two separate layers inside your backend:

- The **application layer** contains the business logic for your application and implements other common workflows such as authentication or integration of 3rd party services.
- The **Prisma database layer** provides a powerful, auto-generated CRUD API.

Both layers are glued together using [**Prisma bindings**](https://github.com/graphcool/prisma-binding).

### Building a GraphQL server with Prisma

Building a GraphQL server with Prisma involves four major steps:

1. Define the **data model** for your application in GraphQL SDL
1. Deploy your Prisma database service → Prisma generates the **database schema** (CRUD)
1. Define your **application schema** that determines the API exposed to your client applications
1. Implement API using resolvers & [**Prisma binding**](https://github.com/graphcool/prisma-binding)

> The database schema is the foundation for the database layer, the application schema defines the API of the application layer.

The application schema can be thought of as a "mask" for the database schema. It is used to _tailor_ an API that matches your application's needs rather than exposing full CRUD capabilities to all clients. (You usually don't want to expose your entire database to your client applications.)

## Architecture

### Prisma connects your web server with the database

Considering the classic [three-tier architecture](https://en.wikipedia.org/wiki/Multitier_architecture#Three-tier_architecture) of client, (web) server and database, Prisma is a layer which _connects your database with the server_. In that sense, it can somewhat be thought of as an [ORM](https://en.wikipedia.org/wiki/Object-relational_mapping), but comes with many additional benefits compared to conventional ORMs - most importantly: it enables an idiomatic way of building GraphQL servers!

When implementing your GraphQL web server, you can use [GraphQL bindings](https://blog.graph.cool/reusing-composing-graphql-apis-with-graphql-bindings-80a4aa37cff5), and in particular the [`prisma-binding`](https://github.com/graphcool/prisma-binding) package, to conveniently implement your Prisma resolvers. In short, `prisma-binding` allows to simply _forward_ incoming queries to Prisma's powerful CRUD API.

### Architectural components

When working with Prisma, you typically have the following components in your stack:

* Client apps (often using a GraphQL client, like [Relay](https://facebook.github.io/relay/) or [Apollo](https://github.com/apollographql/apollo-client))
* GraphQL web server (based on [`graphql-yoga`](https://github.com/graphcool/graphql-yoga))
* Prisma & Prisma bindings
* Database

![](https://imgur.com/QJIcNRm.png)

**Client applications** are the apps that will end up in the hands of your users. This can be a web app written with React, Angular or any other framework or a native mobile app for Android or iOS.

The **GraphQL web server** is the _server_ from the three-tier architecture. It is responsible for business logic and other common pieces of functionality, like authentication and permissions. The GraphQL server exposes a GraphQL API that's defined in the **application schema**.

**Prisma** is the glue between your database and the GraphQL web server.

The **database** provides the actual persistence layer for your backend.

### GraphQL databases and GraphQL bindings for building idiomatic GraphQL servers

Prisma is sometimes, somewhat inaccurately, referred to as a “GraphQL database”. Note that at a high-level, a database has two major roles:

* Enable persistence through some kind of **data store**.
* Enable **data access** by providing an API to the store.

In that sense, Prisma is not a database because it only provides a data access layer - but does not have a built-in data store! The term "GraphQL database" therefore refers more to the _architectural role_ of Prisma in a server-side setup.

## Open source & Prisma Cloud

Prisma is free and entirely [open source](https://github.com/prisma/prisma). You can run Prisma in production on custom infrastructure or using your preferred IaaS provider (such as AWS, Digital Ocean, Microsoft Azure and Google Cloud).

To make it easier to operate Prisma in production, **Prisma Cloud** offers a set of features to save your time in areas like the following:

* Infrastructure management & Cloud control
* Automatic backups
* Analytics & Performance monitoring
* Team management & collaboration
* DevOps and hosting (even on private infrastructure, for example with your own AWS account or inside an AWS VPC).

Note that the **Prisma Cloud also has a free public cluster** allowing you to instantly deploy your Prisma services to the web.
