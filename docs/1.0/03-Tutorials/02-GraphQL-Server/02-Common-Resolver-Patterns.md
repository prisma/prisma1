---
alias: eifeecahx4
description: Learn about different resolver patterns
---

# Common Resolver Patterns

This tutorial gives an overview about common scenarios you might encounter when implementing your GraphQL server with `graphql-yoga` and Prisma.

Note that all scenarios in this tutorial are based on the [`typescript-basic`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic) GraphQL boilerplate project.

### Scenario: Add a new field to the data model and expose it in the API

Adding a new address field to the `User` type in the database, with the purpose of exposing it in the application API as well.

### Instructions

#### 1. Adding the field to the data model

in `database/datamodel.graphql`:

```diff
type User {
  id: ID! @unique
  email: String! @unique
  password: String!
  name: String!
  posts: [Post!]! @relation(name: "UserPosts")
+ address: String
}
```

#### 2. Deploying the updated data model

```sh
graphcool deploy
```

This will...

* ... deploy the new database structure to the local service
* ... download the new GraphQL schema for the database to `database/schema.graphql`

#### 3. Adding the field to the application schema

In `src/schema.graphql`:

```diff
type User {
  id: ID!
  email: String!
  name: String!
  posts: [Post!]!
+ address: String
}
```
