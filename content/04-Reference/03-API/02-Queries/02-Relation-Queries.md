---
alias: aihaeph5ip
description: For every available relation in your GraphQL schema, certain queries are automatically generated.
---

# Relation Queries

Every available [relation](!alias-goh5uthoc1) in your [GraphQL schema](!alias-ahwoh2fohj) adds a new field to the [type queries](!alias-chuilei3ce) of the two connected types.

For example, with the following schema:

```graphql
type Post {
  id: ID!
  title: String!
  author: User @relation(name: "UserOnPost")
}

type User {
  id: ID!
  name : String!
  posts: [Post!]! @relation(name: "UserOnPost")
}
```

the following fields will be available:

* the `Post` and `allPosts` queries expose a new `author` field to [traverse one node](!alias-ian3cae2oh).
* the `User` and `allUsers` queries expose a new `posts` field to [traverse many nodes](!alias-ohree5pu0y) and a `_postsMeta` to fetch [relation aggregation data](!alias-taesee4ua7).


## Traversing One Node

Traversing edges that connect the current node to the one side of a relation can be done by simply selecting the according field defined with the relation.

> Query information on the author node connected to a specific post node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  Post(id: "cixnen24p33lo0143bexvr52n") {
    id
    author {
      id
      name
      email
    }
  }
}
---
{
  "data": {
    "Post": {
      "id": "cixnen24p33lo0143bexvr52n",
      "author": {
        "id": "cixnekqnu2ify0134ekw4pox8",
        "name": "John Doe",
        "email": "john.doe@example.com"
      }
    }
  }
}
```

The `author` field exposes a further selection of properties that are defined on the `Author` type.

> Note: You can add [filter query arguments](!alias-xookaexai0) to an inner field returning a single node.



## Traversing Many Nodes

In the Simple API, traversing edges connecting the current node to the many side of a relation works the same as for a one side of a relation. Simply select the relation field.

> Query information on all post nodes of a certain author node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  User(id: "cixnekqnu2ify0134ekw4pox8") {
    id
    name
    posts {
      id
      published
    }
  }
}
---
{
  "data": {
    "User": {
      "id": "cixnekqnu2ify0134ekw4pox8",
      "name": "John Doe",
      "posts": [
        {
          "id": "cixnen24p33lo0143bexvr52n",
          "published": false
        },
        {
          "id": "cixnenqen38mb0134o0jp1svy",
          "published": true
        },
        {
          "id": "cixneo7zp3cda0134h7t4klep",
          "published": true
        }
      ]
    }
  }
}
```

The `posts` field exposes a further selection of properties that are defined on the `Post` type.

> Note: [Query arguments](!alias-ohrai1theo) for an inner field returning multiple nodes work similar as elsewhere.



## Relation Aggregation

Nodes connected to multiple nodes via a one-to-many or many-to-many edge expose the `_edgeMeta` field that can be used to query meta information of a connection rather than the actual connected nodes.

> Query meta information on all post nodes of a certain author node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  User(id: "cixnekqnu2ify0134ekw4pox8") {
    id
    name
    _postsMeta {
      count
    }
  }
}
---
{
  "data": {
    "User": {
      "id": "cixnekqnu2ify0134ekw4pox8",
      "name": "John Doe",
      "_postsMeta": {
        "count": 3
      }
    }
  }
}
```

> Query meta information on certain post nodes of a certain author node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  User(id: "cixnekqnu2ify0134ekw4pox8") {
    id
    name
    _postsMeta(filter: {
      title_contains: "adventure"
    }) {
      count
    }
  }
}
---
{
  "data": {
    "User": {
      "id": "cixnekqnu2ify0134ekw4pox8",
      "name": "John Doe",
      "_postsMeta": {
        "count": 1
      }
    }
  }
}
```
