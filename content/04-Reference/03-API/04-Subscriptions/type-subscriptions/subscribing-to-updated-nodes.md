---
alias: ohmeta3pi4
path: /docs/reference/simple-api/subscribing-to-updated-nodes
layout: REFERENCE
shorttitle: Subscribing to Updated Nodes
description: For a given type, you can subscribe to all successfully updated nodes using the generated type subscription.
simple_relay_twin: eih4eew7re
tags:
  - simple-api
  - subscriptions
related:
  further:
    - oe8oqu8eis
    - bag3ouh2ii
    - kengor9ei3
---

# Subscribing to Updated Nodes

For a given type, you can subscribe to all successfully updated nodes using the generated type subscription.

## Subscribe to all Updated Nodes

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

## Subscribe to Updated Fields

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
