---
alias: ubohch8quo
description: Create or connect multiple nodes across relations all in a single mutation.
---

# Nested Mutations

When creating or updating nodes, you can execute _nested mutations_ to interact with connected parts of your type schema.

* to **create and connect to a new node** on the other side of a relation, you can use [nested create mutations](!alias-vaet3eengo).
* to **connect to an existing node** on the other side of a relation, you can use [nested connect mutations](!alias-tu9ohwa1ui).

## Limitations

Different limitations and improvement suggestions are available. Please join the discussion on GitHub!

* [Nested delete mutations](https://github.com/graphcool/feature-requests/issues/42) are not available yet. Neither are [cascading deletes](https://github.com/graphcool/feature-requests/issues/47).
* Currently, the [maximum nested level is 3](https://github.com/graphcool/feature-requests/issues/313). If you want to nest more often than that, you need to split up the nested mutations into two separate mutations.

Many other [suggestions and improvements](https://github.com/graphcool/feature-requests/issues/127) are currently being discussed.


## Nested Create Mutations

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

### Nested Create Mutations for to-one Relations

Let's explore the available nested create mutations for the `one-to-one` relation `AuthorContactDetails`.

##### Creating a new Author node and connect it to new ContactDetails

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

##### Updating an existing Author and connect it to new ContactDetails

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

### Nested Create Mutations for to-many Relations

Let's explore the available nested create mutations for the `one-to-many` relation `AuthorPosts`.

##### Creating a new Author node and connect it to multiple new Posts

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

##### Updating an existing Author and Connecting it to multiple new Posts

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



## Nested Connect Mutations

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

### Nested Connect Mutations for to-one Relations

Let's explore the available nested connect mutations for the `one-to-one` relation `AuthorContactDetails`.

##### Creating a new Author node and connect it to existing ContactDetails

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

##### Updating an existing Author and connect it to existing ContactDetails

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

### Nested Connect Mutations for to-many Relations

Let's explore the available nested connect mutations for the `one-to-many` relation `AuthorPosts`.

##### Creating a new Author node and connect it to multiple existing Posts

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

##### Updating an existing Author and Connecting it to multiple new Posts

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

