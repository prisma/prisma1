<a href="https://www.prisma.io"><img src="https://imgur.com/HUu10rH.png" width="248" /></a>

[Website](https://www.prisma.io) • [Docs](https://www.prisma.io/docs/) • [Blog](https://blog.prisma.io/) • [Forum](https://www.prisma.io/forum) • [Slack](https://slack.prisma.io/) • [Twitter](https://twitter.com/graphcool)

[![CircleCI](https://circleci.com/gh/graphcool/prisma.svg?style=shield)](https://circleci.com/gh/graphcool/prisma) [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://badge.fury.io/js/prisma.svg)](https://badge.fury.io/js/prisma)

**Prisma is a performant open-source GraphQL [ORM-like layer](#is-prisma-an-orm)** doing the heavy lifting in your GraphQL server. It turns your database into a GraphQL API to be consumed from your resolvers via [GraphQL bindings](https://new.prisma.io/).

The Prisma GraphQL API provides powerful abstractions and building blocks to develop flexible, scalable GraphQL backends:

1. **Type-safe API** that can be used from frontend and backend, including filters, aggregations and transactions.
2. **Data modeling** with declarative SDL. Prisma migrates your underlying database automatically.
3. **Realtime API** using GraphQL Subscriptions.
4. **Advanced API composition** using GraphQL Bindings and schema stitching.
5. **Works with all frontend frameworks** like React, Vue.js, Angular ([Quickstart Examples](https://www.prisma.io/docs/quickstart/)).

## Contents

* [Quickstart](#quickstart)
* [Examples](#examples)
* [Architecture](#architecture)
* [Is Prisma an ORM?](#is-prisma-an-orm)
* [Database Connectors](#database-connectors)
* [GraphQL API](#graphql-api)
* [Community](#community)
* [Contributing](#contributing)

## Quickstart

[Watch this 4 min tutorial](https://www.youtube.com/watch?v=20zGexpEitc) or follow the steps below to get started with Prisma:

#### 1. Install the CLI via NPM

```bash
npm install -g prisma
```

#### 2. Create a new Prisma service

Run the following command to create the files you need for a new Prisma [service](https://www.prisma.io/docs/reference/service-configuration/overview-ieshoo5ohm).

```bash
prisma init hello-world
```

Then select the **Demo server** and follow the instructions of the interactive CLI prompt.

<details><summary><b>Alternative: Setup Prisma with your own database.</b></summary>
<p>

Instead of using a Demo server, you can also setup a Prisma server that is connected to your own database. Note that this **requires [Docker](https://www.docker.com)**.

To do so, run `prisma init` as shown above and follow the interactive CLI prompts to choose your own database setup:

- Create a new database
- Connect an existing database

Once the command has finished, you need to run `docker-compose up -d` to start the Prisma server.

</p>
</details>

#### 3. Define your data model

Edit `datamodel.graphql` to define your data model using the [GraphQL SDL notation](https://www.prisma.io/docs/reference/service-configuration/data-modelling-(sdl)-eiroozae8u).

```graphql
type Tweet {
  id: ID! @unique
  createdAt: DateTime!
  text: String!
  owner: User!
  location: Location
}

type User {
  id: ID! @unique
  createdAt: DateTime!
  updatedAt: DateTime!
  handle: String! @unique
  name: String!
  tweets: [Tweet!]!
}

type Location {
  latitude: Float!
  longitude: Float!
}
```

#### 4. Deploy your Prisma service

To deploy your service, run the following command:

```bash
prisma deploy
```

#### 5. Explore the API in a Playground

Run the following command to open a GraphQL Playground:

```bash
prisma playground
```

You can now send queries and mutations to the API.

<details><summary><b>I don't know what queries and mutations I can send.</b></summary>
<p>

**Create a new User**:

```graphql
mutation {
  createUser(
    name: "Alice"
    handle: "alice"
  ) {
    id
  }
}
```



</p>
</details>

#### 7. Next steps

- **Build a GraphQL server (recommended)**

## Examples

- [demo-application](https://github.com/graphcool/graphql-server-example)
- [auth](examples/auth)
- [file-handling-s3](examples/file-handling-s3)
- [github-auth](examples/github-auth)
- [permissions](examples/permissions)
- [resolver-forwarding](examples/resolver-forwarding)
- [subscriptions](examples/subscriptions)

## Architecture

Prisma is a secure API layer that sits in front of your database. Acting as a "GraphQL database proxy", Prisma exposes a powerful GraphQL API and manages rate-limiting, authentication, logging and a host of other features. Because Prisma is a standalone process, it can be scaled independently from your application layer and provide scalable subscriptions infrastructure.

![](https://imgur.com/SdssPgT.png)

## Is Prisma an ORM?

Prisma provides a mapping from your API to your database. In that sense, it solves similar problems as conventional ORMs. The big difference between Prisma and other ORMs is the way _how_ the mapping is implemented.

**Prisma takes a radically different approach which avoids the shortcomings and limitations commonly experienced with ORMs.** The core idea is that Prisma turns your database into a GraphQL API which is then consumed by your API server (via [GraphQL binding](https://oss.prisma.io/graphql-binding)). While this makes Prisma particularly well-suited for building GraphQL servers, it can definetely be used in other contexts as well.

Here is how Prisma compares to conventional ORMs:

- **Expressiveness**: Full flexibility thanks to Prisma's GraphQL API, including relational filters and nested mutations
- **Performance**: Prisma uses various optimization techniques to ensure top performance in complex scenarios
- **Architecture**: Using Prisma enables a layered and clean architecture, allowing you to focus on your API layer
- **Type safety**: Thanks to GraphQL's strong type system you're getting a strongly typed API layer for free
- **Realtime**: Out-of-the-box support for realtime updates for all events happening in the database

## Database Connectors

[Database connectors](https://github.com/graphcool/prisma/issues/1751) provide the link between Prisma and the underlying database.

You can connect the following databases to Prisma already:

* MySQL
* Postgres

More database connectors will follow.

### Upcoming Connectors

If you are interested to participate in the preview for one of the following connectors, please reach out in our [Slack](https://slack.graph.cool).

* [MongoDB Connector](https://github.com/graphcool/prisma/issues/1643)
* [Elastic Search Connector](https://github.com/graphcool/prisma/issues/1665)

### Further Connectors

We are still collecting use cases and feedback for the API design and feature set of the following connectors:

* [MS SQL Connector](https://github.com/graphcool/prisma/issues/1642)
* [Oracle Connector](https://github.com/graphcool/prisma/issues/1644)
* [ArangoDB Connector](https://github.com/graphcool/prisma/issues/1645)
* [Neo4j Connector](https://github.com/graphcool/prisma/issues/1646)
* [Druid Connector](https://github.com/graphcool/prisma/issues/1647)
* [Dgraph Connector](https://github.com/graphcool/prisma/issues/1648)
* [DynamoDB Connector](https://github.com/graphcool/prisma/issues/1655)
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

Try the online demo: [open GraphQL Playground](https://www.prisma.io/features)

## Community

Prisma has a community of thousands of amazing developers and contributors. Welcome, please join us! 👋

* [Forum](https://www.graph.cool/forum)
* [Slack](https://slack.graph.cool/)
* [Twitter](https://twitter.com/graphcool)
* [Facebook](https://www.facebook.com/GraphcoolHQ)
* [Meetup](https://www.meetup.com/graphql-berlin)
* [Email](hello@graph.cool)

## Contributing

Contributions are **welcome and extremely helpful** 🙌
Please refer [to the contribution guide](https://github.com/graphcool/prisma/blob/master/CONTRIBUTING.md) for more information.

Releases are separated into two _channels_ - the **stable** and **unstable** channel.

* The stable channel is released every two weeks, incrementing the minor version number. Irregular releases in between minor releases can occur and increment the patch version.

* The unstable channel is released with every commit to master and therefore gives access to features and bug fixes before the stable release. You can find more information about running the Prisma on the unstable channel [here](https://github.com/graphcool/prisma/blob/master/CONTRIBUTING.md#the-unstable-channel).
