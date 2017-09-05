---
alias: tu9ohwa1ui
path: /docs/reference/simple-api/nested-connect-mutations
layout: REFERENCE
shorttitle: Nested Connect Mutations
description: Connect multiple nodes across relations all in a single mutation.
simple_relay_twin: ec6aegaiso
tags:
  - simple-api
  - mutations
  - update
related:
  further:
    - cahkav7nei
  more:
    - cahzai2eur
    - dah6aifoce
---

# Nested Connect Mutations

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

## Nested Connect Mutations for to-one Relations

Let's explore the available nested connect mutations for the `one-to-one` relation `AuthorContactDetails`.

#### Creating a new Author node and connect it to existing ContactDetails

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

#### Updating an existing Author and connect it to existing ContactDetails

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

## Nested Connect Mutations for to-many Relations

Let's explore the available nested connect mutations for the `one-to-many` relation `AuthorPosts`.

#### Creating a new Author node and connect it to multiple existing Posts

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

#### Updating an existing Author and Connecting it to multiple new Posts

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
