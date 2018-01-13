---
alias: ol0yuoz6go
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
---

# Mutations

The Prisma API offers

* **Simple Mutations**, to create, update, upsert and delete single nodes of a certain model
* **Batch Mutations**, to update and delete many nodes of a certain model
* **Relation Mutations**, to connect, disconnect, create, update and upsert nodes across relations

In general, the Prisma API of a service is structured according to [its data model](!alias-TODO N). To explore it, use the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

## Examples

Consider a Prisma service with this data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  isPublished: Boolean!
  author: User!
}

type User {
  id: ID! @unique
  age: Int!
  email: String! @unique
  name: String!
  posts: [Post!]!
}
```

### Model Mutations

```graphql
# Create a new user
mutation {
  createUser(
    data: {
      age: 42
      email: "zeus@example.com"
      name: "Zeus"
    }
  ) {
    id
    name
  }
}
```

```graphql
# Update an existing user
mutation {
  updateUser(
    data: {
      age: 42
      name: "Zeus"
    }
  ) {
    id
    name
  }
}
```

```graphql
# Upsert a user
mutation {
  upsertUser(
    where: {
      email: "zeus@example.com"
    }
    create: {
      email: "zeus@example.com"
      age: 42
      name: "Zeus"
    }
    update: {
      name: "Another Zeus"
    }
  ) {
    name
  }
}
```

```graphql
# Delete a user
mutation {
  deleteUser(where: {
    id: "cjcdi63l20adx0146vg20j1ck"
  }) {
    id
  }
}
```

### Batch Mutations

```graphql
# Publish all unpublished posts that were created in 2017
mutation {
  updateManyPosts(
    where: {
      createdAt_gte: "2017"
      createdAt_lt: "2018"
      isPublished: false
    }
    data: {
      isPublished: true
    }
  ) {
    count
  }
}
```

```graphql
# Delete all unpublished posts of a certain author
mutation {
  deleteManyPosts(
    where: {
      isPublished: false
      author: {
        name: "Zeus"
      }
    }
  ) {
    count
  }
}
```

### Relation Mutations

```graphql
# Create a user, create and connect new posts, and connect to existing posts
mutation {
  createUser(
    data: {
			email: "zeus@example.com"
      name: "Zeus"
      age: 42
      posts: {
        create: [{
          isPublished: true
          title: "name"
        }, {
          isPublished: true
          title: "name"
        }]
        connect: [{
          id: "cjcdi63j80adw0146z7r59bn5"
        }, {
          id: "cjcdi63l80ady014658ud1u02"
        }]
      }
    }
  ) {
    id
    posts {
      id
    }
  }
}
```

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
  updatePost(
    data: {
      text: "This is the start of my biggest adventure!"
      published: true
    }
    where: {
      id: "cixnen24p33lo0143bexvr52n"  # or any other unique field
    }
  ) {
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
    id: "cixneo7zp3cda0134h7t4klep"  # or any other unique field
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
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
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

### "To-one"-relations

#### Connect two nodes in a "to-one"-relation

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

#### Create a new node for a "to-one"-relation

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

#### Update a node in a "to-one"-relation

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
          name: "John Doe-Doe"
        }
      }
    }
  }) {
    id
  }
}
```

#### Update or create a node in a "to-one"-relation

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
          create: {
            name: "John Doe-Doe"
          }
          update: {
            name: "John Doe-Doe"
          }
        }
      }
    }
  }) {
    id
  }
}
```

#### Delete a node in a "to-one"-relation

Updates a related node by its `id` (or any other unique field).

Deletes the `User` node by a related `Post` node where it's the `author`:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      author: {
        delete: true
      }
    }
  }) {
    id
  }
}
```

#### Disconnect two nodes in a "to-one"-relation

Removes an edge between two nodes speficied by their `id` (or any other unique field).

Disconnecting a `Post` from a `User` node can be done as follows:

```graphql
mutation {
  updatePost(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
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

### "To-many"-relations

#### Connect nodes to a "to-many"-relation

Creates new edges between a number of nodes specified by their `id` (or any other unique field).

Connect two existing `Post` nodes with an existing `User` node:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      posts: {
        connect: [
          { id: "thei9je6kaes4raighahzoo7u" }, # or any other unique field
          { id: "pheishaicierahmeequai1oox" }  # or any other unique field
        ]
      }
    }
  ) {
    id
  }
}
```

#### Create nodes in a "to-many"-relation

Creates new nodes and connects them to an existing node specified by its `id` (or any other unique field).

Create two new `Post` nodes and connect them with an existing `User` node:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      posts: {
        create: [
          { title: "GraphQL is awesome" },
          { title: "I love GraphQL" }
        ]
      }
    }
  ) {
    id
  }
}
```

#### Update existing nodes in a "to-many"-relation

Updates existing nodes by a related node specified by its `id` (or any other unique field).

Update the titles of two existing `Post` nodes by a related `User` node:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      posts: {
        update: [
          {
            where: { id: "thei9je6kaes4raighahzoo7u" } # or any other unique field
            data: { title: "GraphQL is great" }
          },
          {
            where: { id: "pheishaicierahmeequai1oox" } # or any other unique field
            data: { title: "I love GraphQL very much" }
          }
        ]
      }
    }
  ) {
    id
  }
}
```

#### Update or create nodes in a "to-many"-relation

Updates existing nodes by a related node specified by its `id` (or any other unique field) or creates them if they didn't exist before.

Update the titles of two existing `Post` nodes by a related `User` node or create them if they didn't exist before:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      posts: {
        update: [
          {
            where: { id: "thei9je6kaes4raighahzoo7u" } # or any other unique field
            create: { title: "GraphQL is great" }
            update: { title: "GraphQL is great" }
          },
          {
            where: { id: "pheishaicierahmeequai1oox" } # or any other unique field
            create: { title: "I love GraphQL very much" }
            update: { title: "I love GraphQL very much" }
          }
        ]
      }
    }
  ) {
    id
  }
}
```

#### Delete nodes in a "to-many"-relation

Deletes existing nodes by a related node specified by its `id` (or any other unique field).

Delete two existing `Post` nodes by a related `User` node:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      posts: {
        delete: [
         { id: "thei9je6kaes4raighahzoo7u" }, # or any other unique field
         { id: "pheishaicierahmeequai1oox" }  # or any other unique field
        ]
      }
    }
  ) {
    id
  }
}
```

#### Disconnect nodes in a "to-many"-relation

Removes one edge between two nodes specified by `id` (or any other unique field).

The query response can contain both nodes of the former edge. The names of query arguments and node names depend on the field names of the relation.

Disconnect a `User` node from two existing `Post` nodes:

```graphql
mutation {
  updateUser(
    where: {
      id: "cixnen24p33lo0143bexvr52n" # or any other unique field
    }
    data: {
      posts: {
        disconnect: [
          { id: "thei9je6kaes4raighahzoo7u" }, # or any other unique field
          { id: "pheishaicierahmeequai1oox" }  # or any other unique field
        ]
      }
    }
  ) {
    id
  }
}
```
