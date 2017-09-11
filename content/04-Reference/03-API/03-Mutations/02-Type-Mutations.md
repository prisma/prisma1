---
alias: eamaiva5yu
description: For every available type in your GraphQL schema, certain mutations are automatically generated.
---

# Type Mutations

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


## Creating a node 

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


## Updating nodes with the Simple API

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


## Deleting nodes

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
