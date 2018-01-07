---
alias: ohm2ouceuj
description: How to use the API
---

# Overview

A Graphcool service exposes a GraphQL API, that is automatically generated based on the deployed data model.

## Authentication

When authentication is enabled, access to the GraphQL API is secured by one or more secrets [configured in `graphcool.yml`](!alias-).

## Exploring the GraphQL API

The [GraphQL Playground](https://github.com/graphcool/graphql-playground) can be used to explore and run GraphQL mutations, queries and subscriptions.

To open up a Playground for your database service, simply run the `graphcool playground` command in the root directory of your service or paste your service's HTTP endpoint into the address bar of your browser.

## The Database Schema

The [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) belonging to the GraphQL API a Graphcool service exposes is often referred to as **the database schema**.

The database schema of a service is automatically generated based on the deployed data model.

As an example, consider this data model:

```graphql
type User {
  id: ID! @unique
  email: String! @unique
  name: String
}
```

After deploying it to a Graphcool service, the service will expose the following database schema:

```graphql
type Query {

  # retrieve a single user by a unique field
  user(where: UserWhereUniqueInput): User

  # retrieve list of users
  users(
    where: UserWhereInput
    orderBy: UserOrderByInput
    skip: Int
    before: String
    after: String
    first: Int
    last: Int
  ): [User!]!

  # retrieve list of users using GraphQL connections
  usersConnection(
    filter: UserWhereInput
    orderBy: UserOrderByInput
    skip: Int
    before: String
    after: String
    first: Int
    last: Int
  ): UserConnection!
}

type Mutation {
  createUser(data: UserCreateInput!): User!
  updateUser(data: UserUpdateInput!, where: UserWhereUniqueInput!): User
  deleteUser(where: UserWhereUniqueInput!): User
  upsertUser(where: UserWhereUniqueInput!, create: UserCreateInput!, update: UserUpdateInput!): User

  updateManyUsers(data: UserUpdateInput!, where: UserWhereInput!): BatchPayload
  deleteManyUsers(where: UserWhereInput!): BatchPayload
}

interface Node {
  id: ID @unique
}

type User implements Node {
  id: ID! @unique
  email: String! @unique
  name: String
}

type UserConnection {
  edges: [UserEdge!]
  aggregate: UserAggregate
  groupwhere: UserGroupBy
}

type UserEdge {
  node: User!
}

input UserCreateInput {
  email: String!
  name: String
}

input UserWhereUniqueInput {
  id: ID
  email: String
}

input UserUpdateInput {
  email: String
  name: String
}

input UserWhereInput {
  # all generated where properties
}

enum UserOrderByInput {
  # all generated order properties
}
```

Most notably, the `Query` and `Mutation` types define all the queries and mutations that can be sent to the API.
