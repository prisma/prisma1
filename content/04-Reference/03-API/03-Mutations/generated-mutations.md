---
alias: ol0yuoz6go
path: /docs/reference/simple-api/mutations
layout: REFERENCE
description: A GraphQL mutation is used to modify data at a GraphQL endpoint.
simple_relay_twin: vah0igucil
tags:
  - simple-api
  - mutations
related:
  further:
  more:
---

# Mutations

A *GraphQL mutation* is used to modify data at a GraphQL [endpoint](!alias-yahph3foch#project-endpoints). This is an example mutation:

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

Here's a list of available mutations. To explore them, use the [playground](!alias-oe1ier4iej) inside your project.

* Based on the [types](!alias-ij2choozae) and [relations](!alias-goh5uthoc1) in your [GraphQL schema](!alias-ahwoh2fohj), [type mutations](!alias-eamaiva5yu) and [relation mutations](!alias-kaozu8co6w) will be generated to modify nodes and edges.
* Additionally, [custom mutations](!alias-thiele0aop) can be added to your API using [Schema Extensions](!alias-xohbu7uf2e).
