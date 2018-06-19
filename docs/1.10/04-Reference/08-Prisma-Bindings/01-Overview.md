---
alias: oobi0eicho
description: Overview
---

# Overview

[`prisma-binding`](https://github.com/graphcool/prisma-binding/) is a dedicated [GraphQL binding](https://oss.prisma.io/content/GraphQL-Binding/01-Overview.html) for Prisma GraphQL APIs. Think of it like an _auto-generated SDK_ for Prisma services:

![](https://oss.prisma.io/assets/bindings.png)

> **Note**: If you're curious about this topic, you can read the following two blog posts:
> - [Reusing & Composing GraphQL APIs with GraphQL Bindings]
(https://blog.graph.cool/reusing-composing-graphql-apis-with-graphql-bindings-80a4aa37cff5)
> - [GraphQL Binding 2.0: Improved API, schema transforms & automatic codegen](https://www.prisma.io/blog/graphql-binding-2-0-improved-api-schema-transforms-automatic-codegen-5934cd039db1/)

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

This is how the corresponding resolvers are implemented with a `Prisma` binding available as a `db` object on `ctx`:

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

> **Note**: To learn more about this particular example, check out [this](!alias-nahgaghei6) tutorial.

## Using Prisma bindings to build GraphQL servers with Prisma

Here is how it works:

1. Create your Prisma service by defining data model
1. Download generated the Prisma database schema `prisma.graphql` (contains the full CRUD API)
1. Define your application schema, typically called `schema.graphql`
1. Instantiate `Prisma` with information about your Prisma service (such as its `endpoint` and the path to the database schema definition)
1. Implement the resolvers for your application schema by delegating to the underlying Prisma service using the generated delegate resolver functions

> **Note**: If you're using a [GraphQL boilerplate](https://github.com/graphql-boilerplates/) project (e.g. with `graphql create`), the Prisma binding will already be configured and a few example resolvers implemented for you. You can either try the _dynamic binding_ (e.g. in the [`node-basic`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic) boilerplate) or a _static binding_ (e.g in the [`typescript-basic`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic) boilerplate).
