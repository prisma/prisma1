---
alias: aengu5iavo
path: /docs/reference/simple-api/many-to-many-edges
layout: REFERENCE
shorttitle: Modifying many-to-many edges
description: Modify edges for many-to-many relations with the Simple API and connect or disconnect two nodes in your GraphQL backend.
simple_relay_twin: gei0pus9si
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
---

# Modifying edges for many-to-many relations with the Simple API

Nodes in a many-to-many [relations](!alias-goh5uthoc1) can be connected to many nodes.

## Connect two nodes in a many-to-many relation

Creates a new edge between two nodes specified by their `id`. The according [types](!alias-ij2choozae) have to be in the same [relation](!alias-goh5uthoc1).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

> Adds a new edge to the relation called `MovieActors` and query the movie's title and the actor's name:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixos23120m0n0173veiiwrjr
disabled: true
---
mutation {
  addToMovieActors(
    moviesMovieId: "cixos5gtq0ogi0126tvekxo27"
    actorsActorId: "cixxibjo1c1go0131ea1t4yor"
  ) {
    moviesMovie {
      title
    }
    actorsActor {
      name
    }
  }
}
---
{
  "data": {
    "addToMovieActors": {
      "moviesMovie": {
        "title": "Inception"
      },
      "actorsActor": {
        "name": "Leonardo DiCaprio"
      }
    }
  }
}
```

Note: Adds the edge only if this node pair is not connected yet by this relation. Does not remove any edges.

#### Disconnect two nodes in a many-to-many relation

Removes one edge between two nodes specified by `id`

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

> Removes an edge for the relation called `MovieActors` and query the movie's title and the actor's name

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  removeFromMovieActors(
    moviesMovieId: "cixos5gtq0ogi0126tvekxo27"
    actorsActorId: "cixxibjo1c1go0131ea1t4yor"
  ) {
    moviesMovie {
      title
    }
    actorsActor {
      name
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
