---
alias: oobi0eicho
description: Overview
---

# Overview

[`prisma-binding`](https://github.com/graphcool/prisma-binding/) is a dedicated [GraphQL binding](!alias-quaidah9ph) for Prisma services.

> **Note**: If you're curious about this topic, you can read the [blog post](https://blog.graph.cool/80a4aa37cff5) which introduces the general idea of GraphQL bindings.

`prisma-binding` provides a convenience layer for building GraphQL servers on top of Prisma services. In short, it simplifies implementing your GraphQL resolvers by _delegating_ execution of queries (or mutations) to the API of the underlying Prisma database service. Rather than writing SQL or accessing a NoSQL database API like MongoDB inside your resolvers, most of your resolver will be implemented as simple one-liners.

## Example

Consider the following application schema for your GraphQL server:

```graphql
# import Post from "./generated/prisma.graphql"

type Query {
  posts: [Post!]!
  post(id: ID!): Post
  description: String!
}

type Mutation {
  createDraft(title: String!, text: String): Post
  deletePost(id: ID!): Post
  publish(id: ID!): Post
}
```

This is how the corresponding resolvers are implemented:

```js
const resolvers = {
  Query: {
    posts(parent, args, ctx, info) {
      return ctx.db.query.posts({ }, info)
    },
    post(parent, args, ctx, info) {
      return ctx.db.query.post({ where: { id: args.id } }, info)
    },
  },
  Mutation: {
    createDraft(parent, { title, text }, ctx, info) {
      return ctx.db.mutation.createPost(
        {
          data: {
            title,
            text,
          },
        },
        info,
      )
    },
    deletePost(parent, { id }, ctx, info) {
      return ctx.db.mutation.deletePost({ where: { id } }, info)
    },
    publish(parent, { id }, ctx, info) {
      return ctx.db.mutation.updatePost(
        {
          where: { id },
          data: { isPublished: true },
        },
        info,
      )
    },
  },
}
```

> **Note**: To learn more about this particular example, check out [this](https://blog.graph.cool/tutorial-how-to-build-a-graphql-server-with-graphql-yoga-6da86f346e68) tutorial.

## Using Prisma bindings to build GraphQL servers with Prisma

Here is how it works:

1. Create your Prisma service by defining data model
1. Download generated database schema definition `database.graphql` (contains the full CRUD API)
1. Define your application schema, typically called `app.graphql`
1. Instantiate `Prisma` with information about your Prisma service (such as its endpoint and the path to the database schema definition)
1. Implement the resolvers for your application schema by delegating to the underlying Prisma service using the generated delegate resolver functions

> **Note**: If you're using a [GraphQL boilerplate](https://github.com/graphql-boilerplates/) project (e.g. with `graphql create`), the Prisma binding will already be configured and a few example resolvers implemented for you. You can either try the _dynamic binding_ (e.g. in the [`node-basic`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic) boilerplate) or a _static binding_ (e.g in the [`typescript-basic`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic) boilerplate).

## Installation

```sh
yarn add prisma-binding
# or
npm install --save prisma-binding
```
