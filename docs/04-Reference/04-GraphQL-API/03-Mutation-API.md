---
alias: ol0yuoz6go
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
---

# Mutation API

## Overview

A *GraphQL mutation* is used to modify data. This is an example mutation:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  createPost(
    title: "My great Vacation"
    slug: "my-great-vacation"
    published: true
    text: "Read about my great vacation."
  ) {
    id
    slug
  }
}
---
{
  "data": {
    "createPost": {
      "id": "cixneo7zp3cda0134h7t4klep",
      "slug": "my-great-vacation"
    }
  }
}
```

Here's a list of available mutations. To explore them, use the [playground](!alias-aiteerae6l#graphcool-playground) for your service.

* Based on the [model types](!alias-eiroozae8u#model-types) and [relations](!alias-eiroozae8u#relations) in your data model, [type mutations](#type-mutations) and [relation mutations](#relation-mutations) will be generated to modify nodes and edges.
* Additionally, [custom mutations](#custom-mutations) can be added to your API using [Resolvers](!alias-su6wu3yoo2) that are implemented as serverless [functions](!alias-aiw4aimie9).


## Type mutations

For every available [model type](!alias-eiroozae8u#model-types) in your data model, certain mutations are automatically generated.

For example, if your schema contains a `Post` type:

```graphql
type Post @model {
  id: ID! @isUnique
  title: String!
  description: String
}
```

the following type mutations will be available:

* the `createPost` mutation [creates a new node](#creating-a-node).
* the `updatePost` mutation [updates an existing node](#updating-a-node).
* the `deletePost` mutation [deletes an existing node](#deleting-a-node).


### Creating a node 

Creates a new node for a specific type that gets assigned a new `id`.
All [required](!alias-eiroozae8u#required) fields of the type without a [default value](!alias-eiroozae8u#default-value) have to be specified, the other fields are optional arguments.

The query response can contain all fields of the newly created node, including the `id` field.

Create a new `Post` node and query its `id` and `slug`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  createPost(
    title: "My great Vacation"
    slug: "my-great-vacation"
    published: true
    text: "Read about my great vacation."
  ) {
    id
    slug
  }
}
---
{
  "data": {
    "createPost": {
      "id": "cixneo7zp3cda0134h7t4klep",
      "slug": "my-great-vacation"
    }
  }
}
```


### Updating a node

Updates [fields](!alias-eiroozae8u#fields) of an existing node of a certain [model type](!alias-eiroozae8u#model-types) specified by the `id` field. The node's fields will be updated according to the additionally provided values. 

The query response can contain all fields of the updated node.

Update the `text` and `published` fields for an existing `Post` node and query its `id`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  updatePost(
    id: "cixnen24p33lo0143bexvr52n"
    text: "This is the start of my biggest adventure!"
    published: true
  ) {
    id
  }
}
---
{
  "data": {
    "updatePost": {
      "id": "cixnen24p33lo0143bexvr52n",
      "text": "This is the start of my biggest adventure!",
      "published": true
    }
  }
}
```


### Deleting a node

Deletes a node specified by the `id` field.

The query response can contain all fields of the deleted node.

Delete an existing `Post` node and query its (then deleted) `id` and `title`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  deletePost(id: "cixneo7zp3cda0134h7t4klep") {
    id
    title
  }
}
---
{
  "data": {
    "id": "cixneo7zp3cda0134h7t4klep",
    "title": "My great Vacation"
  }
}
```


## Relation mutations

