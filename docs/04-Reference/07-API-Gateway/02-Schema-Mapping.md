---
alias: phoh6yiwah
description: The primary use case for the API gateway is to expose a custom API.
---

# Schema Mapping

Every Graphcool service comes with an auto-generated [CRUD API](!alias-abogasd0go) for your [data model](!alias-eiroozae8u). The API gateway can be used to _customize_ the API that's exposed to your client applications. This customization can be implemented using the [schema stitching](http://dev.apollodata.com/tools/graphql-tools/schema-stitching.html) API from [`graphql-tools`](https://github.com/apollographql/graphql-tools).


## Example

Consider the following data model that's defined for a Graphcool service:

```graphql
type User implements Node {
  id: ID! @isUnique
  name: String!
  alias: String! @isUnique
  posts: [Post!]! @relation(name: "UserPosts")
}

type Post implements Node {
  id: ID! @isUnique
  title: String!
  author: User! @relation(name: "UserPosts")
}
```

When [deploying](!alias-aiteerae6l#graphcool-deploy) the service, Graphcool will generate the following CRUD API for you:

```graphql
type Query {
  # Read operations for `Post`
  Post(id: ID): Post
  allPosts(filter: PostFilter, orderBy: PostOrderBy, skip: Int, after: String, before: String, first: Int, last: Int): [Post!]!

  # Read operations for `User`
  User(alias: String, id: ID): User
  allUsers(filter: UserFilter, orderBy: UserOrderBy, skip: Int, after: String, before: String, first: Int, last: Int): [User!]!
}

type Mutation {
  # Create, update, delete operations for `Post`
  createPost(title: String!, authorId: ID, author: PostauthorUser): Post
  updatePost(id: ID!, title: String, authorId: ID, author: PostauthorUser): Post
  deletePost(id: ID!): Post

  # Create, update, delete operations for `User`
  createUser(alias: String!, name: String!, postsIds: [ID!], posts: [UserpostsPost!]): User
  updateUser(alias: String, id: ID!, name: String, postsIds: [ID!], posts: [UserpostsPost!]): User
  deleteUser(id: ID!): User

  # Set relation between `Post` and `User` node
  addToUserPosts(postsPostId: ID!, authorUserId: ID!): AddToUserPostsPayload
}
```

