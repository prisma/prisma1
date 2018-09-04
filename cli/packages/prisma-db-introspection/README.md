<a href="https://www.prismagraphql.com"><img src="https://imgur.com/HUu10rH.png" width="248" /></a>

[Website](https://www.prismagraphql.com) • [Docs](https://www.prismagraphql.com/docs/) • [Blog](https://blog.graph.cool/) • [Forum](https://www.graph.cool/forum) • [Slack](https://slack.graph.cool/) • [Twitter](https://twitter.com/graphcool)

[![CircleCI](https://circleci.com/gh/graphcool/prisma.svg?style=shield)](https://circleci.com/gh/graphcool/prisma) [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://badge.fury.io/js/prisma.svg)](https://badge.fury.io/js/prisma)

**Prisma - turn your database into a GraphQL API**. Prisma lets you design your data model and have a production ready [GraphQL](https://www.howtographql.com/) API online in minutes.

The Prisma GraphQL API provides powerful abstractions and building blocks to develop flexible, scalable GraphQL backends:

1. **Type-safe API** that can be used from frontend and backend, including filters, aggregations and transactions.
2. **Data modeling** with declarative SDL. Prisma migrates your underlying database automatically.
3. **Realtime API** using GraphQL Subscriptions.
4. **Advanced API composition** using GraphQL Bindings and schema stitching.
5. **Works with all frontend frameworks** like React, Vue.js, Angular ([Quickstart Examples](https://www.prismagraphql.com/docs/quickstart/)).

## Contents

<!--
<img align="right" width="400" src="https://imgur.com/EsopgE3.gif" />
-->

* [Quickstart](#quickstart)
* [Examples](#examples)
* [Architecture](#architecture)
* [Supported Databases](#supported-databases)
* [GraphQL API](#graphql-api)
* [Community](#community)
* [Contributing](#contributing)

## Quickstart

[Watch this 4 min tutorial](https://www.youtube.com/watch?v=20zGexpEitc) or follow the steps below to get started with Prisma:

1. **Install the CLI via NPM:**

```console
npm install -g prisma
```

2. **Create a new service:**

The following command creates all files you need for a new [service](https://www.prismagraphql.com/docs/reference/service-configuration/overview-ieshoo5ohm).

```console
prisma init
```

3. **Define your data model:**

Edit `datamodel.prisma` to define your data model using the [GraphQL SDL notation](<https://www.prismagraphql.com/docs/reference/service-configuration/data-modelling-(sdl)-eiroozae8u>).

```graphql
type Tweet {
  id: ID! @unique
  createdAt: DateTime!
  text: String!
  owner: User!
  location: Location!
}

type User {
  id: ID! @unique
  createdAt: DateTime!
  updatedAt: DateTime!
  handle: String! @unique
  name: String
  tweets: [Tweet!]!
}

type Location {
  latitude: Float!
  longitude: Float!
}
```

5. **Deploy your service:**

To deploy your service simply run the following command and select one of the hosted development clusters or setup a local Docker-based development environment:

```console
prisma deploy
```

6. **Connect to your GraphQL endpoint:**

Use the endpoint from the previous step in your frontend (or backend) applications to connect to your GraphQL API.

7. **Read more in the dedicated quickstarts for your favorite technology**

[![](https://imgur.com/T5nakij.png)](https://www.prismagraphql.com/docs/quickstart/)

## Examples

* [demo-application](https://github.com/graphcool/graphql-server-example)
* [auth](examples/auth)
* [file-handling-s3](examples/file-handling-s3)
* [github-auth](examples/github-auth)
* [permissions](examples/permissions)
* [resolver-forwarding](examples/resolver-forwarding)
* [subscriptions](examples/subscriptions)

## Architecture

Prisma is a secure API layer that sits in front of your database. Acting as a proxy, Prisma exposes a powerful GraphQL API and manages Rate-Limiting, Authentication, Logging and a host of other features. Because Prisma is a standalone process, it can be scaled independently from your application layer and provide scalable subscriptions infrastructure.

![](https://imgur.com/SdssPgT.png)

## Supported Databases

Prisma can be used for MySQL Databases out of the box. More database connectors will follow

* [PostgreSQL Connector](https://github.com/graphcool/prisma/issues/1641)
* [MS SQL Connector](https://github.com/graphcool/prisma/issues/1642)
* [MongoDB Connector](https://github.com/graphcool/prisma/issues/1643)
* [Oracle Connector](https://github.com/graphcool/prisma/issues/1644)
* [ArangoDB Connector](https://github.com/graphcool/prisma/issues/1645)
* [Neo4j Connector](https://github.com/graphcool/prisma/issues/1646)
* [Druid Connector](https://github.com/graphcool/prisma/issues/1647)
* [Dgraph Connector](https://github.com/graphcool/prisma/issues/1648)
* [DynamoDB Connector](https://github.com/graphcool/prisma/issues/1655)
* [Elastic Search Connector](https://github.com/graphcool/prisma/issues/1665)
* [Cloud Firestore Connector](https://github.com/graphcool/prisma/issues/1660)
* [CockroachDB Connector](https://github.com/graphcool/prisma/issues/1705)
* [Cassandra Connector](https://github.com/graphcool/prisma/issues/1750)
* [Redis Connector](https://github.com/graphcool/prisma/issues/1722)
* [AWS Neptune Connector](https://github.com/graphcool/prisma/issues/1752)
* [CosmosDB Connector](https://github.com/graphcool/prisma/issues/1663)
* [Influx Connector](https://github.com/graphcool/prisma/issues/1857)

Join the discussion or contribute to influence which we'll work on next!

## GraphQL API

The most important component in Prisma is the GraphQL API:

* Query, mutate & stream data via GraphQL CRUD API
* Define and evolve your data model using GraphQL SDL

Try the online demo: [open GraphQL Playground](https://www.prismagraphql.com/features)

## Community

Prisma has a community of thousands of amazing developers and contributors. Welcome, please join us! 👋

* [Forum](https://www.graph.cool/forum)
* [Slack](https://slack.graph.cool/)
* [Twitter](https://twitter.com/graphcool)
* [Facebook](https://www.facebook.com/GraphcoolHQ)
* [Meetup](https://www.meetup.com/graphql-berlin)
* [Email](hello@graph.cool)

## Contributing

Contributions are **welcome and extremely helpful**
Please refer [to the contribution guide](https://github.com/graphcool/prisma/blob/master/CONTRIBUTING.md) for more information.
