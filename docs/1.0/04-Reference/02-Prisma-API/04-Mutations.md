---
alias: ol0yuoz6go
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
---

# Mutations

The Prisma API offers

* **Simple Mutations**, to create, update, upsert and delete single nodes of a certain model
* **Batch Mutations**, to update and delete many nodes of a certain model
* **Relation Mutations**, to connect, disconnect, create, update and upsert nodes across relations

In general, the Prisma API of a service is structured according to [its data model](!alias-eiroozae8u). To explore it, use the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

In the following, we will explore example queries based on a Prisma service with this data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  isPublished: Boolean!
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

## Model Mutations

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

> Note: all required fields without a [default value](!alias-eiroozae8u#default-value) need to be specified in the `data` input object.

### Updating nodes

We can use `updateUser` to change the email and name. Note that we're [selecting the node](!alias-utee3eiquo#node-selection) to update using the `where` argument:

```graphql
# Update an existing user
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

When we want to either update an existing node, or create a new one in one mutation, we can use upsert mutations.

Here, we use `upsertUser` to update the user with a certain email, or create a new user if that email address doesn't exist yet:

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

Note that `create` and `update` are of the same type as the `data` object in the `createUser` and `updateUser` mutations.

### Deleting nodes

To delete nodes, all we have to do is to [use the `where` selection](!alias-utee3eiquo#node-selection) in a delete mutation.

Here, we use `deleteUser` to delete a user by `id`:

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

## Nested Mutations

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
  * optional `to-one` relation
  * required `to-one`
  * `to-many` relation

For example

* a create mutation only exposes nested `create` and `connect` mutations
* an update mutation exposes `update`, `upsert` mutations for a required `to-one` relation

### Examples

Rather than mapping out all possible scenarios, here's a list of examples.
It's recommended to explore the behaviour of different nested mutations by using the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

#### Creating and connecting related nodes

We can use the `connect` action within a nested input object field to connect to one or more related nodes.

Here, we are creating a new post and connect to an existing author via unique email. In this case, `connect` acts as [node selection](!alias-utee3eiquo#node-selection):

```graphql
# Create a post and connect it to an author
mutation {
  createPost(data: {
    title: "This is a draft"
    isPublished: false
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

If we provide a `create` argument instead of `connect` within `author`, we would _create_ a related author and connect to it, instead of connecting to an existing author.

When creating a user instead of a post, we can actually create and connect to multiple posts at the same time, because users are related to many posts.

Here, we are creating a user and connecting it to multiple new and existing posts:

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
          title: "First blog post"
        }, {
          isPublished: true
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

When updating nodes, you can delete one or more related nodes at the same time. In this case, `delete` acts as [node selection](!alias-utee3eiquo#node-selection):

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

## Batch Mutations

Batch mutations are useful to update or delete many nodes at once. The returned data only contains the `count` of affected nodes.

For updating many nodes, you can [select the affected nodes](!alias-utee3eiquo#node-selection) using the `where` argument, while you specify the new values with `data`. All nodes will be updated to the same value.

Here, we are publishing all unpublished posts that were created in 2017:

```graphql
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

Here, we are deleting all unpublished posts of a certain author:

```graphql
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
