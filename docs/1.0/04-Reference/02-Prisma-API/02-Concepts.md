---
alias: utee3eiquo
description: Concepts
---

# Concepts

## Data Model and Data Schema

The Prisma API of a Prisma service is fully centered around its data model.

The API is automatically generated based on the [data model](!alias-eiroozae8u) deployed to your Prisma service.
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

The actual [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) defining the available GraphQL operations available in a Prisma API is also referred to as **data schema**.

## Advanced API Concepts

### Node Selection

Many operations in the Prisma API can be narrowed down to a specific subset of all nodes they would otherwise affect. Some operations even _require_ the selection of specific nodes. Oftentimes, this is done via a `where` parameter.

An example is selecting all `Post` nodes that are connected to a specific `User` node, and contain a certain string in their `title`.

#### Variant: Select Nodes by Unique Identifier

This is a special case of node selection. Whenever you can select only one node, you can do so by providing a value for any of the fields that are marked [unique](!alias-eiroozae8u#field-constraints) on the according model.

An example is connecting a `Post` node to a specific `User` node by providing the user's unique email address.

### Batch Operations

One application of the node selection concept is the exposed [batch operations](!alias-utee3eiquo#batch-operations). Batch updating or deleting is optimized for making changes to a large number of nodes. As such, these mutations only return _how many_ nodes have been affected, rather than full information on specific nodes.

For example, the mutations `updateManyPosts` and `deleteManyPosts` provide a `where` argument to select specific nodes, and return a `count` field with the number of affected nodes.

### Connections

In constrast to the simpler model queries that directly return a list of nodes, connection queries are based on [Relay Connections](https://facebook.github.io/relay/graphql/connections.htm). Connections not only expose edges and pagination information needed for Relay, but also offer advanced features like aggregation.

For example, while the `posts` query allows you to select specific `Post` nodes, sort them by their publication date and paginate over the result, the `postsConnection` query additionally allows you to _count_ all unpublished posts.

### Transactional Mutations

Single Mutations in the Prisma API that are not batch operations are always executed transactionally, even if they consist of many actions that potentially spread across relations. This is especially useful for [nested mutations](!alias-ol0yuoz6go#batch-mutations), that span operations across relations.

An example is creating a `User` node and two `Post` nodes that will be connected, while also connecting the `User` node to two other, already existing `Post` nodes, all in a single mutation. If any of the mentioned actions fail (for example because of a violated unique field constraint), the complete mutation is rolled back.

Even though mutations are _transactional_, they are not _atomic_. This means that between two separate actions of the same nested mutation, other mutations can alter the data.

## Authentication

The GraphQL API of a Prisma service is typically protected by a secret specified in `prisma.yml`.

The secret is used to sign a JWT that can then be used in the Authorization header:

```
Authorization: Bearer ${jwt}
```

This is an example JWT:

```json
{
  "exp": 1300819380,
  "service": "my-service@prod"
}
```

### Claims

The JWT must contain different claims:

* **Expiration Time**: `exp`, the expiration time of the token.
* **Service Information**: `service`, the name and stage of the service

> In the future we might add support for more fine grained access control by introducing a concept of roles such as `["write:Log", "read:*"]`

### Generating a Signed JWT

#### In the CLI

Run `prisma token` to obtain a new signed JWT.

#### In Node

Consider this `prisma.yml`:

```yml
service: my-service

stage: ${env:PRISMA_STAGE}
cluster: ${env:PRISMA_CLUSTER}

datamodel: database/datamodel.graphql

secret: ${env:PRISMA_SECRET}
```

A Node server could create a signed JWT for the stage `PRISMA_STAGE` of the service `my-service` like this:

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

### JWT verification

For requests made to a Prisma service, the following properties of the JWT will be verified:

* It must be signed with a secret configured for the service
* It must contain an `exp` claim with a time value in the future
* It must contain a `service` claim with service and stage matching the current request

## Error Handling

When an error occurs for one of your queries or mutations, the response contains an `errors` property with more information about the error `code`, the error `message` and more.

There are two kind of API errors:

* application errors usually indicate that your request was invalid. Try to
* internal server errors usually means that something unexpected happened in the service. Check your service logs for more information.

### Application Errors

An error returned by the API usually indicates that something is not correct with the requested query or mutation. Try to investigate your input for possible errors related to the error message.

#### Troubleshooting

Here is a list of common errors that you might encounter:

##### Authentication

**Insufficient Permissions or Invalid Token**

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

### Internal Server Errors

Consult the service logs for more information on the error. For the local cluster, this can be done using the [prisma logs](!alias-aenael2eek) command.
