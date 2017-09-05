---
alias: kaozu8co6w
path: /docs/reference/simple-api/relation-mutations
layout: REFERENCE
shorttitle: Relation Mutations
description: For every available relation in your GraphQL schema, certain mutations are automatically generated.
simple_relay_twin: iephah3lae
tags:
  - simple-api
  - mutations
related:
  further:
  more:
---

# Relation Mutations in the Simple API

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
