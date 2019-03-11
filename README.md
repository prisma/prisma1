<p align="center"><a href="https://www.prisma.io"><img src="https://i.imgur.com/QgwDieO.png" alt="Prisma" height="230px"></a></p>

[Website](https://www.prisma.io) • [Docs](https://www.prisma.io/docs/) • [Blog](https://www.prisma.io/blog) • [Forum](https://www.prisma.io/forum) • [Slack](https://slack.prisma.io/) • [Twitter](https://twitter.com/prisma) • [OSS](https://oss.prisma.io/) • [Learn](https://www.howtographql.com)

[![CircleCI](https://circleci.com/gh/prisma/prisma.svg?style=shield)](https://circleci.com/gh/prismagraphql/prisma) [![Slack Status](https://slack.prisma.io/badge.svg)](https://slack.prisma.io) [![npm version](https://badge.fury.io/js/prisma.svg)](https://badge.fury.io/js/prisma)

Prisma replaces traditional ORMs and simplifies database workflows: 

- _Access_: **Type-safe database access with the auto-generated Prisma client** (in [JavaScript](https://www.prisma.io/client/client-javscript/), [TypeScript](https://www.prisma.io/client/client-typescript/), [Go](https://www.prisma.io/client/client-go/))
- _Migrate_: **Declarative data modelling and migrations**
- _Manage_: **Visual data management with Prisma Admin**

It is used to build **GraphQL, REST, gRPC APIs** and a lot more. Prisma [currently supports](#database-connectors) MySQL, PostgreSQL, MongoDB.

Try a Prisma example online with CodeSandbox:

[![Prisma Client Demo](https://svgshare.com/i/AVY.svg)](https://codesandbox.io/s/github/prisma-csb/prisma-client-demo-ts) [![GraphQL API](https://svgshare.com/i/AVX.svg)](https://codesandbox.io/s/github/prisma-csb/graphql-example-ts) [![REST API](https://svgshare.com/i/AY0.svg)](https://codesandbox.io/s/github/prisma-csb/rest-example-ts?initialpath=/feed)

## Contents

- [Quickstart](#quickstart)
- [Examples](#examples)
- [Database Connectors](#database-connectors)
- [Community](#community)
- [Contributing](#contributing)

## Quickstart

Get started with Prisma from scratch (or [use your existing database](https://www.prisma.io/docs/-t003/)):

#### 1. Install Prisma via Homebrew

```
brew tap prisma/prisma
brew install prisma
```

<Details>
<Summary><b>Alternative</b>: Install with NPM or Yarn</Summary>

```
npm install -g prisma
# or
yarn global add prisma
```
</Details>

#### 2. Connect Prisma to a database

To setup Prisma, you need to have [Docker](https://www.docker.com) installed. Run the following command to get started with Prisma:

```
prisma init hello-world
```

The interactive CLI wizard now helps you with the required setup:

- Select **Create new database** (you can also use an [existing database](https://www.prisma.io/docs/-t003/) or a hosted [demo database](https://www.prisma.io/docs/-t001/))
- Select the database type: **MySQL** or **PostgreSQL**
- Select the language for the generated Prisma client: **TypeScript**, **Flow**, **JavaScript** or **Go**

Once the wizard has terminated, run the following commands to setup Prisma:

```
cd hello-world
docker-compose up -d
```

#### 3. Define your datamodel

Edit `datamodel.prisma` to define your datamodel using [SDL](https://www.prisma.io/blog/graphql-sdl-schema-definition-language-6755bcb9ce51/) syntax. Each model is mapped to a table in your database schema:

```graphql
type User {
  id: ID! @unique
  email: String @unique
  name: String!
  posts: [Post!]!
}

type Post {
  id: ID! @unique
  title: String!
  published: Boolean! @default(value: "false")
  author: User
}
```

#### 4. Deploy datamodel to your migrate database

To deploy your Prisma API, run the following command:

```
prisma deploy
```

The Prisma API is deployed based on the datamodel and exposes CRUD & realtime operations for each model in that file.

#### 5. Use the Prisma client (JavaScript)

The Prisma client connects to the Prisma API and lets you perform read and write operations against your database. This section explains how to use the Prisma client from **JavaScript**.

First, inside the `hello-world` directory, install the `prisma-client-lib` dependency:

```
npm install --save prisma-client-lib
```

Next, to generate the Prisma client, run the following command:

```
prisma generate
```

Create a new Node script inside the `hello-world` directory:

```
touch index.js
```

Now add the following code to it:

```js
const { prisma } = require('./generated/prisma-client')

// A `main` function so that we can use async/await
async function main() {
  // Create a new user with a new post
  const newUser = await prisma.createUser({
    name: 'Alice',
    posts: {
      create: { title: 'The data layer for modern apps' }
    }
  })
  console.log(`Created new user: ${newUser.name} (ID: ${newUser.id})`)

  // Read all users from the database and print them to the console
  const allUsers = await prisma.users()
  console.log(allUsers)

  // Read all posts from the database and print them to the console
  const allPosts = await prisma.posts()
  console.log(allPosts)
}

main().catch(e => console.error(e))
```

Finally, run the code using the following command:

```
node index.js
```

<details><summary><b>See more API operations</b></summary>
<p>

```js
const usersCalledAlice = await prisma
  .users({
    where: {
      name: "Alice"
    }
  })
```

```js
// replace the __USER_ID__ placeholder with an actual user ID
const updatedUser = await prisma
  .updateUser({
    where: { id: "__USER_ID__" },
    data: { email: "alice@prisma.io" }
  })
```

```js
// replace the __USER_ID__ placeholder with an actual user ID
 const deletedUser = await prisma
  .deleteUser({ id: "__USER_ID__" })
```

```js
const postsByAuthor = await prisma
  .user({ email: "alice@prisma.io" })
  .posts()
```

</p>
</details>


#### 6. Next steps

Here is what you can do next:

- [Build an app with Prisma client](https://www.prisma.io/docs/-t201/)
- [Check out some examples](#examples)
- [Read more about how Prisma works](https://www.prisma.io/docs/-j9ff/).

## Examples

#### TypeScript

| Demo | Description |
|:------|:----------|
| [`cli-app`](https://github.com/prisma/prisma-examples/tree/master/typescript/cli-app) | Simple CLI TODO list app |
| [`graphql-auth`](https://github.com/prisma/prisma-examples/tree/master/typescript/graphql-auth) | GraphQL server with email-password authentication & permissions |
| [`graphql`](https://github.com/prisma/prisma-examples/tree/master/typescript/graphql) | Simple GraphQL server |
| [`graphql-subscriptions`](https://github.com/prisma/prisma-examples/tree/master/typescript/graphql-subscriptions) | GraphQL server with realtime subscriptions |
| [`rest-express`](https://github.com/prisma/prisma-examples/tree/master/typescript/rest-express) | Simple REST API with Express.JS |
| [`script`](https://github.com/prisma/prisma-examples/tree/master/typescript/script) | Simple usage of Prisma client in script |

> You can also check the [**AirBnB clone example**](https://github.com/prismagraphql/graphql-server-example) we built as a fully-featured demo app for Prisma.

#### Node.JS

| Demo | Description |
|:------|:----------|
| [`cli-app`](https://github.com/prisma/prisma-examples/tree/master/node/cli-app) | Simple CLI TODO list app |
| [`graphql-auth`](https://github.com/prisma/prisma-examples/tree/master/node/graphql-auth) | GraphQL server with email-password authentication & permissions |
| [`graphql-schema-delegation`](https://github.com/prisma/prisma-examples/tree/master/node/graphql-schema-delegation) | [Schema delegation](https://www.prisma.io/docs/prisma-graphql-api/usage/prisma-bindings-prb1/#building-graphql-servers-with-prisma-bindings) with Prisma binding |
| [`graphql`](https://github.com/prisma/prisma-examples/tree/master/node/graphql) | Simple GraphQL server |
| [`graphql-subscriptions`](https://github.com/prisma/prisma-examples/tree/master/node/graphql-subscriptions) | GraphQL server with realtime subscriptions |
| [`rest-express`](https://github.com/prisma/prisma-examples/tree/master/node/rest-express) | Simple REST API with Express.JS |
| [`script`](https://github.com/prisma/prisma-examples/tree/master/node/script) | Simple usage of Prisma client in script |

#### Golang

| Demo | Description |
|:------|:----------|
| [`cli-app`](https://github.com/prisma/prisma-examples/tree/master/go/cli-app) | Simple CLI TODO list app |
| [`graphql`](https://github.com/prisma/prisma-examples/tree/master/go/graphql) | Simple GraphQL server |
| [`http-mux`](https://github.com/prisma/prisma-examples/tree/master/go/http-mux) | Simple REST API with [gorilla/mux](https://github.com/gorilla/mux) |
| [`rest-gin`](https://github.com/prisma/prisma-examples/tree/master/go/rest-gin) | Simple REST API with [Gin](https://github.com/gin-gonic/gin) |
| [`script`](https://github.com/prisma/prisma-examples/tree/master/go/script) | Simple usage of Prisma client in script |

#### Flow

| Demo | Description |
|:------|:----------|
| [`graphql`](https://github.com/prisma/prisma-examples/tree/master/flow/graphql) | Simple GraphQL server |
| [`script`](https://github.com/prisma/prisma-examples/tree/master/flow/script) | Simple usage of Prisma client in script |

## Architecture

Prisma takes the role of the **data layer** in your backend architecture, replacing traditional ORMs and custom data access layers. It enables a _layered architecture_ which leads to better _separation of concerns_ and improves _maintainability_ of the entire backend.

The **Prisma client** is used inside your application server to perform read and write operations against your database through the Prisma API.

Prisma runs as _standalone processes_ which allows for it to be scaled independently from your application server.

<p align="center"><img src="https://imgur.com/OyIQQxF.png" height="132" /></p>

## Database Connectors

[Database connectors](https://github.com/prisma/prisma/issues/1751) provide the link between Prisma and the underlying database.

You can connect the following databases to Prisma already:

- MySQL
- PostgreSQL
- MongoDB

### Upcoming Connectors

If you are interested to participate in the preview for one of the following connectors, please reach out in our [Slack](https://slack.prisma.io).

- [Elastic Search](https://github.com/prisma/prisma/issues/1665)
- [MS SQL](https://github.com/prisma/prisma/issues/1642)
- [Oracle](https://github.com/prisma/prisma/issues/1644)
- [ArangoDB](https://github.com/prisma/prisma/issues/1645)
- [Neo4j](https://github.com/prisma/prisma/issues/1646)
- [Druid](https://github.com/prisma/prisma/issues/1647)
- [Dgraph](https://github.com/prisma/prisma/issues/1648)
- [DynamoDB](https://github.com/prisma/prisma/issues/1655)
- [Cloud Firestore](https://github.com/prisma/prisma/issues/1660)
- [CockroachDB](https://github.com/prisma/prisma/issues/1705)
- [Cassandra](https://github.com/prisma/prisma/issues/1750)
- [Redis](https://github.com/prisma/prisma/issues/1722)
- [AWS Neptune](https://github.com/prisma/prisma/issues/1752)
- [CosmosDB](https://github.com/prisma/prisma/issues/1663)
- [Influx](https://github.com/prisma/prisma/issues/1857)

Join the discussion or contribute to influence which we'll work on next!

## Community

Prisma has a [community](https://www.prisma.io/community) of thousands of amazing developers and contributors. Welcome, please join us! 👋

### Channels

- [Forum](https://www.prisma.io/forum)
- [Spectrum](https://spectrum.chat/prisma)
- [Slack](https://slack.prisma.io/)
- [Twitter](https://twitter.com/prisma)
- [Facebook](https://www.facebook.com/prisma.io)
- [Meetup](https://www.meetup.com/graphql-berlin)
- [GraphQL Conf](https://www.graphqlconf.org/)
- [Email](mailto:hello@prisma.io)

### Resources

- [Chinese translation of the Prisma docs](https://prisma.1wire.com/) (Thanks to [Victor Kang](https://github.com/Victorkangsh))
- [Awesome Prisma](https://github.com/catalinmiron/awesome-prisma) (Thanks to [Catalin Miron](https://github.com/catalinmiron))

## Contributing

Contributions are **welcome and extremely helpful** 🙌
Please refer [to the contribution guide](https://github.com/prisma/prisma/blob/master/CONTRIBUTING.md) for more information.

Releases are separated into three _channels_: **alpha**, **beta** and **stable**. You can learn more about these three channels and Prisma's release process [here](https://www.prisma.io/blog/improving-prismas-release-process-yaey8deiwaex/).

<p align="center"><a href="https://oss.prisma.io"><img src="https://imgur.com/IMU2ERq.png" alt="Prisma" height="170px"></a></p>
