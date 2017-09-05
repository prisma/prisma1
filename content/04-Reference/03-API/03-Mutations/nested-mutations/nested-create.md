---
alias: vaet3eengo
path: /docs/reference/simple-api/nested-create-mutations
layout: REFERENCE
shorttitle: Nested Create Mutations
description: Create multiple nodes across relations all in a single mutation.
simple_relay_twin: ma6eijae7a
tags:
  - simple-api
  - mutations
  - create
related:
  further:
    - wooghee1za
  more:
    - cahzai2eur
    - dah6aifoce
---

# Nested Create Mutations

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

## Nested Create Mutations for to-one Relations

Let's explore the available nested create mutations for the `one-to-one` relation `AuthorContactDetails`.

#### Creating a new Author node and connect it to new ContactDetails

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

#### Updating an existing Author and connect it to new ContactDetails

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

## Nested Create Mutations for to-many Relations

Let's explore the available nested create mutations for the `one-to-many` relation `AuthorPosts`.

#### Creating a new Author node and connect it to multiple new Posts

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

#### Updating an existing Author and Connecting it to multiple new Posts

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
