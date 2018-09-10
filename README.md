<p align="center"><a href="https://www.prisma.io"><img src="https://i.imgur.com/wD4rVt4.png" alt="Prisma" height="160px"></a></p>

[Website](https://www.prisma.io) • [Docs](https://www.prisma.io/docs/) • [Blog](https://www.prisma.io/blog) • [Forum](https://www.prisma.io/forum) • [Slack](https://slack.prisma.io/) • [Twitter](https://twitter.com/prisma) • [OSS](https://oss.prisma.io/) • [Learn](https://www.howtographql.com)

[![CircleCI](https://circleci.com/gh/prisma/prisma.svg?style=shield)](https://circleci.com/gh/prismagraphql/prisma) [![Slack Status](https://slack.prisma.io/badge.svg)](https://slack.prisma.io) [![npm version](https://badge.fury.io/js/prisma.svg)](https://badge.fury.io/js/prisma)

**Prisma the data layer for modern applications**. It replaces traditional ORMs and custom data access layers by providing a universal database abstraction that is used via the Prisma client.

- **Prisma client for various languages** such as JavaScript, TypeScript,Flow, Go.
- **Supports multiple databases** such as MySQL, PostgreSQL, MongoDB, ... ([see all supported databases](https://www.prisma.io/features/databases/))
- **Type-safe database access** including filters, aggregations, pagination and transactions.
- **Realtime event systems for your database** to get notified about database events.
- **Declarative data modeling & migrations (optional)** with simple SDL syntax.

## Contents

- [Quickstart](#quickstart)
- [Examples](#examples)
- [Architecture](#architecture)
- [Is Prisma an ORM?](#is-prisma-an-orm)
- [Database Connectors](#database-connectors)
- [GraphQL API](#graphql-api)
- [Community](#community)
- [Contributing](#contributing)

## Quickstart

### 1. Install Prisma via Homebrew

```
brew install prisma
```

<Details>
<Summary><b>Alternative: Install with NPM or Yarn</b></Summary>

```
npm install -g prisma
# or
yarn global add prisma
```
</Details>

### 2. Connect Prisma to a database

To setup Prisma, you need to have [Docker](https://www.docker.com) installed. Run the following command to get started with Prisma:

```
prisma init hello-world
```

The interactice CLI wizard now helps you to connect Prisma to your database:

- If you want to start with a new database, select **Create new database**.
- If you already have a database, select **Use existing database** (and provide database credentials).

After you provided the required database information to the wizard, it prompts you to select the language for your Prisma client.

Once the wizard has terminated, run the following commands to setup Prisma:

```
cd hello-world
docker-compose up -d
```

<Details>
<Summary><b>Alternative: Use Prisma in a sandbox without Docker</b></Summary>

```
prisma init hello-world
```

Select the **Demo server** and follow the instructions of the interactive CLI prompt. Note that this requires you to authenticate with [Prisma Cloud](https://www.prisma.io/cloud) as this is wehre the Demo server is hosted.

</Details>

### 3. Deploy your Prisma API

To deploy your Prisma API, run the following command:

```
prisma deploy
```

This creates a 

### 5. Explore the API in a Playground

Run the following command to open a [GraphQL Playground](https://github.com/prismagraphql/graphql-playground/releases) and start sending queries and mutations:

```
prisma playground
```

<details><summary><b>I don't know what queries and mutations I can send.</b></summary>
<p>

**Create a new user**:

```graphql
mutation {
  createUser(data: { name: "Alice", handle: "alice" }) {
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
      owner: { connect: { id: "__USER_ID__" } }
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

### 6. Next steps

You can now connect to Prisma's GraphQL API, select what you would like to do next:

- [**Build a GraphQL server (recommended)**](https://www.prisma.io/docs/tutorials/-ohdaiyoo6c)
- Access Prisma's GraphQL API from a Node script (_coming soon_)
- Access Prisma's GraphQL API directly from the frontend (_coming soon_)

## Examples

Collection of Prisma example projects 💡

- [application-server](./examples/application-server)
- [authentication](./examples/authentication)
- [cli-tool](./examples/cli-tool)
- [data-modelling](./examples/data-modelling)
- [hooks](./examples/hooks)
- [permissions-with-shield](./examples/permissions-with-shield)
- [postgres](./examples/postgres)
- [resolver-forwarding](./examples/resolver-forwarding)
- [server-side-subscriptions](./examples/server-side-subscriptions)
- [subscriptions](./examples/subscriptions)
- [travis](./examples/travis)
- [yml-structure](./examples/yml-structure)

You can also check the [**AirBnB clone example**](https://github.com/prismagraphql/graphql-server-example) we built as a fully-featured demo app for Prisma.

## Architecture

Prisma takes the role of a [data access layer](https://en.wikipedia.org/wiki/Data_access_layer) in your backend architecture by connecting your API server to your databases. It enables a layered architecture which leads to better _separation of concerns_ and improves _maintainability_ of the entire backend.

Acting as a _GraphQL database proxy_, Prisma provides a GraphQL-based abstraction for your databases enabling you to read and write data with GraphQL queries and mutations. Using [Prisma bindings](https://github.com/prismagraphql/prisma-binding), you can access Prisma's GraphQL API from your programming language.

Prisma servers run as standalone processes which allows for them to be scaled independently from your API server.

<!-- Prisma is a secure API layer that sits in front of your database. Acting as a _GraphQL database proxy_, Prisma exposes a powerful GraphQL API and manages rate limiting, authentication, logging and a host of other features. Because Prisma is a standalone process, it can be scaled independently from your application layer and provide scalable subscriptions infrastructure. -->

<p align="center"><img src="https://i.imgur.com/vVaq6yq.png" height="250" /></p>

## Is Prisma an ORM?

Prisma provides a mapping from your API to your database. In that sense, it solves similar problems as conventional ORMs. The big difference between Prisma and other ORMs is _how_ the mapping is implemented.

**Prisma takes a radically different approach which avoids the shortcomings and limitations commonly experienced with ORMs.** The core idea is that Prisma turns your database into a GraphQL API which is then consumed by your API server (via [GraphQL binding](https://oss.prisma.io/content/graphql-binding/01-overview)). While this makes Prisma particularly well-suited for building GraphQL servers, it can definitely be used in other contexts as well.

Here is how Prisma compares to conventional ORMs:

- **Expressiveness**: Full flexibility thanks to Prisma's GraphQL API, including relational filters and nested mutations.
- **Performance**: Prisma uses various optimization techniques to ensure top performance in complex scenarios.
- **Architecture**: Using Prisma enables a layered and clean architecture, allowing you to focus on your API layer.
- **Type safety**: Thanks to GraphQL's strong type system you're getting a strongly typed API layer for free.
- **Realtime**: Out-of-the-box support for realtime updates for all events happening in the database.

## Database Connectors

[Database connectors](https://github.com/prismagraphql/prisma/issues/1751) provide the link between Prisma and the underlying database.

You can connect the following databases to Prisma already:

- MySQL
- Postgres

More database connectors will follow.

### Upcoming Connectors

If you are interested to participate in the preview for one of the following connectors, please reach out in our [Slack](https://slack.prisma.io).

- [MongoDB Connector](https://github.com/prismagraphql/prisma/issues/1643)
- [Elastic Search Connector](https://github.com/prismagraphql/prisma/issues/1665)

### Further Connectors

We are still collecting use cases and feedback for the API design and feature set of the following connectors:

- [MS SQL Connector](https://github.com/prismagraphql/prisma/issues/1642)
- [Oracle Connector](https://github.com/prismagraphql/prisma/issues/1644)
- [ArangoDB Connector](https://github.com/prismagraphql/prisma/issues/1645)
- [Neo4j Connector](https://github.com/prismagraphql/prisma/issues/1646)
- [Druid Connector](https://github.com/prismagraphql/prisma/issues/1647)
- [Dgraph Connector](https://github.com/prismagraphql/prisma/issues/1648)
- [DynamoDB Connector](https://github.com/prismagraphql/prisma/issues/1655)
- [Cloud Firestore Connector](https://github.com/prismagraphql/prisma/issues/1660)
- [CockroachDB Connector](https://github.com/prismagraphql/prisma/issues/1705)
- [Cassandra Connector](https://github.com/prismagraphql/prisma/issues/1750)
- [Redis Connector](https://github.com/prismagraphql/prisma/issues/1722)
- [AWS Neptune Connector](https://github.com/prismagraphql/prisma/issues/1752)
- [CosmosDB Connector](https://github.com/prismagraphql/prisma/issues/1663)
- [Influx Connector](https://github.com/prismagraphql/prisma/issues/1857)

Join the discussion or contribute to influence which we'll work on next!

## GraphQL API

The most important component in Prisma is the GraphQL API:

- Query, mutate & stream data via a auto-generated GraphQL CRUD API
- Define your data model and perform migrations using GraphQL SDL

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
Please refer [to the contribution guide](https://github.com/prismagraphql/prisma/blob/master/CONTRIBUTING.md) for more information.

Releases are separated into three _channels_: **alpha**, **beta** and **stable**. You can learn more about these three channels and Prisma's release process [here](https://www.prisma.io/blog/improving-prismas-release-process-yaey8deiwaex/).

<p align="center"><a href="https://oss.prisma.io"><img src="https://imgur.com/IMU2ERq.png" alt="Prisma" height="170px"></a></p>
