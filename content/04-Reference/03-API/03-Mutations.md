---
alias: ol0yuoz6go
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
---

# Mutations

A *GraphQL mutation* is used to modify data at a GraphQL [endpoint](!alias-yahph3foch#project-endpoints). This is an example mutation:

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

Here's a list of available mutations. To explore them, use the [playground](!alias-oe1ier4iej) inside your project.

* Based on the [types](!alias-ij2choozae) and [relations](!alias-goh5uthoc1) in your [GraphQL schema](!alias-ahwoh2fohj), [type mutations](!alias-eamaiva5yu) and [relation mutations](!alias-kaozu8co6w) will be generated to modify nodes and edges.
* Additionally, [custom mutations](!alias-thiele0aop) can be added to your API using [Schema Extensions](!alias-xohbu7uf2e).



## Type Mutations

For every available [type](!alias-ij2choozae) in your [GraphQL schema](!alias-ahwoh2fohj), certain mutations are automatically generated.

For example, if your schema contains a `Post` type:

```graphql
type Post {
  id: ID!
  title: String!
  description: String
}
```

the following type mutations will be available:

* the `createPost` mutation [creates a new node](!alias-wooghee1za).
* the `updatePost` mutation [updates an existing node](!alias-cahkav7nei).
* the `deletePost` mutation [deletes an existing node](!alias-fasie2rahv).


### Creating a node 

Creates a new node for a specific type that gets assigned a new `id`.
All [required](!alias-teizeit5se#required) fields of the type without a [default value](!alias-teizeit5se#default-value) have to be specified, the other fields are optional arguments.

The query response can contain all fields of the newly created node, including the `id` field.

> Create a new post and query its id:

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


### Updating nodes with the Simple API

Updates [fields](!alias-teizeit5se) of an existing node of a certain [type](!alias-ij2choozae) specified by the `id` field. The node's fields will be updated according to the additionally provided values.

The query response can contain all fields of the updated node.

> Update the text and published fields for an existing post and query its id:

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


### Deleting nodes

Deletes a node specified by the `id` field.

The query response can contain all fields of the deleted node.

> Delete an existing post and query its id and title:

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


## Relation Mutations

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


### Modifying edges for one-to-one relations with the Simple API

A node in a one-to-one [relation](!alias-goh5uthoc1) can at most be connected to one node.

#### Connect two nodes in a one-to-one relation

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

#### Disconnect two nodes in a one-to-one relation

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




### Modifying edges for one-to-many relations with the Simple API

One-to-many [relations](!alias-goh5uthoc1) relate two types to each other.

A node of the one side of a one-to-many relation can be connected to multiple nodes.
A node of the many side of a one-to-many relation can at most be connected to one node.

#### Connect two nodes in a one-to-many relation

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

###### Disconnect two nodes in a one-to-many relation

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



### Modifying edges for many-to-many relations with the Simple API

Nodes in a many-to-many [relations](!alias-goh5uthoc1) can be connected to many nodes.

#### Connect two nodes in a many-to-many relation

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

###### Disconnect two nodes in a many-to-many relation

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



## Nested Mutations

When creating or updating nodes, you can execute _nested mutations_ to interact with connected parts of your type schema.

* to **create and connect to a new node** on the other side of a relation, you can use [nested create mutations](!alias-vaet3eengo).
* to **connect to an existing node** on the other side of a relation, you can use [nested connect mutations](!alias-tu9ohwa1ui).

### Limitations

Different limitations and improvement suggestions are available. Please join the discussion on GitHub!

* [Nested delete mutations](https://github.com/graphcool/feature-requests/issues/42) are not available yet. Neither are [cascading deletes](https://github.com/graphcool/feature-requests/issues/47).
* Currently, the [maximum nested level is 3](https://github.com/graphcool/feature-requests/issues/313). If you want to nest more often than that, you need to split up the nested mutations into two separate mutations.

Many other [suggestions and improvements](https://github.com/graphcool/feature-requests/issues/127) are currently being discussed.


### Nested Create Mutations

Nested create mutations connect the created node to a new node in the related type.
Let's assume the following schema:

```idl
type Author {
  id: ID!
  contactDetails: ContactDetails @relation(name: "AuthorContactDetails")
  posts: [Post!]! @relation(name: "AuthorPosts")
  description: String!
}

type ContactDetails {
  id: ID!
  author: Author @relation(name: "AuthorContactDetails")
  email: String!
}

type Post {
  id: ID!
  text: String!
  author: Author @relation(name: "AuthorPosts")
}
```

We're considering the `createAuthor` and `updateAuthor` mutation to see how to create nested nodes for the *to-one* relation `AuthorContactDetails` and the *to-many* relation `AuthorPosts`.

#### Nested Create Mutations for to-one Relations

Let's explore the available nested create mutations for the `one-to-one` relation `AuthorContactDetails`.

###### Creating a new Author node and connect it to new ContactDetails

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

Note the nested `contactDetails` object that takes the same input arguments as the `createContactDetails` mutation. After running this mutation, new author and contact detail node have been created that are now connected via the `AuthorContactDetails` relation.

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

Note the variable type `AuthorcontactDetailsContactDetails` that follows a consistent naming schema:

* The original type name `Author`
* The related field name `contactDetails`
* The related type name `ContactDetails`

You can also find the type name in the documentation in the Playground:

![](./graphql-variables-type-name.png?width=351)

###### Updating an existing Author and connect it to new ContactDetails

Similarly, we can update an author and simultaneously create new contact details for that author:

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

#### Nested Create Mutations for to-many Relations

Let's explore the available nested create mutations for the `one-to-many` relation `AuthorPosts`.

###### Creating a new Author node and connect it to multiple new Posts

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

Note the nested `posts` object that takes a list of arguments needed for the `createPost` mutation. After running this mutation, new author and post nodes have been created that are now connected via the `AuthorPosts` relation.

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

* The original type name `Author`
* The related field name `posts`
* The related type name `Post`

You can also find the type name in the documentation in the Playground:

![](./graphql-variables-type-name.png)

###### Updating an existing Author and Connecting it to multiple new Posts

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

> Note: This mutation will *replace* the existing list of posts assigned to the author. If instead you want to *append* more posts to the list, you can [modify the edge](!alias-ofee7eseiy) directly instead.



### Nested Connect Mutations

Nested connect mutations connect the original node to an existing node in the related type.
Let's assume the following schema:

```idl
type Author {
  id: ID!
  contactDetails: ContactDetails @relation(name: "AuthorContactDetails")
  posts: [Post!]! @relation(name: "AuthorPosts")
  description: String!
}

type ContactDetails {
  id: ID!
  author: Author @relation(name: "AuthorContactDetails")
  email: String!
}

type Post {
  id: ID!
  text: String!
  author: Author @relation(name: "AuthorPosts")
}
```

We're considering the `createAuthor` and `updateAuthor` mutation to see how to connect nested nodes for the *to-one* relation `AuthorContactDetails` and the *to-many* relation `AuthorPosts`.

#### Nested Connect Mutations for to-one Relations

Let's explore the available nested connect mutations for the `one-to-one` relation `AuthorContactDetails`.

###### Creating a new Author node and connect it to existing ContactDetails

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

Note the nested `contactDetailsId` argument that we pass the id of an existing `ContactDetails` node. After running this mutation, the new author node and the existing contact detail node are connected via the `AuthorContactDetails` relation.

###### Updating an existing Author and connect it to existing ContactDetails

Similarly, we can update an author and simultaneously connect it to existing contact details:

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

#### Nested Connect Mutations for to-many Relations

Let's explore the available nested connect mutations for the `one-to-many` relation `AuthorPosts`.

###### Creating a new Author node and connect it to multiple existing Posts

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

Note the nested `postsIds` list of `Post` ids. After running this mutation, the new author node and the existing post nodes are now connected via the `AuthorPosts` relation.

###### Updating an existing Author and Connecting it to multiple new Posts

Similarly, we can update an author and simultaneously assign it to a new list of existing posts:

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

> Note: This mutation will *replace* the existing list of posts assigned to the author. If instead you want to *append* more posts to the list, you can [modify the edge](!alias-ofee7eseiy) directly instead.


## Custom Mutations

Custom mutations can be added to your GraphQL API using [Schema Extensions](!alias-xohbu7uf2e).

You can define the **name, input arguments and payload of the mutation** and **resolve it with a Graphcool Function**.

### Example

> Return a random number in a specified range

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



## Working with Files

To interact with the [file management](!alias-eer4wiang0) of the platform, you can create, rename or delete files through queries and mutations exposed in the Simple API.

### Uploading files

Uploading files with a GraphQL mutation is not supported yet. For now, use [the File API directly](!alias-eer4wiang0) to upload files.

### Reading meta information of files

To query the meta information stored for a file, use the `allFiles` or `File` queries.

To query a specific file use one of the unique fields `id`, `secret` or `url` fields to [specify the file node](!alias-ua6eer7shu):

```graphql
query {
  File(id: "my-file-id") {
    id
    name
  }
}
```

Similarly, the `allFiles` query can be used to query for [multiple file nodes](!alias-pa2aothaec).

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
