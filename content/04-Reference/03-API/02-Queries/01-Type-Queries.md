---
alias: nia9nushae
description: A GraphQL query is used to fetch data from a GraphQL endpoint.
---

# Overview

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
* Additionally, [custom queries](!alias-nae4oth9ka) can be added to your API using [Resolvers](!alias-xohbu7uf2e) that are implemented as serverless functions.

Some queries support [query arguments](!alias-on1yeiw7ph) to further control the query response.



## Fetching a single node

For each [type](!alias-ij2choozae) in your project, the Simple API provides an automatically generated query to fetch one specific node of that type. To specify the node, all you need to provide is its `id` or another unique field.

For example, for the type called `Post` a top-level query `Post` will be generated.

### Specifying the node by id

You can always use the [system field](!alias-uhieg2shio#id-field) `id` to identify a node.

> Query a specific post by id:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  Post(id: "cixnen24p33lo0143bexvr52n") {
    id
    title
    published
  }
}
---
{
  "data": {
    "Post": {
      "id": "cixnen24p33lo0143bexvr52n",
      "title": "My biggest Adventure",
      "published": false
    }
  }
}
```

### Specifying the node by another unique field

You can also supply any [unique field](!alias-teizeit5se#unique) as an argument to the query to identify a node. For example, if you already declared the `slug` field of the `Post` type to be unique, you could select a post by specifying its slug:

> Query a specific node by slug:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  Post(slug: "my-biggest-adventure") {
    id
    slug
    title
    published
  }
}
---
{
  "data": {
    "Post": {
      "id": "cixnen24p33lo0143bexvr52n",
      "slug": "my-biggest-adventure",
      "title": "My biggest Adventure",
      "published": false
    }
  }
}
```

Note: You cannot specify two or more unique arguments for one query at the same time.



## Fetch multiple nodes

The Simple API contains automatically generated queries to fetch all nodes of a certain [type](!alias-ij2choozae). For example, for the `Post` type the top-level query `allPosts` will be generated.

### Fetch all nodes of a specific type

> Query all post nodes:

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

> A few examples for query names
* type name: `Post`, query name: `allPosts`
* type name: `Todo`, query name: `allTodoes`
* type name: `Hobby`, query name: `allHobbies`.

Note: The query name approximate the plural rules of the English language. If you are unsure about the actual query name, explore available queries in your [playground](!alias-oe1ier4iej).

### Fetch certain nodes of a specific type

> Query all post nodes with a `title` that contains `biggest`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(filter: {
    title_contains: "biggest"
  }) {
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
      }
    ]
  }
}
```


## Type Aggregation Queries

For every type in your GraphQL schema, different aggregation queries are available.

### Fetch the number of all nodes

> Count the number of all `User` nodes:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  _allUsersMeta {
    count
  }
}
---
{
  "data": {
    "_allUsersMeta": {
      "count": 3
    }
  }
}
```

### Count the number of nodes matching a certain filter condition

> Count the number of all `User` nodes with `accessRole` `ADMIN`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  _allUsersMeta(filter: {
    accessRole: ADMIN
  }) {
    count
  }
}
---
{
  "data": {
    "_allUsersMeta": {
      "count": 1
    }
  }
}
```

### More Aggregation Options

Currently, count is the only available aggregation. For specific use cases, you can use [functions](!alias-boo6uteemo) to precalculate certain aggregations and update them when data changes.

Please join the discussion on [GitHub](https://github.com/graphcool/feature-requests/issues/70) if you are interested in a specific aggregation.
