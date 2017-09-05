---
alias: zeich1raej
path: /docs/reference/simple-api/one-to-many-edges
layout: REFERENCE
shorttitle: Modifying one-to-one edges
description: Modify edges for one-to-one relations with the Simple API and connect or disconnect two nodes in your GraphQL backend.
simple_relay_twin: da7pu3seew
tags:
  - simple-api
  - mutations
  - relations
  - edges
related:
  further:
    - ahwoh2fohj
    - ofee7eseiy
    - goh5uthoc1
  more:
    - chietu0ahn
---

# Modifying edges for one-to-one relations with the Simple API

A node in a one-to-one [relation](!alias-goh5uthoc1) can at most be connected to one node.

## Connect two nodes in a one-to-one relation

Creates a new edge between two nodes specified by their `id`. The according types have to be in the same [relation](!alias-goh5uthoc1).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

> Consider a blog where every post is assigned to additional meta information. Adds a new edge to the relation called `PostMetaInformation` and query the tags stored in the meta information and the post title:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  setPostMetaInformation(
    metaInformationMetaInformationId: "cixnjj4l90ipl0106vp6u7a2f"
    postPostId: "cixnen24p33lo0143bexvr52n"
  ) {
    metaInformationMetaInformation {
      tags
    }
    postPost {
      title
    }
  }
}
---
{
  "data": {
    "setPostMetaInformation": {
      "metaInformationMetaInformation": {
        "tags": [
          "GENERAL"
        ]
      },
      "postPost": {
        "title": "My biggest Adventure"
      }
    }
  }
}
```

Note: First removes existing connections containing one of the specified nodes, then adds the edge connecting both nodes.

You can also use the `updatePost` or `updateMetaInformation` mutations to connect a post with a meta information:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  updatePost(
    id: "cixnen24p33lo0143bexvr52n"
    metaInformationId: "cixnjj4l90ipl0106vp6u7a2f"
  ) {
    metaInformation {
      tags
    }
  }
}
---
{
  "data": {
    "updatePost": {
      "metaInformation": {
        "tags": [
          "GENERAL"
        ]
      }
    }
  }
}
```

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  updateMetaInformation(
    id: "cixnjj4l90ipl0106vp6u7a2f",
    postId: "cixnen24p33lo0143bexvr52n"
  ) {
    post {
      title
    }
  }
}
---
{
 "data": {
   "updateMetaInformation": {
     "post": {
       "title": "My biggest Adventure"
     }
   }
 }
}
```

## Disconnect two nodes in a one-to-one relation

Removes an edge between two nodes speficied by their `id`.

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

> Removes an edge from the relation called `PostMetaInformation` and query the tags stored in the meta information and the post title:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  unsetPostMetaInformation(
    metaInformationMetaInformationId: "cixnjj4l90ipl0106vp6u7a2f"
    postPostId: "cixnen24p33lo0143bexvr52n"
  ) {
    metaInformationMetaInformation {
      tags
    }
    postPost {
      title
    }
  }
}
---
{
  "data": {
    "unsetPostMetaInformation": {
      "metaInformationMetaInformation": {
        "tags": [
          "GENERAL"
        ]
      },
      "postPost": {
        "title": "My biggest Adventure"
      }
    }
  }
}
```
