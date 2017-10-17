---
alias: iox3aqu0ee
description: GraphQL permission queries allow you to express permissions by leveraging the power of GraphQL queries. This is a simple and powerful combination.
---

# Permission Queries

Permission queries allow you to **express permissions by leveraging the power of GraphQL queries**. This combination is a powerful concept and provides a great tool to express even complex permission scenarios in a simple form.

## The GraphQL permission schema

<!-- PERMISSION_EXAMPLES -->

All available queries in the **GraphQL permission schema** are derived from the available types and relations in your data model. The permisson schema leverages the familiar and powerful [filter system](!alias-nia9nushae#filtering-by-field) that allows you to define very specific permissions.

For every type `Type` in your data model, the field `SomeTypeExists(filter: TypeFilter): Boolean!` is part of the permission schema. `SomeTypeExists` returns `true` only if the given filters match at least one existing `Type` node.

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

## Writing permission queries in a GraphQL Playground

The [GraphQL Playground](https://github.com/graphcool/graphql-playground) has built-in support for writing permission queries.

You can download it [here](https://github.com/graphcool/graphql-playground/releases).

### Opening a Playground

The Playground is a standalone app that you can simply open using the interface of your operating system and then set the endpoint of your GraphQL API.

To directly open the Playground for a Graphcool service, you can execute the [`graphcool playground`](!alias-aiteerae6l#graphcool-playground) command in the root directory of your service definition.

### Opening a new "permission query"-tab

Once the Playground is open and configured to run against your Graphcool service, you can click the **Key**-icon in the top-right corner to open a new "permission query"-tab:

![](https://i.imgur.com/yNwhYBq.png)

Before the tab opens, you can specify the _operation_ for which the permission query should apply:

![](https://imgur.com/s1EXJmx.png)

Once you selected the operation, a new "permission query"-tab opens and you can start writing your query. The query will be executed against the [permission schema](#the-qraphql-permission-schema).


### Writing a permission query

Depending on the operation that was selected, the Playground displays a number of available [permission variables](#available-graphql-variables-for-permission-queries) in the center pane.

When writing the query, you can select a variable and it will be automatically injected as an input argument for your query. Note that you still have to set the values of for the variables in the _Query Variables_ section of the Playground (in the bottom left corner):

![](https://imgur.com/qfo6I8n.png)



