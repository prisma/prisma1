---
alias: ofee7eseiy
path: /docs/reference/simple-api/one-to-many-edges
layout: REFERENCE
shorttitle: Modifying one-to-many edges
description: Modify edges for one-to-many relations with the Simple API and connect or disconnect two nodes in your GraphQL backend.
simple_relay_twin: ek8eizeish
tags:
  - simple-api
  - mutations
  - relations
  - edges
related:
  further:
    - ahwoh2fohj
    - zeich1raej
    - goh5uthoc1
  more:
    - chietu0ahn
---

# Modifying edges for one-to-many relations with the Simple API

One-to-many [relations](!alias-goh5uthoc1) relate two types to each other.

A node of the one side of a one-to-many relation can be connected to multiple nodes.
A node of the many side of a one-to-many relation can at most be connected to one node.

## Connect two nodes in a one-to-many relation

Creates a new edge between two nodes specified by their `id`. The according [types](!alias-ij2choozae) have to be in the same [relation](!alias-goh5uthoc1).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

> Adds a new edge to the relation called `UserPosts` and query the user name and the post title:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  addToAuthorPosts(
    authorUserId: "cixnekqnu2ify0134ekw4pox8"
    postsPostId: "cixnen24p33lo0143bexvr52n"
  ) {
    authorUser {
      name
    }
    postsPost {
      title
    }
  }
}
---
{
  "data": {
    "addToAuthorPosts": {
      "authorUser": {
        "name": "John Doe"
      },
      "postsPost": {
        "title": "My biggest Adventure"
      }
    }
  }
}
```

Note: Adds the edge only if this node pair is not connected yet by this relation. Does not remove any edges.

#### Disconnect two nodes in a one-to-many relation

Removes one edge between two nodes specified by `id`

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

> Removes an edge for the relation called `UserPosts` and query the user id and the post slug

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  removeFromAuthorPosts(
    authorUserId: "cixnekqnu2ify0134ekw4pox8"
    postsPostId: "cixnen24p33lo0143bexvr52n"
  ) {
    authorUser {
      name
    }
    postsPost {
      slug
    }
  }
}
---
{
  "data": {
    "removeFromAuthorPosts": {
      "authorUser": {
        "name": "John Doe"
      },
      "postsPost": {
        "title": "My biggest Adventure"
      }
    }
  }
}
```
