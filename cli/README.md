<p align="center"><a href="https://www.prisma.io"><img src="https://i.imgur.com/wD4rVt4.png" alt="Prisma" height="160px"></a></p>

[Website](https://www.prisma.io) • [Docs](https://www.prisma.io/docs/) • [Blog](https://www.prisma.io/blog) • [Forum](https://www.prisma.io/forum) • [Slack](https://slack.prisma.io/) • [Twitter](https://twitter.com/prisma) • [OSS](https://oss.prisma.io/) • [Learn](https://www.howtographql.com)

[![CircleCI](https://circleci.com/gh/prismagraphql/prisma.svg?style=shield)](https://circleci.com/gh/prismagraphql/prisma) [![Slack Status](https://slack.prisma.io/badge.svg)](https://slack.prisma.io) [![npm version](https://badge.fury.io/js/prisma.svg)](https://badge.fury.io/js/prisma)

**Prisma is a performant open-source GraphQL [ORM-like layer](#is-prisma-an-orm)** doing the heavy lifting in your GraphQL server. It turns your database into a GraphQL API which can be consumed by your resolvers via [GraphQL bindings](https://oss.prisma.io/content/graphql-binding/01-overview).

Prisma's auto-generated GraphQL API provides powerful abstractions and modular building blocks to develop flexible and scalable GraphQL backends:

- **Type-safe API** including filters, aggregations, pagination and transactions.
- **Data modeling & migrations** with declarative GraphQL SDL.
- **Realtime API** using GraphQL subscriptions.
- **Advanced API composition** using GraphQL bindings and schema stitching.
- **Works with all frontend frameworks** like React, Vue.js, Angular.

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

[Watch this 3-min tutorial](https://www.youtube.com/watch?v=CORQo5rooX8) or follow the steps below to get started with Prisma.

#### 1. Install the CLI via NPM

```bash
npm install -g prisma
```

#### 2. Create a new Prisma service

Run the following command to create the files you need for a new Prisma [service](https://www.prisma.io/docs/reference/service-configuration/overview-ieshoo5ohm).

```bash
prisma init hello-world
```

Then select the **Demo server** (hosted in Prisma Cloud) and follow the instructions of the interactive CLI prompt.

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

Edit `datamodel.prisma` to define your data model using GraphQL SDL:

```graphql
type Tweet {
  id: ID! @unique
  createdAt: DateTime!
  text: String!
  owner: User!
}

type User {
  id: ID! @unique
  handle: String! @unique
  name: String!
  tweets: [Tweet!]!
}
```

#### 4. Deploy your Prisma service

To deploy your service, run the following command:

```bash
prisma deploy
```

#### 5. Explore the API in a Playground

Run the following command to open a [GraphQL Playground](https://github.com/prismagraphql/graphql-playground/releases) and start sending queries and mutations:

```bash
prisma playground
```

<details><summary><b>I don't know what queries and mutations I can send.</b></summary>
<p>

**Create a new user**:

```graphql
mutation {
  createUser(
    data: {
      name: "Alice"
      handle: "alice"
    }
  ) {
    id
  }
}
```

**Query all users and their tweets**:

```graphql
query {
  users {
    id
    name
    tweets {
      id
      createdAt
      text
    }
  }
}
```

**Create a new tweet for a user**:

> Replace the `__USER_ID__` placeholder with the `id` of an actual `User`

```graphql
mutation {
  createTweet(
    data: {
      text: "Prisma makes building GraphQL servers fun & easy"
      owner: {
        connect: {
          id: "__USER_ID__"
        }
      }
    }
  ) {
    id
    createdAt
    owner {
      name
    }
  }
}
```

</p>
</details>

#### 6. Next steps

You can now connect to Prisma's GraphQL API, select what you would like to do next:

- [**Build a GraphQL server (recommended)**](https://www.prisma.io/docs/tutorials/-ohdaiyoo6c)
- Access Prisma's GraphQL API from a Node script (_coming soon_)
- Access Prisma's GraphQL API directly from the frontend (_coming soon_)

## Examples

Check out the [`prisma-examples`](https://github.com/prisma/prisma-examples) 💡 containing a collection of Prisma example projects. You can also check the [**AirBnB clone example**](https://github.com/prismagraphql/graphql-server-example) we built as a fully-featured demo app for Prisma.

## Architecture

Prisma takes the role of a [data access layer](https://en.wikipedia.org/wiki/Data_access_layer) in your backend architecture by connecting your API server to your databases. It enables a layered architecture which leads to better _separation of concerns_ and improves _maintainability_ of the entire backend.

Acting as a _GraphQL database proxy_, Prisma provides a GraphQL-based abstraction for your databases enabling you to read and write data with GraphQL queries and mutations. Using [Prisma bindings](https://github.com/prisma/prisma-binding), you can access Prisma's GraphQL API from your programming language.

Prisma servers run as standalone processes which allows for them to be scaled independently from your API server.

<!-- Prisma is a secure API layer that sits in front of your database. Acting as a _GraphQL database proxy_, Prisma exposes a powerful GraphQL API and manages rate limiting, authentication, logging and a host of other features. Because Prisma is a standalone process, it can be scaled independently from your application layer and provide scalable subscriptions infrastructure. -->

<p align="center"><img src="https://i.imgur.com/vVaq6yq.png" height="250" /></p>

## Is Prisma an ORM?

Prisma provides a mapping from your API to your database. In that sense, it solves similar problems as conventional ORMs. The big difference between Prisma and other ORMs is the way _how_ the mapping is implemented.

**Prisma takes a radically different approach which avoids the shortcomings and limitations commonly experienced with ORMs.** The core idea is that Prisma turns your database into a GraphQL API which is then consumed by your API server (via [GraphQL binding](https://oss.prisma.io/content/graphql-binding/01-overview)). While this makes Prisma particularly well-suited for building GraphQL servers, it can definetely be used in other contexts as well.

Here is how Prisma compares to conventional ORMs:

- **Expressiveness**: Full flexibility thanks to Prisma's GraphQL API, including relational filters and nested mutations.
- **Performance**: Prisma uses various optimization techniques to ensure top performance in complex scenarios.
- **Architecture**: Using Prisma enables a layered and clean architecture, allowing you to focus on your API layer.
- **Type safety**: Thanks to GraphQL's strong type system you're getting a strongly typed API layer for free.
- **Realtime**: Out-of-the-box support for realtime updates for all events happening in the database.

## Database Connectors

[Database connectors](https://github.com/prisma/prisma/issues/1751) provide the link between Prisma and the underlying database.

You can connect the following databases to Prisma already:

- MySQL
- Postgres

More database connectors will follow.

### Upcoming Connectors

If you are interested to participate in the preview for one of the following connectors, please reach out in our [Slack](https://slack.prisma.io).

* [MongoDB Connector](https://github.com/prisma/prisma/issues/1643)
* [Elastic Search Connector](https://github.com/prisma/prisma/issues/1665)

### Further Connectors

We are still collecting use cases and feedback for the API design and feature set of the following connectors:

* [MS SQL Connector](https://github.com/prisma/prisma/issues/1642)
* [Oracle Connector](https://github.com/prisma/prisma/issues/1644)
* [ArangoDB Connector](https://github.com/prisma/prisma/issues/1645)
* [Neo4j Connector](https://github.com/prisma/prisma/issues/1646)
* [Druid Connector](https://github.com/prisma/prisma/issues/1647)
* [Dgraph Connector](https://github.com/prisma/prisma/issues/1648)
* [DynamoDB Connector](https://github.com/prisma/prisma/issues/1655)
* [Cloud Firestore Connector](https://github.com/prisma/prisma/issues/1660)
* [CockroachDB Connector](https://github.com/prisma/prisma/issues/1705)
* [Cassandra Connector](https://github.com/prisma/prisma/issues/1750)
* [Redis Connector](https://github.com/prisma/prisma/issues/1722)
* [AWS Neptune Connector](https://github.com/prisma/prisma/issues/1752)
* [CosmosDB Connector](https://github.com/prisma/prisma/issues/1663)
* [Influx Connector](https://github.com/prisma/prisma/issues/1857)

Join the discussion or contribute to influence which we'll work on next!

## GraphQL API

The most important component in Prisma is the GraphQL API:

* Query, mutate & stream data via a auto-generated GraphQL CRUD API
* Define your data model and perform migrations using GraphQL SDL

Prisma's auto-generated GraphQL APIs are fully compatible with the [OpenCRUD](https://www.opencrud.org/) standard.

> [Try the online demo!](https://www.prisma.io/features/graphql-api/)

## Community

Prisma has a community of thousands of amazing developers and contributors. Welcome, please join us! 👋

- [Forum](https://www.prisma.io/forum)
- [Slack](https://slack.prisma.io/)
- [Twitter](https://twitter.com/prisma)
- [Facebook](https://www.facebook.com/prisma.io)
- [Meetup](https://www.meetup.com/graphql-berlin)
- [GraphQL Europe](https://www.graphql-europe.org/) (June 15, Berlin)
- [GraphQL Day](https://www.graphqlday.org/)
- [Email](mailto:hello@prisma.io)

## Contributing

Contributions are **welcome and extremely helpful** 🙌
Please refer [to the contribution guide](https://github.com/prisma/prisma/blob/master/CONTRIBUTING.md) for more information.

Releases are separated into two _channels_ - the **stable** and **unstable** channel.

* The stable channel is released every two weeks, incrementing the minor version number. Irregular releases in between minor releases can occur and increment the patch version.

* The unstable channel is released with every commit to master and therefore gives access to features and bug fixes before the stable release. You can find more information about running the Prisma on the unstable channel [here](https://github.com/prisma/prisma/blob/master/CONTRIBUTING.md#the-unstable-channel).

<p align="center"><a href="https://oss.prisma.io"><img src="https://imgur.com/IMU2ERq.png" alt="Prisma" height="170px"></a></p>
