---
alias: utee3eiquo
description: Concepts
---

# Concepts

## Data model and Prisma database schema

The Prisma API of a Prisma service is fully centered around its data model.

The API is automatically generated based on the [data model](!alias-eiroozae8u) that's associated with your Prisma service.

Every operation exposed in the Prisma API is associated with a model or relation from your data model:

* [Queries](!alias-ahwee4zaey)
  * query one or more nodes of a certain model
  * query nodes across relations
  * query data aggregated across relations
* [Mutations](!alias-ol0yuoz6go)
  * create, update, upsert and delete nodes of a certain model
  * create, connect, disconnect, update and upsert nodes across relations
  * batch update or delete nodes of a certain model
* [Subscriptions](!alias-aey0vohche)
  * get notified about created, updated and deleted nodes

The actual [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) defining the available GraphQL operations in the Prisma API is also referred to as **Prisma database schema**.

> You can learn more about the differences between the data model and the prisma database in the [data modelling](!alias-eiroozae8u#prisma-database-schema-vs-data-model) chapter.

## Advanced API concepts

### Node selection

Many operations in the Prisma API only affect a subset of the existing nodes in the database, oftentimes even only a single node.

In these case, you need a way to ask for specific nodes in the API - most of the time this is done via a `where` argument.

Nodes can be selected via any field that's annotated with the [`@unique`](!alias-eiroozae8u#unique) directive.

For the following examples, consider the following simple data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  published: Boolean @default(value: "false")
}
```

Here are a few scenarios where node selection is required.

**Retrieve a single node by its `email`**:

```graphql
query {
  post(where: {
    email: "hello@graph.cool"
  }) {
    id
  }
}
```

**Update the `title` of a single node**:

```graphql
mutation {
  updatePost(
    where: {
      id: "ohco0iewee6eizidohwigheif"
    }
    data: {
      title: "GraphQL is awesome"
    }
  ) {
    id
  }
}
```

**Update `published` of a many nodes at once** (also see [Batch operations](#batch-operations)):

```graphql
mutation {
  updatePost(
    where: {
      id_in: ["ohco0iewee6eizidohwigheif", "phah4ooqueengij0kan4sahlo", "chae8keizohmiothuewuvahpa"]
    }
    data: {
      published: true
    }
  ) {
    count
  }
}
```

### Batch operations

One application of the node selection concept is the exposed [batch operations](!alias-utee3eiquo#batch-operations). Batch updating or deleting is optimized for making changes to a large number of nodes. As such, these mutations only return _how many_ nodes have been affected, rather than full information on specific nodes.

For example, the mutations `updateManyPosts` and `deleteManyPosts` provide a `where` argument to select specific nodes, and return a `count` field with the number of affected nodes (see the example above).

<InfoBox type="warning">

Note that no [subscription](!alias-aey0vohche) events are triggered for batch mutations!

</InfoBox>

### Connections

In contrast to the simpler object queries that directly return a list of nodes, connection queries are based on the [Relay Connection](https://facebook.github.io/relay/graphql/connections.htm) model. In addition to pagination information, connections also offer advanced features like aggregation.

For example, while the `posts` query allows you to select specific `Post` nodes, sort them by some field and paginate over the result, the `postsConnection` query additionally allows you to _count_ all unpublished posts:

```graphql
query {
  postsConnection {
    # `aggregate` allows to perform common aggregation operations
    aggregate {
      count
    }
    edges {
      # each `node` refers to a single `Post` element
      node {
        title
      }
    }
  }
}
```

### Transactional mutations

Single mutations in the Prisma API that are not batch operations are always executed _transactionally_, even if they consist of many actions that potentially spread across relations. This is especially useful for [nested mutations](!alias-ol0yuoz6go#nested-mutations) that perform several database writes on multiple types.

An example is creating a `User` node and two `Post` nodes that will be connected, while also connecting the `User` node to two other, already existing `Post` nodes, all in a single mutation. If any of the mentioned actions fail (for example because of a violated `@unique` field constraint), the entire mutation is rolled back!

Mutations are _transactional_, meaning they are [_atomic_](https://en.wikipedia.org/wiki/Atomicity_(database_systems)) and [_isolated_](https://en.wikipedia.org/wiki/Isolation_(database_systems)). This means that between two separate actions of the same nested mutation, no other mutations can alter the data. Also the result of a single action cannot be observed until the complete mutation has been processed.

## Authentication

### Service secret

The GraphQL API of a Prisma service is typically protected by a [service secret](!alias-ufeshusai8#secret-optional) which you specify in [`prisma.yml`](!alias-foatho8aip).

### Service token

The `secret` is used to sign a [JWT](https://jwt.io/) which can then be used in the `Authorization` field of the HTTP header:

```
Authorization: Bearer __TOKEN__
```

This is a sample payload for a JWT:

```json
{
  "exp": 1300819380,
  "service": "my-service@prod"
}
```

#### Claims

The JWT must contain different [claims](https://jwt.io/introduction/#payload):

* **Expiration time**: `exp`, the expiration time of the token.
* **Service information**: `service`, the name and stage of the service

> In the future there might be support for more fine grained access control by introducing a concept of roles such as `["write:Log", "read:*"]`

#### Generating a service token

##### Prisma CLI

Run `prisma token` to obtain a new signed JWT for your current Prisma service.

##### JavaScript

Consider the following `prisma.yml`:

```yml
service: my-service

stage: ${env:PRISMA_STAGE}
cluster: ${env:PRISMA_CLUSTER}

datamodel: database/datamodel.graphql

secret: ${env:PRISMA_SECRET}
```

> Note that this example uses [environment variables inside `prisma.yml`](!alias-nu5oith4da#environment-variables).

A Node server could create a signed JWT, based on the [`jsonwebtoken`](https://github.com/auth0/node-jsonwebtoken) library, for the stage `PRISMA_STAGE` of the service `my-service` like this:

```js
var jwt = require('jsonwebtoken')

jwt.sign(
  {
    data: {
      service: 'my-service@' + process.env.PRISMA_STAGE,
    },
  },
  process.env.PRISMA_SECRET,
  {
    expiresIn: '1h',
  }
)
```

#### JWT verification

For requests made against a Prisma service, the following properties of the JWT will be verified:

* It must be signed with a secret configured for the service
* It must contain an `exp` claim with a time value in the future
* It must contain a `service` claim with service and stage matching the current request

## Error handling

When an error occurs for one of your queries or mutations, the response contains an `errors` property with more information about the error `code`, the error `message` and more.

There are two kind of API errors:

* **Application errors** usually indicate that your request was invalid.
* **Internal server errors** usually mean that something unexpected happened inside of the Prisma service. Check your service logs for more information.

> **Note**: The `errors` field behaves according to the official [GraphQL specification for error handling](http://facebook.github.io/graphql/October2016/#sec-Errors).

### Application errors

An error returned by the API usually indicates that something is not correct with the requested query or mutation. You might have accidentally made a typo or forgot a required argument in your query. Try to investigate your input for possible errors related to the error message.

#### Troubleshooting

Here is a list of common errors that you might encounter:

##### Authentication

###### Insufficient permissions / Invalid token

```json
{
  "errors": [
    {
      "code": 3015,
      "requestId": "api:api:cjc3kda1l000h0179mvzirggl",
      "message":
        "Your token is invalid. It might have expired or you might be using a token from a different project."
    }
  ]
}
```

Check if the token you provided has not yet expired and is signed with a secret listed in [`prisma.yml`](!alias-foatho8aip).

### Internal server errors

Consult the service logs for more information on the error. For the local cluster, this can be done using the [prisma logs](!alias-aenael2eek) command.
