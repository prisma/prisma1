---
alias: ag2ahrohyi
description: GraphQL bindings for Graphcool services
---

# Graphcool Binding

## Overview

[`graphcool-binding`](https://github.com/graphcool/graphcool-binding) provides a convenience layer for building GraphQL servers on top of your Graphcool services. In short, it simplifies the process of implementing your GraphQL resolvers by _delegating_ execution of queries (or mutations) to the API of the underlying Graphcool database service.

Here is how it works:

1. Create your Graphcool service by defining data model
1. Download generated database schema definition `database.graphql` (contains the full CRUD API)
1. Define your application schema, typically called `app.graphql`
1. Instantiate `Graphcool` with information about your Graphcool service (such as its endpoint and the path to the database schema definition)
1. Implement resolvers for application schema by delegating to underlying Graphcool service using the generated _binding functions_

## Installation

You can install `graphcool-binding` with npm or yarn:

```sh
yarn add graphcool-binding
# or
npm install --save graphcool-binding
```

## Binding functions

The core idea of GraphQL bindings is to create dedicated functions mirroring the _root fields_ of your GraphQL schema. These functions are called _binding functions_.

Rather than having to construct a full GraphQL query/mutation and sending it to the API manually (e.g. with `fetch` or `graphql-request`), this allows to simply invoke functions for sending specific queries/mutations.

Additionally, if the binding functions are generated in a build step and based on a strongly type language (like TypeScript or Flow), you also get compile-time safety for all interactions with your GraphQL API.

## Examples

Assume you specify the following data model for your Graphcool service:

```graphql
type User {
  id: ID! @unique
  name: String
}
```

When deploying the service, Graphcool will generate a database schema similar to this:

```graphql
type Query {
  user(id: ID!): User
  users: [User!]!
}

type Mutation {
  createUser(name: String!): User
  updateUser(id: ID!, name: String): User
  deleteUser(id: ID!): User
}
```

> Note: This is a simplified version of the schema that's actually generated. This one only serves as a simple CRUD example and for example doesn't contain any filter or pagination arguments for the `users` list.

If you instantiate `Graphcool` based on this service, the resulting object exposes a number of _binding functions_ named after the root fields in the database schema. Here's an overview of the binding functions you can now invoke on your `Graphcool` instance:

```js
// Instantiate `Graphcool` based on concrete service
const graphcool = Graphcool({ ... })

// Retrieve `name` of a specific user
graphcool.user({ id: 'abc' }, '{ name }')

// Retrieve `id` and `name` of all users
graphcool.users(null, '{ id name }')

// Create new user called `Sarah` and retrieve the `id`
graphcool.createUser({ name: 'Sarah' }, '{ id }')

// Update name of a specific user and retrieve the `id`
graphcool.updateUser({ id: 'abc', name: 'Sarah' }, '{ id }')

// Delete a specific user and retrieve the `name`
graphcool.deleteUser({ id: 'abc' }, '{ id }')
```

Under the hood, each of these function calls is simply translated into an actual HTTP request against your Graphcool service (using [`graphql-request`](https://github.com/graphcool/graphql-request)). So, for example, the call to `graphcool.user({ id: 'abc' }, '{ name }')` is translated to the following:

```js
const { request } = require('graphql-request')

const query = `
{
  user {
    name
  }
}
`

const variables = { id: 'abc' }

request(
  __GRAPHCOOL_ENDPOINT__,
  query,
  variables
)
```

Assume your application schema now looks as follows:

```graphql
type Query {
  usersCalledSarah: [User!]!
}
```

When implementing the resolver for `usersCalledSarah`, you can write the following:

```js
const resolvers = {
  Query: {
    usersCalledSarah: async (parent, args, context, info) => {
      context.db.users(
        { filter: { name: 'Sarah' } },
        info
      )
    }
  }
}
```

The second argument for any of the generated binding functions is either a string to specify the selection set for the query/mutation or an object of type [`GraphQLResolveInfo`](http://graphql.org/graphql-js/type/#graphqlobjecttype) (thus effectively also a represenation of a selection set). Here, you're simply passing on the `info` object that's already passed into the resolver.

Note that the above setup requires that your `Graphcool` instance is added to the `context` object which is passed down the resolver chain. If you're using `graphql-yoga` for your GraphQL server implementation, the setup might look similar to this:

```js
import { GraphQLServer } from 'graphql-yoga'
import { Graphcool } from 'graphcool-binding'
import { importSchema } from 'graphql-import'
import { resolvers } from './resolvers'
const typeDefs = importSchema('./src/schemas/app.graphql')

const server = new GraphQLServer({
  typeDefs,
  resolvers,
  context: req => ({
    ...req,
    db: new Graphcool({
      schema: './src/schemas/database.graphql',
      endpoint: process.env.GRAPHCOOL_ENDPOINT,
    }),
  }),
})
```

## API

### `Graphcool`

Instances of `Graphcool` allow you to interact with your Graphcool service, this includes:

- delegating execution of queries and mutations to the Graphcool service with binding functions
- checking if a certain node exists in the Graphcool database
- sending standard queries and mutations to the Graphcool service

#### `constructor(options: GraphcoolOptions): Graphcool`

The `GraphcoolOptions` type has the following fields:

| Key | Required |  Type | Default | Note |
| ---  | --- | --- | --- | --- |
| `schemaPath` | Yes | `string` |  - | File path to the schema definition of your Graphcool service (typically a file called `database.graphql`) |
| `endpoint` | Yes | `string` |  - | The endpoint of your Graphcool service |
| `secret` | Yes | `string` |  - | The secret of your Graphcool service |
| `fragmentReplacements` | No | `FragmentReplacements` |  `null` | A list of GraphQL fragment definitions, specifying fields that are required for the resolver to function correctly |

#### `query` and `mutation`

`query` and `mutation` are public properties on your `Graphcool` instance. They both are of type `Query` and expose a number of binding functions that are named after the fields on the `Query` and `Mutation` types in your Graphcool database schema.

Each of these binding functions in essence provides a convenience API for sending a queries/mutations to your Graphcool service, so you don't have to spell out the full query/mutation from scratch and worry about sending an HTTP request.

Binding functions have the following API:

```js
(args: any, info: GraphQLResolveInfo | string): Promise<T>
```

The input arguments are used as follows:

- `args`: An object that carries potential arguments for the query/mutation
- `info`: Represents the selection set of the query/mutation

#### `exists`

## Usage

See [graphql-boilerplate](https://github.com/graphcool/graphql-boilerplate).