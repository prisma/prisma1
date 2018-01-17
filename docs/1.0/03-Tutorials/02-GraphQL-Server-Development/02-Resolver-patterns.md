---
alias: eifeecahx4
description: Learn about common resolver patterns
---

# Common Resolver Patterns

This tutorial gives an overview about common scenarios you might encounter when implementing your GraphQL server with `graphql-yoga` and Prisma.

Note that all scenarios in this tutorial are based on the [`typescript-basic`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic) GraphQL boilerplate project.

### Scenario: Add a new field to the data model and expose it in the API

Adding a new address field to the `User` type in the database, with the purpose of exposing it in the application API as well.

### Instructions

#### 1. Adding the field to the data model

in `database/datamodel.graphql`:

```graphql
type User {
  id: ID! @unique
  email: String! @unique
  password: String!
  name: String!
  posts: [Post!]!
+ address: String
}
```

#### 2. Deploying the updated data model

```sh
prisma deploy
```

This will...

* ... deploy the new database structure to the local service
* ... download the new GraphQL schema for the database to `database/schema.graphql`

#### 3. Adding the field to the application schema

In `src/schema.graphql`:

```graphql
type User {
  id: ID!
  email: String!
  name: String!
  posts: [Post!]!
+ address: String
}
```

### Scenario: Adding a new resolver

Suppose we want to add a custom resolver to delete a `Post`.

### Instructions

Add a new `delete` field to the Mutation type in `src/schema.graphql`

```graphql
type Mutation {
  createDraft(title: String!, text: String): Post
  publish(id: ID!): Post
+ delete(id: ID!): Post
}
```

Add a `delete` resolver to Mutation part of `src/index.js`

```js
delete(parent, { id }, ctx, info) {
  return ctx.db.mutation.deletePost(
  {
    where: { id }
  },
    info
  );
}
```

Run `yarn start`.

Then you can run the following mutation to delete a post:

```graphql
mutation {
  delete(id: "__POST_ID__") {
    id
  }
}
```