For every available [relation](!alias-eiroozae8u#relations) in your data model, certain mutations are automatically generated.

The names and arguments of the generated mutations depend on the relation name and its cardinalities. For example, with the following schema:

```graphql
type Post @model {
  id: ID! @isUnique
  title: String!
  author: User @relation(name: "WrittenPosts")
  likedBy: [User!]! @relation(name: "LikedPosts")
}

type User @model {
  id: ID! @isUnique
  name : String!
  address: Address @relation(name: "UserAddress")
  writtenPosts: [Post!]! @relation(name: "WrittenPosts")
  likedPosts: [Post!]! @relation(name: "LikedPosts")
}

type Address @model {
  id: ID! @isUnique
  city: String!
  user: User @relation(name: "UserAddress")
}
```

the following relation mutations will be available:

- the `setUserAddress` and `unsetUserAddress` mutations [connect and disconnect two nodes](#modifying-edges-for-one-to-one-relations) in the **one-to-one** relation `UserAddress`.
- the `addToWrittenPosts` and `removeFromWrittenPosts` mutations [connect and disconnect two nodes](#modifying-edges-for-one-to-many-relations) in the **one-to-many** relation `WrittenPosts`.
- the `addToLikedPosts` and `removeFromLikedPosts` mutations [connect and disconnect two nodes](#modifying-edges-for-many-to-many-relations) in the a **many-to-many** relation `LikedPosts`.


### Modifying edges for one-to-one relations

A node in a one-to-one [relation](!alias-eiroozae8u#relations) can at most be connected to one node.

#### Connect two nodes in a one-to-one relation

Creates a new edge between two nodes specified by their `id`. The according types have to be in the same [relation](!alias-eiroozae8u#relations).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

Consider a blog where every `Post` node is assigned to additional `MetaInformation`. Add a new edge to the relation called `PostMetaInformation` and query the `tags` stored in the `metaInformation` node as well as the `title`:

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

> Note: First removes existing connections containing one of the specified nodes, then adds the edge connecting both nodes.

You can also use the `updatePost` or `updateMetaInformation` mutations to connect a `Post` node with a `metaInformation` node:

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

#### Disconnect two nodes in a one-to-one relation

Removes an edge between two nodes speficied by their `id`.

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

Remove an edge from the relation called `PostMetaInformation` and query the `tags` stored in the `metaInformation`node and the `title`:

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


### Modifying edges for one-to-many relations

One-to-many [relations](!alias-eiroozae8u#relations) relate two types to each other.

A node of the one side of a one-to-many relation can be connected to multiple nodes. A node of the many side of a one-to-many relation can at most be connected to one node.

#### Connect two nodes in a one-to-many relation

Creates a new edge between two nodes specified by their `id`. The according [model type](!alias-eiroozae8u#model-types) have to be in the same [relations](!alias-eiroozae8u#relations).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

Adds a new edge to the relation called `UserPosts` and query the `name` of the `User` node as well as the `title` of the `Post` node:

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

> Note: Adds the edge only if this node pair is not connected yet by this relation. Does not remove any edges.

###### Disconnect two nodes in a one-to-many relation

Removes one edge between two nodes specified by `id`

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

Removes an edge for the relation called `UserPosts` and query the `id` of the `User` node as well as the `slug` of the `Post` node:

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


### Modifying edges for many-to-many relations

Nodes in a many-to-many [relations](!alias-eiroozae8u#relations) can be connected to many nodes.

#### Connect two nodes in a many-to-many relation

Creates a new edge between two nodes specified by their `id`. The according [model types](!alias-eiroozae8u#model-types) have to be in the same [relations](!alias-eiroozae8u#relations).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

Add a new edge to the relation called `MovieActors` and query the `title` of the `Movie` node as well as the `name` of the `Actor` node:

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

> Note: Adds the edge only if this node pair is not connected yet by this relation. Does not remove any edges.

###### Disconnect two nodes in a many-to-many relation

Removes one edge between two nodes specified by `id`.

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

Removes an edge for the relation called `MovieActors` and query the `title` of the `Movie` node as well as the `name` of the `Actor` node:

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

## Nested mutations

When creating or updating nodes, you can execute _nested mutations_ to interact with connected parts of your type schema.

- to **create and connect to a new node** on the other side of a relation, you can use [nested create mutations](#nested-create-mutations).
- to **connect to an existing node** on the other side of a relation, you can use [nested connect mutations](#nested-connect-mutations).

### Limitations

Different limitations and improvement suggestions are available. Please join the discussion on GitHub!

* [Nested delete mutations](https://github.com/graphcool/feature-requests/issues/42) are not available yet. Neither are [cascading deletes](https://github.com/graphcool/feature-requests/issues/47).
* Currently, the [maximum nested level is 3](https://github.com/graphcool/feature-requests/issues/313). If you want to nest more often than that, you need to split up the nested mutations into two separate mutations.

Many other [suggestions and improvements](https://github.com/graphcool/feature-requests/issues/127) are currently being discussed.


### Nested create mutations

_Nested create mutations_ connect the created node to a new node in the related type.

Consider the following data model:

```graphql
type Author @model {
  id: ID! @isUnique
  contactDetails: ContactDetails @relation(name: "AuthorContactDetails")
  posts: [Post!]! @relation(name: "AuthorPosts")
  description: String!
}

type ContactDetails @model {
  id: ID! @isUnique
  author: Author @relation(name: "AuthorContactDetails")
  email: String!
}

type Post @model {
  id: ID! @isUnique
  text: String!
  author: Author @relation(name: "AuthorPosts")
}
```

We're considering the `createAuthor` and `updateAuthor` mutation to see how to create nested nodes for the *to-one* relation `AuthorContactDetails` and the *to-many* relation `AuthorPosts`.

#### Nested create mutations for to-one relations

Let's explore the available nested create mutations for the `one-to-one` relation `AuthorContactDetails`.

###### Creating a new `Author` node and connect it to new `ContactDetails`

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation createAuthorAndContactDetails {
  createAuthor(
    description: "I am a good author!"
    contactDetails: {
      email: "nilan@graph.cool"
    }
  ) {
    id
    contactDetails {
      id
    }
  }
}
---
{
  "data": {
    "createAuthor": {
      "id": "ciz7573ffx1w70112hwj04hqv",
      "contactDetails": {
        "id": "ciz7573ffx1w80112cjwjduxn"
      }
    }
  }
}

```

Notice that the nested `contactDetails` object that takes the same input arguments as the `createContactDetails` mutation. After running this mutation, a new `Author` and `ContactDetail` node have been created that are connected via the `AuthorContactDetails` relation.

Here's the same mutation using GraphQL variables:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation createAuthorAndContactDetails($contactDetails: AuthorcontactDetailsContactDetails) {
  createAuthor(
    description: "I am a good author!"
    contactDetails: $contactDetails
  ) {
    id
    contactDetails {
      id
    }
  }
}
---
{
  "contactDetails": {
    "email": "nilan@graph.cool"
  }
}
---
{
  "data": {
    "createAuthor": {
      "id": "ciz7573ffx1w70112hwj04hqv",
      "contactDetails": {
        "id": "ciz7573ffx1w80112cjwjduxn"
      }
    }
  }
}

```

Notice the variable type `AuthorcontactDetailsContactDetails` that follows a consistent naming schema:

- The original type name `Author`
- The related field name `contactDetails`
- The related type name `ContactDetails`

You can also find the type name in the documentation in the Playground:

![](./graphql-variables-type-name.png?width=351)

###### Updating an existing `Author` node and connect it to new `ContactDetails`

Similarly, we can update an `Author` node and simultaneously create new `ContactDetails` for it:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation updateAuthorAndCreateContactDetails($contactDetails: AuthorcontactDetailsContactDetails) {
  updateAuthor(
    id: "ciz7573ffx1w70112hwj04hqv"
    description: "I write posts"
    contactDetails: $contactDetails
  ) {
    id
    contactDetails {
      email
    }
  }
}
---
{
  "contactDetails": {
    "email": "johannes@graph.cool"
  }
}
---
{
  "data": {
    "updateAuthor": {
      "id": "ciz7573ffx1w70112hwj04hqv",
      "description": "I write posts"
      "contactDetails": {
        "email": "johannes@graph.cool"
      }
    }
  }
}
```

#### Nested create mutations for to-many relations

Let's explore the available nested create mutations for the `one-to-many` relation `AuthorPosts`.

###### Creating a new `Author` node and connect it to multiple new `Post` nodes

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation createAuthorAndPosts {
  createAuthor(
    description: "I am a good author!"
    posts: [{
      text: "A post of mine"
    }, {
      text: "Another post"
    }]
  ) {
    description
    posts {
      text
    }
  }
}
---
{
  "data": {
    "createAuthor": {
      "description": "I am a good author!",
      "posts": [
        {
          "text": "A post of mine"
        },
        {
          "text": "Another post"
        }
      ]
    }
  }
}
```

Note that the nested `posts` object that takes a list of arguments needed for the `createPost` mutation. After running this mutation, a new `Author` and two `Post` nodes have been created that are now connected via the `AuthorPosts` relation.

Here's the same mutation using GraphQL variables:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation createAuthorAndPosts($posts: [AuthorpostsPost!]) {
  createAuthor(
    description: "I am a good author!"
    posts: $posts
  ) {
    description
    posts {
      text
    }
  }
}
---
{
  "posts": [{
      "text": "A post of mine"
    }, {
      "text": "Another post"
    }]
}
---
{
  "data": {
    "createAuthor": {
      "description": "I am a good author!",
      "posts": [
        {
          "text": "A post of mine"
        },
        {
          "text": "Another post"
        }
      ]
    }
  }
}
```

Note the variable type `[AuthorpostsPost!]` that follows a consistent naming schema:

- The original type name `Author`
- The related field name `posts`
- The related type name `Post`

You can also find the type name in the documentation in the Playground:

![](./graphql-variables-type-name.png)

###### Updating an existing `Author` node and connecting it to multiple new `Post` nodes

Similarly, we can update an author and simultaneously assign it to a new list of new posts:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation updateAuthorAndConnectToPosts($posts: [AuthorpostsPost!]) {
  updateAuthor(
    id: "ciz7573ffx1w70112hwj04hqv"
    description: "I write posts"
    posts: $posts
  ) {
    id
    posts {
      text
    }
  }
}
---
{
  "posts": [{
    "text": "A post of mine"
  }, {
    "text": "Another post"
  }]
}
---
{
  "data": {
    "createAuthor": {
      "description": "I am a good author!",
      "posts": [
        {
          "text": "A post of mine"
        },
        {
          "text": "Another post"
        }
      ]
    }
  }
}
```

> Note: This mutation will *replace* the existing list of posts assigned to the author. If instead you want to *append* more posts to the list, you can [modify the edge](#modifying-edges-for-one-to-many-relations) directly instead.


### Nested connect mutations

_Nested connect mutations_ connect the original node to an existing node in the related type.

Consider the following data model:

```idl
type Author @model {
  id: ID! @isUnique
  contactDetails: ContactDetails @relation(name: "AuthorContactDetails")
  posts: [Post!]! @relation(name: "AuthorPosts")
  description: String!
}

type ContactDetails @model {
  id: ID! @isUnique
  author: Author @relation(name: "AuthorContactDetails")
  email: String!
}

type Post @model {
  id: ID! @isUnique
  text: String!
  author: Author @relation(name: "AuthorPosts")
}
```

We're considering the `createAuthor` and `updateAuthor` mutation to see how to connect nested nodes for the *to-one* relation `AuthorContactDetails` and the *to-many* relation `AuthorPosts`.

#### Nested connect mutations for to-one relations

Let's explore the available nested connect mutations for the `one-to-one` relation `AuthorContactDetails`.

###### Creating a new `Author` node and connecting it to an existing `ContactDetails` node

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation createAuthorAndConnectContactDetails {
  createAuthor(
    description: "I am a good author!"
    contactDetailsId: "ciz7573ffx1w80112cjwjduxn"
  ) {
    id
    contactDetails {
      id
    }
  }
}
---
{
  "data": {
    "createAuthor": {
      "id": "ciz7573ffx1w70112hwj04hqv",
      "contactDetails": {
        "id": "ciz7573ffx1w80112cjwjduxn"
      }
    }
  }
}
```

Notice the nested `contactDetailsId` argument that gets passed the `id` of an existing `ContactDetails` node. After running this mutation, the new `Author` node and the existing `ContactDetails` node are connected via the `AuthorContactDetails` relation.

###### Updating an existing `Author` node and connecting it to an existing `ContactDetails` node

Similarly, we can update an `Author` node and simultaneously connect it to an existing `ContactDetails` node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation updateAuthorAndConnectContactDetails {
  updateAuthor(
    id: "ciz7573ffx1w70112hwj04hqv"
    description: "I write posts"
    contactDetailsId: "ciz7573ffx1w80112cjwjduxn"
  ) {
    id
    contactDetails {
      email
    }
  }
}
---
{
  "data": {
    "updateAuthor": {
      "id": "ciz7573ffx1w70112hwj04hqv",
      "description": "I write posts"
      "contactDetails": {
        "email": "nilan@graph.cool"
      }
    }
  }
}
```

#### Nested connect mutations for to-many relations

Let's explore the available nested connect mutations for the `one-to-many` relation `AuthorPosts`.

###### Creating a new `Author` node and connecting it to multiple existing `Post` nodes

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation createAuthorAndConnectPosts($postsIds: [ID!]) {
  createAuthor(
    description: "I am a good author!"
    postsIds: $postsIds
  ) {
    description
    posts {
      text
    }
  }
}
---
{
  "postsIds": ["ciz787j6eqmf5014929vvo2hp", "ciz787j6eqmf60149lg3jvi4r"]
}
---
{
  "data": {
    "createAuthor": {
      "description": "I am a good author!",
      "posts": [
        {
          "text": "A post of mine"
        },
        {
          "text": "Another post"
        }
      ]
    }
  }
}
```

Notice the nested `postsIds` list of `Post` ids. After running this mutation, the new `Author` node and the existing `Post` nodes are now connected via the `AuthorPosts` relation.

###### Updating an existing `Author` node and connecting it to multiple new `Post` nodes

Similarly, we can update an `Author` node and simultaneously assign it to a new list of existing `Post` nodes:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/ciz751zxu2nnd01494hr653xl
disabled: true
---
mutation updateAuthorAndConnectPosts($postsIds: [ID!]) {
  updateAuthor(
    id: "ciz7573ffx1w70112hwj04hqv"
    description: "I write posts"
    postsIds: $postsIds
  ) {
    id
    posts {
      text
    }
  }
}
---
{
  "postsIds": ["ciz787j6eqmf5014929vvo2hp", "ciz787j6eqmf60149lg3jvi4r"]
}
---
{
  "data": {
    "createAuthor": {
      "description": "I write posts",
      "posts": [
        {
          "text": "A post of mine"
        },
        {
          "text": "Another post"
        }
      ]
    }
  }
}
```

> Note: This mutation will *replace* the existing list of `Post` nodes assigned to the `Author` node. If instead you want to *append* more posts to the list, you can [modify the edge](#modifying-edges-for-one-to-many-relations) directly.


## Custom mutations

Custom mutations can be added to your GraphQL API using [Resolver](!alias-xohbu7uf2e) functions.

You can define the **name, input arguments and payload of the mutation** and **resolve it with a Graphcool Function**.

### Example

#### Return a random number in a specified range

Schema Extension SDL document:

```graphql
type RandomNumberPayload {
  number: Float!
}

extend type Mutation {
  randomNumber(min: Int!, max: Int!): RandomNumberPayload
}
```

Graphcool Function:

```js
module.exports = function randomNumber(event) {
  const min = event.data.min
  const max = event.data.max

  if (min > max) {
    return {
      error: "Invalid input"
    }
  }

  const number = Math.random() * (max - min) + min

  return {
    data: {
      number
    }
  }
}
```

Then the mutation can be called like this using the Simple API:

```graphql
mutation {
  isValidAge(age: 12) {
    isValid # false
    age # 12
  }
}
```

Note that the returned object contains a `data` key, which in turn contains the `number` field that was specified in the `RandomNumberPayload` in the SDL document. [Error handling](!alias-quawa7aed0) works similarly to other Graphcool Functions, if an object containing the `error` key is returned.


## Working with files (only for [legacy Console projects](!alias-aemieb1aev))

To interact with the [File API](!alias-eer4wiang0) of the platform, you can create, rename or delete files through queries and mutations that are exposed in the Simple API.

### Uploading files

Uploading files with a GraphQL mutation is not supported yet. For now, use the [File API](!alias-eer4wiang0) directly to upload files.

### Reading meta information of files

To query the meta information stored for a file, use the `allFiles` or `File` queries.

To query a specific file use one of the unique fields `id`, `secret` or `url` fields to specify the file node:

```graphql
query {
  File(id: "my-file-id") {
    id
    name
  }
}
```

Similarly, the `allFiles` query can be used to query for multiple file nodes.

### Renaming files

To rename a file, use the `updateFile` mutation and choose a new value for the `name` field:

```graphql
mutation {
  updateFile(
    id: "my-file-id"
    name: "new-comment-name.png"
  ) {
    file {
      id
      name
    }
  }
}
```

### Deleting files

To delete a file, use the `deleteFile` mutation as you would use any other delete mutation:

```graphql
mutation {
  deleteFile(id: "my-file-id") {
    file {
      id
    }
  }
}
```
