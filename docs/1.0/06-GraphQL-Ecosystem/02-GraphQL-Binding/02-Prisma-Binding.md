---
alias: gai5urai6u
description: Prisma Binding provides a GraphQL Binding for Prisma services (GraphQL Database)
---

# prisma-binding

[`prisma-binding`](https://github.com/graphcool/prisma-binding/) is a dedicated [GraphQL binding](!alias-quaidah9ph) for Prisma services.

> If you're curious about this topic, you can read the [blog post](https://blog.graph.cool/80a4aa37cff5) which introduces the general idea of GraphQL bindings.

## Overview

`prisma-binding` provides a convenience layer for building GraphQL servers on top of Prisma services. In short, it simplifies implementing your GraphQL resolvers by _delegating_ execution of queries (or mutations) to the API of the underlying Prisma database service.

Here is how it works:

1. Create your Prisma service by defining data model
1. Download generated database schema definition `database.graphql` (contains the full CRUD API)
1. Define your application schema, typically called `app.graphql`
1. Instantiate `Prisma` with information about your Prisma service (such as its endpoint and the path to the database schema definition)
1. Implement the resolvers for your application schema by delegating to the underlying Prisma service using the generated delegate resolver functions

> **Note**: If you're using a [GraphQL boilerplate](https://github.com/graphql-boilerplates/) project (e.g. with `graphql create`), the Prisma binding will already be configured and a few example resolvers implemented for you. You can either try the _dynamic binding_ (e.g. in the [`node-basic`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic) boilerplate) or a _static binding_ (e.g in the [`typescript-basic`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic) boilerplate).

## Install

```sh
yarn add prisma-binding
# or
npm install --save prisma-binding
```

## Example

Consider the following data model for your Prisma service:

```graphql
type User {
  id: ID! @unique
  name: String
}
```

If you instantiate `Prisma` based on this service, you'll be able to send the following queries/mutations:

```js
// Instantiate `Prisma` based on concrete service
const prisma = new Prisma({
  typeDefs: 'schemas/database.graphql',
  endpoint: 'https://api.graph.cool/simple/v1/my-prisma-service'
  secret: 'my-super-secret-secret'
})

// Retrieve `name` of a specific user
prisma.query.user({ where { id: 'abc' } }, '{ name }')

// Retrieve `id` and `name` of all users
prisma.query.users(null, '{ id name }')

// Create new user called `Sarah` and retrieve the `id`
prisma.mutation.createUser({ data: { name: 'Sarah' } }, '{ id }')

// Update name of a specific user and retrieve the `id`
prisma.mutation.updateUser({ where: { id: 'abc' }, data: { name: 'Sarah' } }, '{ id }')

// Delete a specific user and retrieve the `name`
prisma.mutation.deleteUser({ where: { id: 'abc' } }, '{ id }')
```

Under the hood, each of these function calls is simply translated into an actual HTTP request against your Prisma service (using [`graphql-request`](https://github.com/graphcool/graphql-request)).

The API also allows to ask whether a specific node exists in your Prisma database:

```js
// Ask whether a post exists with `id` equal to `abc` and whose
// `author` is called `Sarah` (return boolean value)
prisma.exists.Post({
  id: 'abc',
  author: {
    name: 'Sarah'
  }
})
```

## API

### Prisma

#### constructor

```ts
constructor(options: PrismaOptions): Prisma
```

The `PrismaOptions` type has the following fields:

| Key | Required |  Type | Default | Note |
| ---  | --- | --- | --- | --- |
| `schemaPath` | Yes | `string` |  - | File path to the schema definition of your Prisma service (typically a file called `database.graphql`) |
| `endpoint` | Yes | `string` |  - | The endpoint of your Prisma service |
| `secret` | Yes | `string` |  - | The secret of your Prisma service |
| `fragmentReplacements` | No | `FragmentReplacements` |  `null` | A list of GraphQL fragment definitions, specifying fields that are required for the resolver to function correctly |
| `debug` | No | `boolean` |  `false` | Log all queries/mutations to the console |

#### query & mutation

`query` and `mutation` are public properties on your `Prisma` instance (see also the [GraphQL Binding documentation](!alias-quaidah9ph) for more info). They both are of type `Query` and expose a number of auto-generated delegate resolver functions that are named after the fields on the `Query` and `Mutation` types in your Prisma database schema.

Each of these delegate resolvers in essence provides a convenience API for sending queries/mutations to your Prisma service, so you don't have to spell out the full query/mutation from scratch and worry about sending it over HTTP. This is all handled by the delegate resolver function under the hood.

Delegate resolver have the following interface:

```js
(args: any, info: GraphQLResolveInfo | string): Promise<T>
```

The input arguments are used as follows:

- `args`: An object carrying potential arguments for the query/mutation
- `info`: An object representing the selection set of the query/mutation, either expressed directly as a string or in the form of `GraphQLResolveInfo` (you can find more info about the `GraphQLResolveInfo` type [here](http://graphql.org/graphql-js/type/#graphqlobjecttype))

The generic type `T` corresponds to the type of the respective field.

#### exists

`exists` also is a public property on your `Prisma` instance. Similar to `query` and `mutation`, it also exposes a number of auto-generated functions. However, it exposes only a single function per type. This function is named according to the root field that allows the retrieval of a single node of that type (e.g. `User` for a type called `User`). It takes a `where` object as an input argument and returns a `boolean` value indicating whether the condition expressed with `where` is met.

This function enables you to easily check whether a node of a specific type exists in your Prisma database.

#### request

The `request` method lets you send GraphQL queries/mutations to your Prisma service. The functionality is identical to the auto-generated delegate resolves, but the API is more verbose as you need to spell out the full query/mutation. `request` uses [`graphql-request`](https://github.com/graphcool/graphql-request) under the hood.

Here is an example of how it can be used:

```js
const query = `
  query ($userId: ID!){
    user(id: $userId) {
        id
      name
    }
  }
`

const variables = { userId: 'abc' }

prisma.request(query, variables)
  .then(result => console.log(result))
// sample result:
// {"data": { "user": { "id": "abc", "name": "Sarah" } } }
```

## Usage

- [graphql-boilerplate](https://github.com/graphcool/graphql-boilerplate).
- [graphql-server-example](https://github.com/graphcool/graphql-server-example).

## Next steps

- Code generation at build-time for the auto-generated delegate resolvers
