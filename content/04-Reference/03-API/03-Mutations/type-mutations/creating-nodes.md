---
alias: wooghee1za
path: /docs/reference/simple-api/creating-nodes
layout: REFERENCE
shorttitle: Creating nodes
description: Creates a new node for a specific type in your GraphQL backend. The node gets assigned a unique node id on creation.
simple_relay_twin: oodoi6zeit
tags:
  - simple-api
  - mutations
related:
  further:
    - fasie2rahv
    - cahkav7nei
    - koo4eevun4
    - geekae9gah
  more:
    - cahzai2eur
    - dah6aifoce
---

# Creating a node with the Simple API

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
