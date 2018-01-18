---
alias: iox3aqu0ee
description: GraphQL permission queries allow you to express permissions by leveraging the power of GraphQL queries. This is a simple and powerful combination.
---

# Permission Queries

Permission queries allow you to **express permissions by leveraging the power of GraphQL queries**. This combination is a powerful concept and provides a great tool to express even complex permission scenarios in a simple form.

## The Graphcool permission schema

<!-- PERMISSION_EXAMPLES -->

### Overview

All available queries in the **GraphQL permission schema** are derived from the available types and relations in your data model. The permisson schema leverages the familiar and powerful [filter system](!alias-nia9nushae#filtering-by-field) that allows you to define very specific permissions.

For every type `Type` in your data model, the field `SomeTypeExists(filter: TypeFilter): Boolean!` is part of the permission schema. `SomeTypeExists` returns `true` only if the given filters match at least one existing `Type` node.

As an example, consider the following type definitions:

```graphql
type Post @model {
  id: ID! @isUnique # read-only (managed by Graphcool)
  title: String!
  author: User! @relation(name: "UsersPosts") 
}

type User @model {
  id: ID! @isUnique # read-only (managed by Graphcool)
  name: String!
  posts: [Post!]! @relation(name: "UsersPosts") 
}
```

The `Query` type from generated permission schema will look as follows:

```graphql
type Query {
  SomePostExists(filter: PostFilter, orderBy: PostOrderBy, skip: Int, after: String, before: String, first: Int, last: Int): Boolean!
  SomeUserExists(filter: UserFilter, orderBy: UserOrderBy, skip: Int, after: String, before: String, first: Int, last: Int): Boolean!
}
```

The `PostFilter` and `UserFilter` as well as `PostOrderBy` and `UserOrderBy` types are [similar to the ones in the regular GraphQL API](!alias-nia9nushae#explore-available-filter-criteria).

### Accessing the permission schema

Every Graphcool service comes with a dedicated endpoint for the permission schema. This endpoint is of the following form:

```
https://api.graph.cool/simple/v1/__SERVICE_ID__/permissions
```

To access it, you need to set the `Authorization` header in the request. Here is a sample request against the example schema above:

```sh
curl 'https://api.graph.cool/simple/v1/__SERVICE_ID__/permissions' \
-H 'Authorization: Bearer __PLATFORM_TOKEN__' \
-H 'Content-Type: application/json' \
-d '{"query":"{\n  SomePostExists\n}"}' 
```

## The execution and evaluation of permission queries

The execution of permission queries differs for read and write operations:

For **read operations**, all matching permission queries are executed and evaluated for every single node that is part of the response. For all **write operations**, all matching permission queries are executed and evaluated before the pending operation.

**A permission query grants permission to an operation if and only if all top-level fields return `true`**.

## Available GraphQL variables for permission queries

Depending on the pending operation, several **GraphQL variables** are available that can be used to influence the behaviour of a permission query.

**You can't specify the values for these variables. You can only use the variables in the query itself**.

### Custom variables

* `$now: DateTime!` the current time as a [`DateTime`](!alias-teizeit5se#datetime)

> The `$now` variable is not available yet.

### Variables for type permissions

* `$node_id: ID!` the id of the current node
* `$node_scalar` the value of a scalar field on an existing node

* `$user_id: ID!` the id of the authenticated user
* `$input_scalar` the value of an input argument of a given mutation

### Variables for relation permissions

* `$leftType_id: ID!`, `$rightType_id: ID!` the ids of the nodes to be connected or disconnected

For example, a relation with the two fields `users: [User!]!` and `post: Post`, will result in these variables:

* `usersUser_id: ID!`
* `postPost_id: ID!`

## Common authorization patterns

Permission queries can be used to express all common authorization patterns like **role-based authorization**, **access control lists** and **ownership derived access**. For more information check [the full guide](!alias-miesho4goo).



