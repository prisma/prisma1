---
alias: ol0yuoz6go
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
---

# Mutation API

## Overview

A _GraphQL mutation_ is used to modify data. This is an example mutation:

```graphql
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
```

Here's a list of available mutations. To explore them, use the [playground](!alias-aiteerae6l#graphcool-playground) for your service.

* Based on the [model types](!alias-eiroozae8u#model-types) and [relations](!alias-eiroozae8u#relations) in your data model, [type mutations](#type-mutations) and [relation mutations](#relation-mutations) will be generated to modify nodes and edges.

## Type mutations

For every available [model type](!alias-eiroozae8u#model-types) in your data model, certain mutations are automatically generated.

For example, if your schema contains a `Post` type:

```graphql
type Post {
  id: ID! @unique
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
All [required](!alias-eiroozae8u#required) fields of the type without a [default value](!alias-eiroozae8u#default-value) have to be specified, the other fields are optional arguments. All mutation arguments are wrapped in a single input object called `data`.

The query response can contain all fields of the newly created node, including the `id` field.

Create a new `Post` node and query its `id` and `slug`:

```graphql
mutation {
  createPost(data: {
    title: "My great Vacation"
    slug: "my-great-vacation"
    published: true
    text: "Read about my great vacation."
  }) {
    id
    slug
  }
}
```

### Updating a node

Updates [fields](!alias-eiroozae8u#fields) of an existing node of a certain [model type](!alias-eiroozae8u#model-types) specified by the `id` (or any other unique) field. The node's fields will be updated according to the additionally provided values which are wrapped in the `data` input object.

The query response can contain all fields of the updated node.

Update the `text` and `published` fields for an existing `Post` node and query its `id`:

```graphql
mutation {
  updatePost(data: {
    text: "This is the start of my biggest adventure!"
    published: true
  }, where: {
    id: "cixnen24p33lo0143bexvr52n"
  }) {
    id
  }
}
```

### Deleting a node

Deletes a node specified by the `id` (or any other unique) field.

The query response can contain all fields of the deleted node.

Delete an existing `Post` node and query its (then deleted) `id` and `title`:

```graphql
mutation {
  deletePost(where: {
    id: "cixneo7zp3cda0134h7t4klep"
  }) {
    id
    title
  }
}
```

## Relation mutations

To update the relations between nodes, you need to use the `update`-mutation for the corresponding type.

The following data model will be used for all examples in this section:

```graphql
type User {
  id: ID! @unique
  username: String! @unique
  name: String
  posts: [Post!]!
}

type Post {
  id: ID! @unique
  title: String!
  author: User
}
```

In order to apply a relation mutation on the `author` field of `Post`, you need to use the following base syntax (note the mutation is not complete):

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n"
    }
    data: {
      author: {
        <operation>: <data>
      }
    }
  ) {
    id
  }
}
```

The above mutation has two placeholders that need to be properly set in an actual mutation:

- `<operation>`: This specifies the _kind_ of relation mutation and can be either of the following: `create`, `update`, `delete`, `upsert`, `connect`, `disconnect`
- `<data>`: The input data for the corresponding operation

See the following examples to see different scenarios in practice.

### Connect two nodes in a "to-one"-relation

Creates a new edge between two nodes specified by their `id` (or any other unique field).

Connecting a `Post` with a `User` node can be done as follows:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      author: {
        connect: {
          username: "alice" # or any other unique field
        }
      }
    }
  }) {
    id
  }
}
```

### Create a new node for a "to-one"-relation

Creates a new node and connects it with an existing node specified by its `id` (or any other unique field).

Connecting an existing `Post` node with a newly created `User` node can be done as follows:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      author: {
        create: {
          username: "johnny"
          name: "Johne Doe"
        }
      }
    }
  }) {
    id
  }
}
```

### Update a node in a "to-one"-relation

Updates a related node by its `id` (or any other unique field).

Updating the `name` of an existing `User` node by a related `Post` node where it's the `author`:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      author: {
        update: {
          id: "cixneo7zp3cda0134h7t4klep" # or any other unique field
          name: "John Doe-Doe"
        }
      }
    }
  }) {
    id
  }
}
```

### Update or create a node in a "to-one"-relation

Updates or creates a related node by its `id` (or any other unique field).

Updating the `name` of an existing `User` node by a related `Post` node where it's the `author` if it already exists, otherwise create it:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      author: {
        upsert: {
          id: "cixneo7zp3cda0134h7t4klep" # or any other unique field
          name: "John Doe-Doe"
        }
      }
    }
  }) {
    id
  }
}
```

### Delete a node in a "to-one"-relation

Updates a related node by its `id` (or any other unique field).

Updating the `name` of an existing `User` node by a related `Post` node where it's the `author`:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      author: {
        delete: {
          id: "cixneo7zp3cda0134h7t4klep" # or any other unique field
        }
      }
    }
  }) {
    id
  }
}
```

### Disconnect two nodes in a "to-one"-relation

Removes an edge between two nodes speficied by their `id` (or any other unique field).

Disconnecting a `Post` from a `User` node can be done as follows:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n"
    }
    data: {
      author: {
        disconnect: true
      }
    }
  ) {
    id
  }
}
```

### Modifying edges for "to-many"-relations

#### Add nodes to a "to-many"-relation

