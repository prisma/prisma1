---
alias: nia9nushae
description: A GraphQL query is used to fetch data from a GraphQL endpoint.
---

# Query API

## Overview

A *GraphQL query* is used to fetch data from a GraphQL endpoint. This is an example query:

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

> You can click the **Play**-button in the code snippet to run the query and see the server response. You can also modify the query and observe the changes in the responses that are sent by the server.

Here's a list of available queries. To explore them, use the [playground](!alias-aiteerae6l#graphcool-playground) for your service.

- Based on the [model types](!alias-eiroozae8u#model-types) and [relations](!alias-eiroozae8u#relations) in your data model, [type queries](#type-queries) and [relation queries](#relation-queries) will be generated to fetch type and relation data.
- Additionally, [custom queries and mutations](#custom-queries) can be added to your API using [resolver](!alias-su6wu3yoo2) functions.

Some queries support [query arguments](#query-arguments) to further control the query response.

## Type queries

### Fetching a single node

For each [model type](!alias-eiroozae8u#model-types) in your service, the `Simple API` provides an auto-generated query to fetch one specific node of that type. To specify the node, all you need to provide is its `id` or another unique field.

For example, for a type called `Post` a top-level query `Post` will be generated.

#### Specifying the node by id

You can always use the system-managed [`id`](!alias-eiroozae8u#required-system-field-id) field to identify a node.

Query a specific post by its `id`:

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

#### Specifying the node by another unique field

You can also supply any [unique field](!alias-eiroozae8u#unique) as an argument to the query to identify a node. For example, if you already declared the `slug` field of the `Post` type to be unique, you could select a post by specifying its `slug`:

Query a specific `Post` node by its `slug`:

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

> Note: You cannot specify two or more unique arguments for one query at the same time.


### Fetch multiple nodes

The `Simple API` contains automatically generated queries to fetch all nodes of a certain [model type](!alias-eiroozae8u#model-types). For example, for the `Post` type the top-level query `allPosts` will be generated.

#### Fetch all nodes of a specific type

Query all `Post` nodes:

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

Here are a few examples for the generated query names:

- type name: `Post`, query name: `allPosts`
- type name: `Todo`, query name: `allTodoes`
- type name: `Hobby`, query name: `allHobbies`.

> Note: The query name approximate the plural rules of the English language. If you are unsure about the actual query name, explore available queries in your [playground](!alias-aiteerae6l#graphcool-playground).

#### Fetch certain nodes of a specific type

Query all `Post` nodes with a `title` that contains the string `biggest`:

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


### Type aggregation queries

For every type in your GraphQL schema, different aggregation queries are available.

#### Fetch the number of all nodes

Count the number of all `User` nodes:

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

#### Count the number of nodes matching a certain filter condition

Count the number of all `User` nodes with `accessRole` `ADMIN`:

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

#### More aggregation options

Currently, `count` is the only available aggregation. For specific use cases, you can use [functions](!alias-aiw4aimie9) to precalculate certain aggregations and update them when data changes.

Please join the discussion on [GitHub](https://github.com/graphcool/feature-requests/issues/70) if you are interested in a specific aggregation.


## Relation queries

Every available [relation](!alias-eiroozae8u#relations) in your type definitions adds a new field to the [type queries](#type-queries) of the two connected types.

For example, with the following schema:

```graphql
type Post @model {
  id: ID! @isUnique
  title: String!
  author: User @relation(name: "UserOnPost")
}

type User @model {
  id: ID! @isUnique
  name : String!
  posts: [Post!]! @relation(name: "UserOnPost")
}
```

the following fields will be available:

- the `Post` and `allPosts` queries expose a new `author` field to [traverse one node](#traversing-a-single-node).
- the `User` and `allUsers` queries expose a new `posts` field to [traverse many nodes](#traversing-many-nodes) and a `_postsMeta` to fetch [relation aggregation data](#relation-aggregation).


### Traversing a single node

Traversing edges that connect the current node to the one side of a relation can be done by simply selecting the according field defined with the relation.

Query information on the `author` node connected to a specific `Post` node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  Post(id: "cixnen24p33lo0143bexvr52n") {
    id
    author {
      id
      name
      email
    }
  }
}
---
{
  "data": {
    "Post": {
      "id": "cixnen24p33lo0143bexvr52n",
      "author": {
        "id": "cixnekqnu2ify0134ekw4pox8",
        "name": "John Doe",
        "email": "john.doe@example.com"
      }
    }
  }
}
```

The `author` field exposes a further selection of properties that are defined on the `Author` type.

> Note: You can add [filter query arguments](#filtering-by-field) to an inner field returning a single node.


### Traversing many nodes

In the Simple API, traversing edges connecting the current node to the many side of a relation works the same as for a one side of a relation. Simply select the relation field.

Query information on all `Post` nodes of a certain `author` node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  User(id: "cixnekqnu2ify0134ekw4pox8") {
    id
    name
    posts {
      id
      published
    }
  }
}
---
{
  "data": {
    "User": {
      "id": "cixnekqnu2ify0134ekw4pox8",
      "name": "John Doe",
      "posts": [
        {
          "id": "cixnen24p33lo0143bexvr52n",
          "published": false
        },
        {
          "id": "cixnenqen38mb0134o0jp1svy",
          "published": true
        },
        {
          "id": "cixneo7zp3cda0134h7t4klep",
          "published": true
        }
      ]
    }
  }
}
```

The `posts` field exposes a further selection of properties that are defined on the `Post` type.

> Note: [Query arguments](#query-arguments) for an inner field returning multiple nodes work similar as elsewhere.


### Relation aggregation

Nodes connected to multiple nodes via a one-to-many or many-to-many edge expose the `_<edge>Meta` field that can be used to query meta information of a connection rather than the actual connected nodes.

Query meta information on all `Post` nodes of a certain `author` node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  User(id: "cixnekqnu2ify0134ekw4pox8") {
    id
    name
    _postsMeta {
      count
    }
  }
}
---
{
  "data": {
    "User": {
      "id": "cixnekqnu2ify0134ekw4pox8",
      "name": "John Doe",
      "_postsMeta": {
        "count": 3
      }
    }
  }
}
```

Query meta information on certain `Post` nodes of a certain `author` node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  User(id: "cixnekqnu2ify0134ekw4pox8") {
    id
    name
    _postsMeta(filter: {
      title_contains: "adventure"
    }) {
      count
    }
  }
}
---
{
  "data": {
    "User": {
      "id": "cixnekqnu2ify0134ekw4pox8",
      "name": "John Doe",
      "_postsMeta": {
        "count": 1
      }
    }
  }
}
```


## Query arguments

All queries and fields that return many nodes accept different query arguments to further control the query response. The response can be either of the following:

- [ordered by field](#ordering-by-field)
- [filtered by multiple fields](#filtering-by-field)
- [paginated](#pagination) into multiple pages by fixing one specific node and either seeking forwards or backwards

These query arguments can be combined to achieve very specific query responses.


### Ordering by field

When querying all nodes of a [model type](!alias-eiroozae8u#model-types) you can supply the `orderBy` argument for every scalar field of the type: `orderBy: <field>_ASC` or `orderBy: <field>_DESC`.

Order the list of all `Post` nodes ascending by `title`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(orderBy: title_ASC) {
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
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation",
        "published": true
      },
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies",
        "published": true
      }
    ]
  }
}
```

Order the list of all `Post` nodes descending by `published`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(orderBy: published_DESC) {
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
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies",
        "published": true
      },
      {
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation",
        "published": true
      },
      {
        "id": "cixnen24p33lo0143bexvr52n",
        "title": "My biggest Adventure",
        "published": false
      }
    ]
  }
}
```

> Note: The field you are ordering by does not have to be selected in the actual query. If you do not specify an ordering, the response is implicitely ordered ascending by the `id` field


#### Limitations

It's currently not possible to order responses [by multiple fields](https://github.com/graphcool/feature-requests/issues/62) or [by related fields](https://github.com/graphcool/feature-requests/issues/95). Join the discussion in the feature requests if you're interested in these features!


### Filtering by field

When querying all nodes of a type you can supply different parameters to the `filter` argument to filter the query response accordingly. The available options depend on the scalar fields defined on the type in question.

You can also include filters when including related fields in your queries to [traverse your data graph](#relation-queries).

#### Applying single filters

If you supply exactly one parameter to the `filter` argument, the query response will only contain nodes that adhere to this constraint.

#### Filtering by value

The easiest way to filter a query response is by supplying a field value to filter by.

Query all `Post` nodes that are not yet `published`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(filter: {
    published: false
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

##### Advanced filter criteria

Depending on the type of the field you want to filter by, you have access to different advanced criteria you can use to filter your query response. See how to [explore available filter criteria](#explore-available-filter-criteria).

Query all `Post` nodes whose `title` is in a given list of strings:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(filter: {
    title_in: ["My biggest Adventure", "My latest Hobbies"]
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
      },
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies",
        "published": true
      }
    ]
  }
}
```

> Note: you have to supply a *list* as the `<field>_in` argument: `title_in: ["My biggest Adventure", "My latest Hobbies"]`.

#### Relation filters

For to-one relations, you can define conditions on the related node by nesting the according argument in `filter`

Query all `Post` nodes where the `author` has the `USER` access role:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(filter: {
    author: {
      accessRole: USER
    }
  }) {
    title
  }
}
---
{
  "data": {
    "allPosts": [
      {
        "title": "My biggest Adventure"
      },
      {
        "title": "My latest Hobbies"
      },
      {
        "title": "My great Vacation"
      }
    ]
  }
}
```


For to-many relations, three additional arguments are available: `every`, `some` and `none`, to define that a condition should match every, some or none related nodes.

Query all `User` nodes that have at least one `Post` node that's `published`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allUsers(filter: {
    posts_some: {
      published: true
    }
  }) {
    id
    posts {
      published
    }
  }
}
---
{
  "data": {
    "allUsers": [
      {
        "id": "cixnekqnu2ify0134ekw4pox8",
        "posts": [
          {
            "published": false
          },
          {
            "published": true
          },
          {
            "published": true
          }
        ]
      }
    ]
  }
}
```

Relation filters are also available in the nested arguments for to-one or to-many relations.

Query all `User` nodes that did not _like_ a `Post` of an `author` in the `ADMIN` access role:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allUsers(filter: {
    likedPosts_none: {
      author: {
        accessRole: ADMIN
      }
    }
  }) {
    name
  }
}
---
{
  "data": {
    "allUsers": [
      {
        "name": "John Doe"
      },
      {
        "name": "Sally Housecoat"
      },
      {
        "name": "Admin"
      }
    ]
  }
}
```

#### Combining multiple filters

You can use the filter combinators `OR` and `AND` to create an arbitrary logical combination of filter conditions.

##### Using `OR` or `AND`

Let's start with an easy example:

Query all `Post` nodes that are `published` _and_ whose `title` is in a given list of strings:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query {
  allPosts(filter: {
    AND: [{
      title_in: ["My biggest Adventure", "My latest Hobbies"]
    }, {
      published: true
    }]
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
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies",
        "published": true
      }
    ]
  }
}
```

> Note: `OR` and `AND` accept a *list* as input where individual list items have to be wrapped by `{}`: `AND: [{title_in: ["My biggest Adventure", "My latest Hobbies"]}, {published: true}]`

##### Arbitrary combination of filters with `AND` and `OR`

You can combine and even nest the filter combinators `AND` and `OR` to create arbitrary logical combinations of filter conditions.

Query all `Post` nodes that are either `published` _and_ whose `title` is in a list of given strings, _or_ have the specific `id` we supply:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: false
---
query($published: Boolean) {
  allPosts(filter: {
    OR: [{
      AND: [{
        title_in: ["My biggest Adventure", "My latest Hobbies"]
      }, {
        published: $published
      }]
    }, {
      id: "cixnen24p33lo0143bexvr52n"
    }]
  }) {
    id
    title
    published
  }
}
---
{
  "published": true
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
      }
    ]
  }
}
```

> Notice how we nested the `AND` combinator inside the `OR` combinator, on the same level with the `id` value filter.

#### Explore available filter criteria

Apart from the filter combinators `AND` and `OR`, the available filter arguments for a query for all nodes of a type depend on the fields of the type and their types.

Let's consider the following schema:

```graphql
type Meta {
  id: ID! @isUnique
  text: String!
  number: Int!
  decimal: Float!
  flag: Boolean!
  date: DateTime!
  enum: SomeEnum!
  object: Json!
}
```

Based on this type, a `MetaFilter` type will be generated with the following fields, grouped by field type.

```graphql
input MetaFilter {
  # logical operators
  AND: [MetaFilter!] # combines all passed `MetaFilter` objects with logical AND
  OR: [MetaFilter!] # combines all passed `MetaFilter` objects with logical OR

  # DateTime filters
  createdAt: DateTime # matches all nodes with exact value
  createdAt_not: DateTime # matches all nodes with different value
  createdAt_in: [DateTime!] # matches all nodes with value in the passed list
  createdAt_not_in: [DateTime!] # matches all nodes with value not in the passed list
  createdAt_lt: DateTime # matches all nodes with lesser value
  createdAt_lte: DateTime # matches all nodes with lesser or equal value
  createdAt_gt: DateTime # matches all nodes with greater value
  createdAt_gte: DateTime # matches all nodes with greater or equal value

  # Float filters
  decimal: Float # matches all nodes with exact value
  decimal_not: Float # matches all nodes with different value
  decimal_in: [Float!] # matches all nodes with value in the passed list
  decimal_not_in: [Float!] # matches all nodes with value not in the passed list
  decimal_lt: Float # matches all nodes with lesser value
  decimal_lte: Float # matches all nodes with lesser or equal value
  decimal_gt: Float # matches all nodes with greater value
  decimal_gte: Float # matches all nodes with greater or equal value

  # Enum filters
  enum: META_ENUM # matches all nodes with exact value
  enum_not: META_ENUM # matches all nodes with different value
  enum_in: [META_ENUM!] # matches all nodes with value in the passed list
  enum_not_in: [META_ENUM!] # matches all nodes with value not in the passed list

  # Boolean filters
  flag: Boolean # matches all nodes with exact value
  flag_not: Boolean # matches all nodes with different value

  # ID filters
  id: ID # matches all nodes with exact value
  id_not: ID # matches all nodes with different value
  id_in: [ID!] # matches all nodes with value in the passed list
  id_not_in: [ID!] # matches all nodes with value not in the passed list
  id_lt: ID # matches all nodes with lesser value
  id_lte: ID # matches all nodes with lesser or equal value
  id_gt: ID # matches all nodes with greater value
  id_gte: ID # matches all nodes with greater or equal value
  id_contains: ID # matches all nodes with a value that contains given substring
  id_not_contains: ID # matches all nodes with a value that does not contain given substring
  id_starts_with: ID # matches all nodes with a value that starts with given substring
  id_not_starts_with: ID # matches all nodes with a value that does not start with given substring
  id_ends_with: ID # matches all nodes with a value that ends with given substring
  id_not_ends_with: ID # matches all nodes with a value that does not end with given substring

  # Int filters
  number: Int # matches all nodes with exact value
  number_not: Int # matches all nodes with different value
  number_in: [Int!] # matches all nodes with value in the passed list
  number_not_in: [Int!] # matches all nodes with value not in the passed list
  number_lt: Int # matches all nodes with lesser value
  number_lte: Int # matches all nodes with lesser or equal value
  number_gt: Int # matches all nodes with greater value
  number_gte: Int # matches all nodes with greater or equal value

  # String filters
  text: String # matches all nodes with exact value
  text_not: String # matches all nodes with different value
  text_in: [String!] # matches all nodes with value in the passed list
  text_not_in: [String!] # matches all nodes with value not in the passed list
  text_lt: String # matches all nodes with lesser value
  text_lte: String # matches all nodes with lesser or equal value
  text_gt: String # matches all nodes with greater value
  text_gte: String # matches all nodes with greater or equal value
  text_contains: String # matches all nodes with a value that contains given substring
  text_not_contains: String # matches all nodes with a value that does not contain given substring
  text_starts_with: String # matches all nodes with a value that starts with given substring
  text_not_starts_with: String # matches all nodes with a value that does not start with given substring
  text_ends_with: String # matches all nodes with a value that ends with given substring
  text_not_ends_with: String # matches all nodes with a value that does not end with given substring
}
```

#### Limitations

Currently, neither [**scalar list filters**](https://github.com/graphcool/feature-requests/issues/60) nor [**JSON filters**](https://github.com/graphcool/feature-requests/issues/148) are available. Join the discussion in the respective feature requests on GitHub!

If you want to filter a list of strings `tags: [String!]!`:

```graphql
type Item @model {
  id: ID! @isUnique
  tags: [String!]!
}
```

you can introduce a new type `Tag` with a single `key: String` field and connect `Item` to `Key` one-to-many or many-to-many:

```graphql
type Item @model {
  id: ID! @isUnique
  tags: [Tag!]! @relation(name: "ItemTags")
}

type Tag {
  id: ID! @model @isUnique
  key: String!
  item: Item @relation(name: "ItemTags")
}
```

Now you can filter items based on their connected tags using the `tag_none`, `tags_some` and `tags_every` filters.



### Pagination

When querying all nodes of a specific [model type](!alias-eiroozae8u#model-types), you can supply arguments that allow you to _paginate_ the query response.

Pagination allows you to request a certain amount of nodes at the same time. You can seek forwards or backwards through the nodes and supply an optional starting node:

- to seek forwards, use `first`; specify a starting node with `after`.
- to seek backwards, use `last`; specify a starting node with `before`.

You can also skip an arbitrary amount of nodes in whichever direction you are seeking by supplying the `skip` argument.

Consider a blog where only 3 `Post` nodes are shown at the front page. To query the first page:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
query {
  allPosts(first: 3) {
    id
    title
  }
}
---
{
  "data": {
    "allPosts": [
      {
        "id": "cixnen24p33lo0143bexvr52n",
        "title": "My biggest Adventure"
      },
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies"
      },
      {
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation"
      }
    ]
  }
}
```

To query the first two `Post` node after the first `Post` node:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
query {
  allPosts(
    first: 2
    after: "cixnen24p33lo0143bexvr52n"
  ) {
    id
    title
  }
}
---
{
  "data": {
    "allPosts": [
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies"
      },
      {
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation"
      }
    ]
  }
}
```

We could reach the same result by combining `first` and `skip`:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
query {
  allPosts(
    first: 2
    skip: 1
  ) {
    id
    title
  }
}
---
{
  "data": {
    "allPosts": [
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies"
      },
      {
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation"
      }
    ]
  }
}
```

Query the `last` 2 posts:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
query {
  allPosts(last: 2) {
    id
    title
  }
}
---
{
  "data": {
    "allPosts": [
      {
        "id": "cixnenqen38mb0134o0jp1svy",
        "title": "My latest Hobbies"
      },
      {
        "id": "cixneo7zp3cda0134h7t4klep",
        "title": "My great Vacation"
      }
    ]
  }
}
```

> Note: You cannot combine `first` with `before` or `last` with `after`. If you query more nodes than exist, your response will simply contain all nodes that actually do exist in that direction.

#### Limitations

Note that *a maximum of 1000 nodes* can be returned per pagination field. If you need to query more nodes than that, you can use `first` and `skip` to seek through the different pages. You can also include multiple versions of the same field with different pagination parameter in one query using GraphQL Aliases.

Please join [the discussion on GitHub](https://github.com/graphcool/feature-requests/issues/259) for an according feature request to lift this limitation.

## Custom queries

For use cases that are not covered by the automatically generated CRUD API, [resolver](!alias-xohbu7uf2e) functions can be used to extend your service's GraphQL schema with custom queries and mutations.

You can define the **name, input arguments and payload of the query** and **resolve it with a Graphcool Function**.

### Example

#### Validate the age of a user

Schema Extension SDL document:

```graphql
type AgePayload {
  isValid: Boolean!
  age: Int!
}

extend type Query {
  isValidAge(age: Int!): AgePayload
}
```

Graphcool Function:

```js
module.exports = function age(event) {
  const age = event.data.age

  if (age < 0) {
    return {
      error: "Invalid input"
    }
  }

  const isValid = age >= 18

  return {
    data: {
      isValid,
      age
    }
  }
}
```

Then the query can be called like this using the Simple API:

```graphql
query {
  isValidAge(age: 12) {
    isValid # false
    age # 12
  }
}
```

Note that the returned object contains a `data` key, which in turn contains the `number` field that was specified in the `RandomNumberPayload` in the SDL document. [Error handling](!alias-geihakoh4e) works similarly to other Graphcool Functions, if an object containing the `error` key is returned.


## The authenticated `User` (only for [legacy Console projects](!alias-aemieb1aev))

If the request of a query contains authentication information on the [session user](!alias-geekae9gah#user-login), you can use the `user` query to query information on that user. All fields of the `User` type are available.

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
query {
  user {
    id
  }
}
---
{
  "data": {
    "user": {
      "id": "my-user-id"
    }
  }
}
```

If no user is signed in, the query response will look like this:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
query {
  user {
    id
  }
}
---
{
  "data": {
    "user": null
  }
}
```

Note that you have to set appropriate [permissions](!alias-iegoo0heez) on the `User` type to use the `user` query.
