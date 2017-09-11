---
alias: ohc0oorahn
description: For every available type mutation in your GraphQL schema, certain subscriptions are automatically generated.
---

# Type Subscriptions in the Simple API

For every available [type](!alias-ij2choozae) mutation in your [GraphQL schema](!alias-ahwoh2fohj), certain subscriptions are automatically generated.

For example, if your schema contains a `Post` type:

```graphql
type Post {
  id: ID!
  title: String!
  description: String
}
```

a `Post` subscription is available that you can use to be notified whenever certain nodes are [created](!alias-oe8oqu8eis), [updated](!alias-ohmeta3pi4) or [deleted](!alias-bag3ouh2ii).



## Subscribing to Created Nodes

For a given type, you can subscribe to all successfully created nodes using the generated type subscription.

### Subscribe to all Created Nodes

If you want to subscribe to created nodes of the `Post` type, you can use the `Post` subscription and specify the `filter` object and set `mutation_in: [CREATED]`.

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cj03vcl4777hv0180zqjk5a6q
disabled: true
---
subscription createPost {
  Post(
    filter: {
      mutation_in: [CREATED]
    }
  ) {
    mutation
    node {
      description
      imageUrl
      author {
        id
      }
    }
  }
}
---
{
  "data": {
    "Post": {
      "mutation": "CREATED",
      "node": {
        "description": "#bridge",
        "imageUrl": "https://images.unsplash.com/photo-1420768255295-e871cbf6eb81",
        "author": {
          "id": "cj03wz212nbvt01925reuolju"
        }
      }
    }
  }
}
```

The payload contains

* `mutation`: in this case it will return `CREATED`
* `node`: allows you to query information on the created node and connected nodes

### Subscribe to Specific Created Nodes

You can make use of a similar [filter system as for queries](!alias-xookaexai0) using the `node` argument of the `filter` object.

For example, to only be notified of a created post if a specific user follows the author:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cj03vcl4777hv0180zqjk5a6q
disabled: true
---
subscription followedAuthorCreatedPost {
  Post(
    filter: {
      mutation_in: [CREATED]
      node: {
        author: {
          followedBy_some: {
            id: "cj03x3nacox6m0119755kmcm3"
          }
        }
      }
    }
  ) {
    mutation
    node {
      description
      imageUrl
      author {
        id
      }
    }
  }
}
---
{
  "data": {
    "Post": {
      "mutation": "CREATED",
      "node": {
        "description": "#bridge",
        "imageUrl": "https://images.unsplash.com/photo-1420768255295-e871cbf6eb81",
        "author": {
          "id": "cj03wz212nbvt01925reuolju"
        }
      }
    }
  }
}
```



## Subscribing to Deleted Nodes

For a given type, you can subscribe to all successfully deleted nodes using the generated type subscription.

### Subscribe to all Deleted Nodes

If you want to subscribe for updated nodes of the `Post` type, you can use the `Post` subscription and specify the `filter` object and set `mutation_in: [DELETED]`.

```graphql
subscription deletePost {
  Post(
    filter: {
      mutation_in: [DELETED]
    }
  ) {
    mutation
    previousValues {
      id
    }
  }
}
```

The payload contains

* `mutation`: in this case it will return `DELETED`
* `previousValues`: previous scalar values of the node

> Note: `previousValues` is `null` for `CREATED` subscriptions.

### Subscribe to Specific Deleted Nodes

You can make use of a similar [filter system as for queries](!alias-xookaexai0) using the `node` argument of the `filter` object.

For example, to only be notified of a deleted post if a specific user follows the author:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cj03vcl4777hv0180zqjk5a6q
disabled: true
---
subscription followedAuthorUpdatedPost {
  Post(
    filter: {
      mutation_in: [DELETED]
      node: {
        author: {
          followedBy_some: {
            id: "cj03x3nacox6m0119755kmcm3"
          }
        }
      }
    }
  ) {
    mutation
    previousValues {
      id
    }
  }
}
---
{
  "data": {
    "Post": {
      "mutation": "DELETED",
      "previousValues": {
        "id": "cj03x8r0mqhdq01190hx2ad2b"
      }
    }
  }
}
```


## Subscribing to Updated Nodes

For a given type, you can subscribe to all successfully updated nodes using the generated type subscription.

### Subscribe to all Updated Nodes

If you want to subscribe for updated nodes of the `Post` type, you can use the `Post` subscription and specify the `filter` object and set `mutation_in: [UPDATED]`.

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cj03vcl4777hv0180zqjk5a6q
disabled: true
---
subscription updatePost {
  Post(
    filter: {
      mutation_in: [UPDATED]
    }
  ) {
    mutation
    node {
      description
      imageUrl
      author {
        id
      }
    }
    updatedFields
    previousValues {
      description
      imageUrl
    }
  }
}
---
{
  "data": {
    "Post": {
      "mutation": "UPDATED",
      "node": {
        "description": "#food",
        "imageUrl": "https://images.unsplash.com/photo-1432139438709-ee8369449944",
        "author": {
          "id": "cj03wz212nbvt01925reuolju"
        }
      }
      "updatedFields": [
        "imageUrl"
      ],
      "previousValues": {
        "description": "#food",
        "imageUrl": "https://images.unsplash.com/photo-1457518919282-b199744eefd6"
      }
    }
  }
}
```

The payload contains

* `mutation`: in this case it will return `UPDATED`
* `node`: allows you to query information on the updated node and connected nodes
* `updatedFields`: a list of the fields that changed
* `previousValues`: previous scalar values of the node

> Note: `updatedFields` is `null` for `CREATED` and `DELETED` subscriptions. `previousValues` is `null` for `CREATED` subscriptions.

### Subscribe to Updated Fields

You can make use of a similar [filter system as for queries](!alias-xookaexai0) using the `node` argument of the `filter` object.

For example, to only be notified of an updated post if its `description` changed:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cj03vcl4777hv0180zqjk5a6q
disabled: true
---
subscription followedAuthorUpdatedPost {
  Post(
    filter: {
      mutation_in: [UPDATED]
      updatedFields_contains: "description"
    }
  ) {
    mutation
    node {
      description
    }
    updatedFields
    previousValues {
      description
    }
  }
}
---
{
  "data": {
    "Post": {
      "mutation": "UPDATED",
      "node": {
        "description": "#best #food",
        "imageUrl": "https://images.unsplash.com/photo-1457518919282-b199744eefd6",
        "author": {
          "id": "cj03wz212nbvt01925reuolju"
        }
      }
      "node": {
        "description": "#best #food"
      },
      "updatedFields": [
        "description"
      ],
      "previousValues": {
        "description": "#food"
      }
    }
  }
}
```

Similarily to `updatedFields_contains`, more filter conditions exist:

* `updatedFields_contains_every: [String!]`: matches if all fields specified have been updated
* `updatedFields_contains_some: [String!]`: matches if some of the specified fields have been updated

> Note: you cannot use the `updatedFields` filter conditions together with `mutation_in: [CREATED]` or `mutation_in: [DELETED]`!
