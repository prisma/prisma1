---
alias: nia9nushae
description: A GraphQL query is used to fetch data from a GraphQL endpoint.
---

# Queries in the Simple API

A *GraphQL query* is used to fetch data from a GraphQL [endpoint](!alias-yahph3foch#project-endpoints). This is an example query:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts {
    id
    title
    published
  }
}
---
{
  "data": {
    "allPosts": [
      {
        "id": "cixnen24p33lo0143bexvr52n",
        "title": "My biggest Adventure",
        "published": false
      },
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies",
        "published": true
      },
      {
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation",
        "published": true
      }
    ]
  }
}
```

Here's a list of available queries. To explore them, use the [playground](!alias-oe1ier4iej) inside your project.

* Based on the [types](!alias-ij2choozae) and [relations](!alias-goh5uthoc1) in your [GraphQL schema](!alias-ahwoh2fohj), [type queries](!alias-chuilei3ce) and [relation queries](!alias-aihaeph5ip) will be generated to fetch type and relation data.
* Additionally, [custom queries](!alias-nae4oth9ka) can be added to your API using [Schema Extensions](!alias-xohbu7uf2e).

Some queries support [query arguments](!alias-on1yeiw7ph) to further control the query response.
