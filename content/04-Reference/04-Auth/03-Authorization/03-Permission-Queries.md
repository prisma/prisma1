---
alias: iox3aqu0ee
description: GraphQL permission queries allow you to express permissions by leveraging the power of GraphQL queries. This is a simple and powerful combination.
---

# Permission Queries

Permission queries allow you to **express permissions by leveraging the power of GraphQL queries**. This combination is a powerful concept and provides a great tool to express even complex permission scenarios in a simple form.

## The GraphQL Permission Schema

<!-- PERMISSION_EXAMPLES -->

All available queries in the **GraphQL permission schema** are derived from the available types and relations in your data model. The permisson schema leverages the familiar and powerful [filter system](!alias-xookaexai0) that allows you to define very specific permissions.

For every type `Type` in your data model, the field `SomeTypeExists(filter: TypeFilter): Boolean!` is part of the permission schema. `SomeTypeExists` returns `true` only if the given filters match at least one existing `Type` node.

## The Execution and Evaluation of Permission Queries

The execution of permission queries differs for read and write operations:

For **read operations**, all matching permission queries are executed and evaluated for every single node that is part of the response. For all **write operations**, all matching permission queries are executed and evaluated before the pending operation.

**A permission query grants permission to an operation if and only if all top-level fields return `true`**.

## Available GraphQL Variables for Permission Queries

Depending on the pending operation, several **GraphQL variables** are available that can be used to influence the behaviour of a permission query.

**You can't specify the values for these variables. You can only use the variables in the query itself**.

### Custom Variables

* `$now: DateTime!` the current time as a [`DateTime`](!alias-teizeit5se#datetime)

> The `$now` variable is not available yet.

### Variables for Type Permissions

* `$node_id: ID!` the id of the current node
* `$node_scalar` the value of a scalar field on an existing node

* `$user_id: ID!` the id of the authenticated user
* `$input_scalar` the value of an input argument of a given mutation

### Variables for Relation Permissions

* `$leftType_id: ID!`, `$rightType_id: ID!` the ids of the nodes to be connected or disconnected

## Common Authorization Patterns

Permission queries can be used to express all common authorization patterns like **role-based authorization**, **access control lists** and **ownership derived access**. For more information check [the full guide](!alias-miesho4goo).
