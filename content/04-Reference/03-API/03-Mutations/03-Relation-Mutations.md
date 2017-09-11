---
alias: kaozu8co6w
description: For every available relation in your GraphQL schema, certain mutations are automatically generated.
---

# Relation Mutations

For every available [relation](!alias-goh5uthoc1) in your [GraphQL schema](!alias-ahwoh2fohj), certain mutations are automatically generated.

The names and arguments of the generated mutations depend on the relation name and its cardinalities. For example, with the following schema:

```graphql
type Post {
  id: ID!
  title: String!
  author: User @relation(name: "WrittenPosts")
  likedBy: [User!]! @relation(name: "LikedPosts")
}

type User {
  id: ID!
  name : String!
  address: Address @relation(name: "UserAddress")
  writtenPosts: [Post!]! @relation(name: "WrittenPosts")
  likedPosts: [Post!]! @relation(name: "LikedPosts")
}

type Address {
  id: ID!
  city: String!
  user: User @relation(name: "UserAddress")
}
```

these relation mutations will be available

* the `setUserAddress` and `unsetUserAddress` mutations [connect and disconnect two nodes](!alias-zeich1raej) in the **one-to-one** relation `UserAddress`.
* the `addToWrittenPosts` and `removeFromWrittenPosts` mutations [connect and disconnect two nodes](!alias-ofee7eseiy) in the **one-to-many** relation `WrittenPosts`.
* the `addToLikedPosts` and `removeFromLikedPosts` mutations [connect and disconnect two nodes](!alias-aengu5iavo) in the a **many-to-many** relation `LikedPosts`.


## Modifying edges for one-to-one relations with the Simple API

A node in a one-to-one [relation](!alias-goh5uthoc1) can at most be connected to one node.

### Connect two nodes in a one-to-one relation

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

### Disconnect two nodes in a one-to-one relation

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




## Modifying edges for one-to-many relations with the Simple API

One-to-many [relations](!alias-goh5uthoc1) relate two types to each other.

A node of the one side of a one-to-many relation can be connected to multiple nodes.
A node of the many side of a one-to-many relation can at most be connected to one node.

### Connect two nodes in a one-to-many relation

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

##### Disconnect two nodes in a one-to-many relation

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



## Modifying edges for many-to-many relations with the Simple API

Nodes in a many-to-many [relations](!alias-goh5uthoc1) can be connected to many nodes.

### Connect two nodes in a many-to-many relation

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

##### Disconnect two nodes in a many-to-many relation

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
