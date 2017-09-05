---
alias: cahkav7nei
path: /docs/reference/simple-api/updating-nodes
layout: REFERENCE
shorttitle: Updating nodes
description: Updates fields of an existing node in your GraphQL backend. The node fields will be updated according to the provided values.
simple_relay_twin: uath8aifo6
tags:
  - simple-api
  - mutations
  - nodes
related:
  further:
    - wooghee1za
    - fasie2rahv
    - koo4eevun4
  more:
    - cahzai2eur
    - dah6aifoce
---

# Updating nodes with the Simple API

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
