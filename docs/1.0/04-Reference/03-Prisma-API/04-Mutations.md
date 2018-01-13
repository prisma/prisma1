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

## Introduction

For the following example mutations, consider a Prisma service with this data model:

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

### Creating nodes

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

> Note: all required fields without a default value need to be specified in the `data` input object.

### Updating nodes

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

> Note: all required fields without a default value need to be specified in the `data` input object.


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

## Batch Mutations

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

## Relation Mutations

```graphql
# Create a post and connect it to an existing author via unique email
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
