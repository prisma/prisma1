---
alias: fasie2rahv
path: /docs/reference/simple-api/deleting-nodes
layout: REFERENCE
shorttitle: Deleting nodes
description: Deletes a node in your GraphQL backend. The query response can contain all fields of the deleted node.
simple_relay_twin: oojie3ooje
tags:
  - simple-api
  - mutations
related:
  further:
    - wooghee1za
    - cahkav7nei
    - koo4eevun4
  more:
    - cahzai2eur
    - dah6aifoce
    - iet3phoum8
---

# Deleting nodes with the Simple API

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
