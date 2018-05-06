---
alias: ol0yuoz6go
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
---

# Mutations

The Prisma API offers

* **Simple mutations**: Create, update, upsert and delete single nodes of a certain object type
* **Batch mutations**: Update and delete many nodes of a certain model
* **Relation mutations**: Connect, disconnect, create, update and upsert nodes across relations

In general, the Prisma API of a service is generated based on its [data model](!alias-eiroozae8u). To explore the operations in your Prisma API, you can use a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

In the following, we will explore example queries based on a Prisma service with this data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  published: Boolean!
  author: User!
}

type User {
  id: ID! @unique
  age: Int
  email: String! @unique
  name: String!
  posts: [Post!]!
}
```

## Object mutations

We can use **model mutations** to modify single nodes of a certain model.

### Creating nodes

Here, we use the `createUser` mutation to create a new user:

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

> **Note**: All required fields without a [default value](!alias-eiroozae8u#default-value) need to be specified in the `data` input object.

### Updating nodes

We can use `updateUser` to change the `email` and `name`. Note that we're [selecting the node](!alias-utee3eiquo#node-selection) to update using the `where` argument:

```graphql
mutation {
  updateUser(
    data: {
      email: "zeus2@example.com"
      name: "Zeus2"
    }
    where: {
      email: "zeus@example.com"
    }
  ) {
    id
    name
  }
}
```

### Upserting nodes

When we want to either update an existing node, or create a new one in a single mutation, we can use _upsert_ mutations.

Here, we use `upsertUser` to update the `User` with a certain `email`, or create a new `User` if a `User` with that `email` doesn't exist yet:

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

> **Note**: `create` and `update` are of the same type as the `data` object in the `createUser` and `updateUser` mutations.

### Deleting nodes

To delete nodes, all we have to do is to use the [select the node(s)](!alias-utee3eiquo#node-selection) to be deleted in a `delete` mutation.

Here, we use `deleteUser` to delete a user by its `id`:

```graphql
mutation {
  deleteUser(where: {
    id: "cjcdi63l20adx0146vg20j1ck"
  }) {
    id
    name
    email
  }
}
```

Because `email` is also annotated with the [`@unique`](!alias-eiroozae8u#unique) directive, we can also selected (and thus delete) `User` nodes by their `email`:

```graphql
mutation {
  deleteUser(where: {
    email: "cjcdi63l20adx0146vg20j1ck"
  }) {
    id
    name
    email
  }
}
```

## Nested mutations

We can use create and update model mutations to modify nodes across relations at the same time. This is referred to as **nested mutations** and is executed [transactionally](!alias-utee3eiquo#transactional-mutations).

### Overview

Several nested mutation arguments exist:

* `create`
* `update`
* `upsert`
* `delete`
* `connect`
* `disconnect`

Their availability and the exact behaviour depends on the following two parameters:

* the type of the parent mutation
  * create mutation
  * update mutation
  * upsert mutation
* the type of the relation
  * optional _to-one_ relation
  * required _to-one_
  * _to-many_ relation

For example

* a create mutation only exposes nested `create` and `connect` mutations
* an update mutation exposes `update`, `upsert` mutations for a required `to-one` relation

### Examples

Rather than mapping out all possible scenarios at this point, we provide a list of examples.

It's recommended to explore the behaviour of different nested mutations by using the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

#### Creating and connecting related nodes

We can use the `connect` action within a nested input object field to `connect` to one or more related nodes.

Here, we are creating a new `Post` and `connect` to an existing `author` via the unique `email` field. In this case, `connect` provides a way for [node selection](!alias-utee3eiquo#node-selection):

```graphql
# Create a post and connect it to an author
mutation {
  createPost(data: {
    title: "This is a draft"
    published: false
    author: {
      connect: {
        email: "zeus@example.com"
      }
    }
  }) {
    id
    author {
      name
    }
  }
}
```

If we provide a `create` argument instead of `connect` within `author`, we would _create_ a related `author` and at the same time `connect` to it, instead of connecting to an existing `author`.

When creating a `User` instead of a `Post`, we can actually `create` and `connect` to multiple `Post` nodes at the same time, because `User` has a _to-many_ relation `Post`.

Here, we are creating a new `User` and directly connect it to several new and existing `Post` nodes:

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
          published: true
          title: "First blog post"
        }, {
          published: true
          title: "Second blog post"
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

#### Updating and upserting related nodes

When updating nodes, you can update one or more related nodes at the same time.

```graphql
mutation {
  updateUser(
    data: {
      posts: {
        update: [{
          where: {
            id: "cjcf1cj0r017z014605713ym0"
          }
          data: {
            title: "Hello World"
          }
        }]
      }
    }
    where: {
      id: "cjcf1cj0c017y01461c6enbfe"
    }
  ) {
    id
  }
}
```

Note that `update` accepts a list of objects with `where` and `data` fields suitable for the `updatePost` mutation.

Nested upserting works similarly:

```graphql
mutation {
  updatePost(
    where: {
      id: "cjcf1cj0r017z014605713ym0"
    }
    data: {
      author: {
        upsert: {
          where: {
            id: "cjcf1cj0c017y01461c6enbfe"
          }
          update: {
            email: "zeus2@example.com"
            name: "Zeus2"
          }
          create: {
            email: "zeus@example.com"
            name: "Zeus"
          }
        }
      }
    }
  ) {
    id
  }
}
```

#### Deleting related nodes

When updating nodes, you can delete one or more related nodes at the same time. In this case, `delete` provides a way [node selection](!alias-utee3eiquo#node-selection):

```graphql
mutation {
  updateUser(
    data: {
      posts: {
        delete: [{
          id: "cjcf1cj0u01800146jii8h8ch"
        }, {
          id: "cjcf1cj0u01810146m84cnt34"
        }]
      }
    }
    where: {
      id: "cjcf1cj0c017y01461c6enbfe"
    }
  ) {
    id
  }
}
```

## Scalar list mutations

When an [object type](!alias-eiroozae8u#object-types) has a field that is has a _scalar list_ as its type, there are a number of special mutations available.

In the following data model, the `User` type has three such fields:

```graphql
type User {
  id: ID! @unique
  scores: [Int!]!         # scalar list for integers
  friends: [String!]!     # scalar list for strings
  coinFlips: [Boolean!]!  # scalar list for booleans
}
```

### Creating nodes

When creating a new node of type `User`, a list of values can be provided for each scalar list field using `set`.

#### Example

```grahpql
mutation {
  createUser(data: {
    scores: { set: [1, 2, 3] }
    friends: { set: ["Sarah", "Jane"] }
    throws: { set: [false, false] }
  }) {
    id
  }
}
```

### Updating nodes

When updating an existing node of type `User`, a number of additional operations can be performed on the scalar list fields:

- `set`: Override the existing list with an entirely new list.
- `push` (coming soon): Add one or more elements anywhere in the list.
- `pop` (coming soon): Remove one or more elements from the beginning or the end of the list.
- `remove` (coming soon): Remove all elements from the list that match a given filter.

> **Note**: `push`, `pop` and `remove` are not yet implemented. If you're curios what these are going to look like, you can get a preview in the respective [specification](https://github.com/graphcool/prisma/issues/1275).

#### `set`

Each scalar list field takes an object with a `set` field in an `update`-mutation. The value of that field is a single value _or_ a list of the corresponding scalar type.

##### Examples

Set the `scores` of an existing `User` node to `[1]`:

```graphql
mutation {
  updateUser(
    where: {
      id: "cjd4lfdyww0h00144zst9alur"
    }
    data: {
      scores: {
        set: 1
      }
    }
  ) {
    id
  }
}
```

Set the `scores` of an existing `User` node to `[10,20,30]`:

```graphql
mutation {
  updateUser(
    where: {
      id: "cjd4lfdyww0h00144zst9alur"
    }
    data: {
      scores: {
        set: [10,20,30]
      }
    }
  ) {
    id
  }
}
```

## Batch Mutations

Batch mutations are useful to update or delete many nodes at once. The returned data only contains the `count` of affected nodes.

For updating many nodes, you can [select the affected nodes](!alias-utee3eiquo#node-selection) using the `where` argument, while you specify the new values with `data`. All nodes will be updated to the same value.

<InfoBox type="warning">

Note that no [subscription](!alias-aey0vohche) events are triggered for batch mutations!

</InfoBox>

Here, we are publishing all unpublished `Post` nodes that were created in 2017:

```graphql
mutation {
  updateManyPosts(
    where: {
      createdAt_gte: "2017"
      createdAt_lt: "2018"
      published: false
    }
    data: {
      published: true
    }
  ) {
    count
  }
}
```

Here, we are deleting all unpublished `Post` nodes of a certain `author`:

```graphql
mutation {
  deleteManyPosts(
    where: {
      published: false
      author: {
        name: "Zeus"
      }
    }
  ) {
    count
  }
}
```