Creates a new edge between two nodes specified by their `id` (or any other unique field).

The query response can contain both nodes of the new edge. The names of query arguments and node names depend on the field names of the relation.

Connect two existing `Post` nodes with a `User` node:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n"
    }
    data: {
      posts: {
        connect: [
          { id: "thei9je6kaes4raighahzoo7u" },
          { id: "pheishaicierahmeequai1oox" }
        ]
      }
    }
  ) {
    id
  }
}
```

###### Disconnect nodes in a "to-many"-relation

Removes one edge between two nodes specified by `id` (or any other unique field).

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

Disconnect a `User` node from two existing `Post` nodes:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n"
    }
    data: {
      posts: {
        disconnect: [
          { id: "thei9je6kaes4raighahzoo7u" },
          { id: "pheishaicierahmeequai1oox" }
        ]
      }
    }
  ) {
    id
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
type Author {
  id: ID! @unique
  contactDetails: ContactDetails @relation(name: "AuthorContactDetails")
  posts: [Post!]! @relation(name: "AuthorPosts")
  description: String!
}

type ContactDetails {
  id: ID! @unique
  author: Author @relation(name: "AuthorContactDetails")
  email: String!
}

type Post {
  id: ID! @unique
  text: String!
  author: Author @relation(name: "AuthorPosts")
}
```

We're considering the `createAuthor` and `updateAuthor` mutation to see how to create nested nodes for the *to-one* relation `AuthorContactDetails` and the *to-many* relation `AuthorPosts`.

#### Nested create mutations for to-one relations

Let's explore the available nested create mutations for the `one-to-one` relation `AuthorContactDetails`.

###### Creating a new `Author` node and connect it to new `ContactDetails`

Notice that the nested `contactDetails` object that takes the same input arguments as the `createContactDetails` mutation. After running this mutation, a new `Author` and `ContactDetail` node have been created that are connected via the `AuthorContactDetails` relation.

Here's the same mutation using GraphQL variables:


Notice the variable type `AuthorcontactDetailsContactDetails` that follows a consistent naming schema:

- The original type name `Author`
- The related field name `contactDetails`
- The related type name `ContactDetails`

You can also find the type name in the documentation in the Playground:

![](./graphql-variables-type-name.png?width=351)

###### Updating an existing `Author` node and connect it to new `ContactDetails`

Similarly, we can update an `Author` node and simultaneously create new `ContactDetails` for it:

```graphql

```

#### Nested create mutations for to-many relations

Let's explore the available nested create mutations for the `one-to-many` relation `AuthorPosts`.

###### Creating a new `Author` node and connect it to multiple new `Post` nodes

```graphql

```

Note that the nested `posts` object that takes a list of arguments needed for the `createPost` mutation. After running this mutation, a new `Author` and two `Post` nodes have been created that are now connected via the `AuthorPosts` relation.

Here's the same mutation using GraphQL variables:

```graphql

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

```

> Note: This mutation will *replace* the existing list of posts assigned to the author. If instead you want to *append* more posts to the list, you can [modify the edge](#modifying-edges-for-one-to-many-relations) directly instead.

### Nested connect mutations

_Nested connect mutations_ connect the original node to an existing node in the related type.

Consider the following data model:

```idl
type Author {
  id: ID! @unique
  contactDetails: ContactDetails @relation(name: "AuthorContactDetails")
  posts: [Post!]! @relation(name: "AuthorPosts")
  description: String!
}

type ContactDetails {
  id: ID! @unique
  author: Author @relation(name: "AuthorContactDetails")
  email: String!
}

type Post {
  id: ID! @unique
  text: String!
  author: Author @relation(name: "AuthorPosts")
}
```

We're considering the `createAuthor` and `updateAuthor` mutation to see how to connect nested nodes for the *to-one* relation `AuthorContactDetails` and the *to-many* relation `AuthorPosts`.

#### Nested connect mutations for to-one relations

Let's explore the available nested connect mutations for the `one-to-one` relation `AuthorContactDetails`.

###### Creating a new `Author` node and connecting it to an existing `ContactDetails` node

```graphql

```

Notice the nested `contactDetailsId` argument that gets passed the `id` of an existing `ContactDetails` node. After running this mutation, the new `Author` node and the existing `ContactDetails` node are connected via the `AuthorContactDetails` relation.

###### Updating an existing `Author` node and connecting it to an existing `ContactDetails` node

Similarly, we can update an `Author` node and simultaneously connect it to an existing `ContactDetails` node:

```graphql

```

#### Nested connect mutations for to-many relations

Let's explore the available nested connect mutations for the `one-to-many` relation `AuthorPosts`.

###### Creating a new `Author` node and connecting it to multiple existing `Post` nodes

```graphql

```

Notice the nested `postsIds` list of `Post` ids. After running this mutation, the new `Author` node and the existing `Post` nodes are now connected via the `AuthorPosts` relation.

###### Updating an existing `Author` node and connecting it to multiple new `Post` nodes

Similarly, we can update an `Author` node and simultaneously assign it to a new list of existing `Post` nodes:

```graphql

```

> Note: This mutation will *replace* the existing list of `Post` nodes assigned to the `Author` node. If instead you want to *append* more posts to the list, you can [modify the edge](#modifying-edges-for-one-to-many-relations) directly.