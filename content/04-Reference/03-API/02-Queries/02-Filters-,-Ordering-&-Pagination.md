---
alias: ohrai1theo
path: /docs/reference/simple-api/query-arguments
layout: REFERENCE
shorttitle: Query Arguments
description: All queries and fields that return many nodes accept different query arguments to filter, order and paginate the response.
simple_relay_twin: on1yeiw7ph
tags:
  - simple-api
related:
  further:
  more:
---

# Query Arguments in the Simple API

All queries and fields that return many nodes accept different query arguments to further control the query response. The response can be

* [ordered by field](!alias-vequoog7hu).
* [filtered by multiple fields](!alias-xookaexai0).
* [paginated](!alias-ojie8dohju) into multiple pages by fixing one specific node and either seeking forwards or backwards.

These query arguments can be combined to achieve very specific query responses.


## Filtering by field

When querying all nodes of a type you can supply different parameters to the `filter` argument to filter the query response accordingly. The available options depend on the scalar fields defined on the type in question.

You can also include filters when including related fields in your queries to [traverse your data graph](!alias-aihaeph5ip).

### Applying single filters

If you supply exactly one parameter to the `filter` argument, the query response will only contain nodes that fulfill this constraint.

#### Filtering by value

The easiest way to filter a query response is by supplying a field value to filter by.

> Query all posts not yet published:

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

#### Advanced filter criteria

Depending on the type of the field you want to filter by, you have access to different advanced criteria you can use to filter your query response. See how to [explore available filter criteria](#explore-available-filter-criteria).

> Query all posts whose title are in a given list of titles:

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

Note: you have to supply a *list* as the `<field>_in` argument: `title_in: ["My biggest Adventure", "My latest Hobbies"]`.

#### Relation filters

* For to-one relations, you can define conditions on the related node by nesting the according argument in `filter`

> Query all posts of authors with the `USER` access role

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


* For to-many relations, three additional arguments are available: `every`, `some` and `none`, to define that a condition should match every, some or none related nodes.

> Query all users that have at least one published post

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

* Relation filters are also available in the nested arguments for to-one or to-many relations.

> Query all users that did not like a post of an author in the `ADMIN` access role

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

### Combining multiple filters

You can use the filter combinators `OR` and `AND` to create an arbitrary logical combination of filter conditions.

#### Using `OR` or `AND`

Let's start with an easy example:

> Query all published posts whose title are in a given list of titles:

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

Note: `OR` and `AND` accept a *list* as input where individual list items have to be wrapped by `{}`: `AND: [{title_in: ["My biggest Adventure", "My latest Hobbies"]}, {published: true}]`

#### Arbitrary combination of filters with `AND` and `OR`

You can combine and even nest the filter combinators `AND` and `OR` to create arbitrary logical combinations of filter conditions.

> Query all posts that are either published and in a list of given titles, or have the specific id we supply:

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

Note how we nested the `AND` combinator inside the `OR` combinator, on the same level with the `id` value filter.

### Explore available filter criteria

Apart from the filter combinators `AND` and `OR`, the available filter arguments for a query for all nodes of a type depend on the fields of the type and their types.

Let's consider the following schema:

```graphql
type Meta {
  id: ID!
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

### Limitations

Currently, neither [**scalar list filters**](https://github.com/graphcool/feature-requests/issues/60) nor [**JSON filters**](https://github.com/graphcool/feature-requests/issues/148) are available. Join the discussion in the respective feature requests on GitHub!

If you want to filter a list of strings `tags: [String!]!`:

```graphql
type Item {
  id: ID!
  tags: [String!]!
}
```

You can introduce a new type `Tag` with a single `key: String` field and connect `Item` to `Key` one-to-many or many-to-many:

```graphql
type Item {
  id: ID!
  tags: [Tag!]! @relation(name: "ItemTags")
}

type Tag {
  id: ID!
  key: String!
  item: Item @relation(name: "ItemTags")
}
```

Now you can filter items based on their connected tags using the `tag_none`, `tags_some` and `tags_every` filters.


## Ordering by field

When querying all nodes of a [type](!alias-ij2choozae) you can supply the `orderBy` argument for every scalar field of the type: `orderBy: <field>_ASC` or `orderBy: <field>_DESC`.

> Order the list of all posts ascending by title:

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

> Order the list of all posts descending by published:

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

Note: The field you are ordering by does not have to be selected in the actual query.
Note: If you do not specify an ordering, the response is implicitely ordered ascending by the `id` field

### Limitations

It's currently not possible to order responses [by multiple fields](https://github.com/graphcool/feature-requests/issues/62) or [by related fields](https://github.com/graphcool/feature-requests/issues/95). Join the discussion in the feature requests if you're interested in these features!


## Pagination

When querying all nodes of a specific [type](!alias-ij2choozae) you can supply arguments that allow you to paginate the query response.

Pagination allows you to request a certain amount of nodes at the same time. You can seek forwards or backwards through the nodes and supply an optional starting node:
* to seek forwards, use `first`; specify a starting node with `after`.
* to seek backwards, use `last`; specify a starting node with `before`.

You can also skip an arbitrary amount of nodes in whichever direction you are seeking by supplying the `skip` argument.

> Consider a blog where only 3 posts are shown at the front page. To query the first page:

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

> To query the first two posts after the first post:

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

> We could reach the same result by combining `first` and `skip`:

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

> Query the `last` 2 posts:

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

Note: You cannot combine `first` with `before` or `last` with `after`.
Note: If you query more nodes than exist, your response will simply contain all nodes that actually do exist in that direction.

### Limitations

Note that *a maximum of 1000 nodes* can be returned per pagination field. If you need to query more nodes than that, you can use `first` and `skip` to seek through the different pages. You can also include [multiple versions of the same field with different pagination parameters](!alias-cahzai2eur) in one query using GraphQL Aliases.

Please join [the discussion on GitHub](https://github.com/graphcool/feature-requests/issues/259) for an according feature request to lift this limitation.
