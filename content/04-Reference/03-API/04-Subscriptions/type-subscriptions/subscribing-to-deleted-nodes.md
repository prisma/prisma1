---
alias: bag3ouh2ii
path: /docs/reference/simple-api/subscribing-to-deleted-nodes
layout: REFERENCE
shorttitle: Subscribing to Deleted Nodes
description: For a given type, you can subscribe to all successfully deleted nodes using the generated type subscription.
simple_relay_twin: eih4eew7re
tags:
  - simple-api
  - subscriptions
related:
  further:
    - oe8oqu8eis
    - ohmeta3pi4
    - kengor9ei3
---

# Subscribing to Deleted Nodes

For a given type, you can subscribe to all successfully deleted nodes using the generated type subscription.

## Subscribe to all Deleted Nodes

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

## Subscribe to Specific Deleted Nodes

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
