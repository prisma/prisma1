---
alias: ahwee4zaey
description: Queries
---

# Queries

The Prisma API offers

* **Model Queries**, to fetch single or multiple nodes of a certain model
* **Connection Queries**, that expose advanced features like _aggregation_ and [Relay compliant connections](TODO N)
* **Query arguments**, for filters, sorting, pagination and more

GraphQL queries are _hierarchical_, allowing you to fetch data across relations in a single request.

In general, the Prisma API of a service is structured according to [its data model](!alias-TODO N). To explore it, use the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

## Examples

Consider a Prisma service with this data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  isPublished: Boolean!
  author: User!
}

type User {
  id: ID! @unique
  age: Int!
  name: String!
  posts: [Post!]!
}
```

### Model Queries

```graphql
# Fetch all posts
query {
  posts {
    id
    title
  }
}
```

```graphql
# Fetch all users older than 18
query {
  users(where: {
    age_gt: 18
  }) {
    id
    name
  }
}
```

```graphql
# Fetch posts from authors that are older than 18
query {
  posts(where: {
    author: {
      age_gt: 18
    }
  }) {
    id
    title
    isPublished
    author {
      name
    }
  }
}
```

### Connection Queries

```graphql
# Fetch all posts using Relay connections
query {
  postsConnection {
    edges {
      node {
        id
        title
      }
    }
  }
}
```

```graphql
# Count all posts with a title containing 'GraphQL'
query {
  postsConnection(where: {
    title_contains: "GraphQL"
  }) {
    aggregate {
      count
    }
  }
}
```

## Fetching a single node

To fetch a node of a certain [model](!alias-eiroozae8u#model-types), all you need to do is provide its `id` (or any another unique field) to the according model query.

For example, for a type called `Post` a top-level query `post` will be generated.

### Fetching a node by `id`

You can always use the `id` field to identify a node.

Query a specific post by its `id`:

```graphql
query {
  post(where: {
    id: "cixnen24p33lo0143bexvr52n"
  }) {
    id
    title
    isPublished
  }
}
```

### Fetching a node by another unique field

You can also supply any [unique field](!alias-eiroozae8u#unique) as an argument to the query to identify a node. For example, if you already declared the `slug` field of the `Post` type to be unique, you could select a post by specifying its `slug`:

Query a specific `Post` node by its `slug`:

```graphql
query {
  post(where: {
    slug: "my-biggest-adventure"
  }) {
    id
    slug
    title
    isPublished
  }
}
```

> Note: You cannot specify two or more unique arguments for one query at the same time.

> Note: You'll find the `where` argument in other parts of the Prisma API, allowing you to specify a node by a unique identifier.

## Fetching multiple nodes

There are two different queries to fetch multiple nodes of a certain [model](!alias-eiroozae8u#model-types):

- A simple query that directly returns the nodes of that type
- An advanced query based on [Relay connections](https://facebook.github.io/relay/graphql/connections.htm), that is also used to return aggregation data

For a sample type `Post`, the corresponding top-level queries are called `posts` and `postsConnection`.

```graphql
# Using `posts`
query {
  posts {
    id
    title
    isPublished
  }
}
```

```graphql
# Using `postsConnection`
query {
  postsConnection {
    edges {
      node {
        id
        title
        isPublished
      }
    }
    aggregate {
      count
    }
  }
}
```

Here are a few examples for the generated query names:

- type name: `Post`, query name: `posts`, `postsConnection`
- type name: `Todo`, query name: `todoes`, `todoesConnection`
- type name: `Hobby`, query name: `hobbies`, hobbiesConnection`

> Note: The query name approximate the plural rules of the English language. If you are unsure about the actual query name, explore available queries in your [GraphQL Playground](https://github.com/graphcool/graphql-playground).

## Aggregations

<InfoBox type=info>

More **Aggregation** features will be implemented soon. For more information, follow [the discussion on GitHub](https://github.com/graphcool/framework/issues/1312).

</InfoBox>

For every type in your GraphQL schema, different aggregation queries are available through the connection-based approach. Here is a list of all aggregation options that can be applied to a list:

- `avg`
- `median`
- `min` / `max`
- `count`
- `sum`

For the following examples, assume the `User` type looks as follows:

```graphql
type User {
  id: ID! @unique
  name: String!
  age: Int!
}
```

### Count all nodes

Count the number of all `User` nodes:

```graphql
query {
  usersConnection {
    aggregate {
      count
    }
  }
}
```

### Compute the average for a specific field

Compute the average `age` of all `User` nodes:

```graphql
query {
  usersConnection {
    aggregate {
      avg {
        age
      }
    }
  }
}
```

### Compute the median for a specific field

Compute the median `age` of all `User` nodes:

```graphql
query {
  usersConnection {
    aggregate {
      median {
        age
      }
    }
  }
}
```

### Compute the min/max values for a specific field

Compute the median `min`/`max` value for `age` of all `User` nodes:

```graphql
query {
  usersConnection {
    aggregate {
      min { # or max
        age
      }
    }
  }
}
```

### Compute the sum for a specific field

Compute the sum of the `age` fields of all `User` nodes:

```graphql
query {
  usersConnection {
    aggregate {
      sum {
        age
      }
    }
  }
}
```

### Using multiple aggregation fields simultaneously

It's also possible to use multiple aggregation options within the same query. In this example, the query asks for the total `count` of `User` nodes as well the `sum` and `max` values for the `age` field:

```graphql
query {
  usersConnection {
    aggregate {
      count
      sum {
        age
      }
      max {
        age
      }
    }
  }
}
```

## Querying data across relations

Every available [relation](!alias-eiroozae8u#relations) in your data model adds a new field to the queries of the two models it connects.

For example, consider the following schema:

```graphql
type Post {
  id: ID! @unique
  title: String!
  author: User
}

type User {
  id: ID! @unique
  name : String!
  posts: [Post!]!
}
```

In the actual database schema, the `User` type will have an additional field that exposes the list of `Post` node through connections mentioned above. So, it will look as follows:

```graphql
type User {
  id: ID! @unique
  name : String!
  posts: [Post!]!
  postsConnection: PostConnection!
}
```

The following fields will be available:

- the `post` and `posts` queries expose a new `author` field to [traverse one node](#traversing-a-single-node)
- the `postConnection` exposes the `edges` (of type `[PostEdge!]`) field where each `PostEdge` again has a field called `node` which is of type `Post`
- the `user` and `users` queries expose a new `posts` field to [traverse many nodes](#traversing-many-nodes)

### Traversing a single node

Traversing edges that connect the current node to the one side of a relation can be done by simply selecting the according field defined with the relation.

Query information on the `author` node connected to a specific `Post` node:

```graphql
query {
  post(where: {
    id: "cixnen24p33lo0143bexvr52n"
  }) {
    id
    author {
      id
      name
      email
    }
  }
}
```

The `author` field exposes a further selection of properties that are defined on the `User` type.

> Note: You can add [filter query arguments](#filtering-by-field) to an inner field returning a single node.

### Traversing many nodes

Like before, many nodes can be retrieved either using the simple approach and directly accessing a list of nodes or using the more advanced connections.

#### Traversing many nodes of a specific type directly

Query information on all `Post` nodes of a certain `User` node:

```graphql
query {
  user(where: {
    id: "cixnekqnu2ify0134ekw4pox8"
  }) {
    id
    name
    posts {
      id
      isPublished
    }
  }
}
```

The `posts` field exposes a further selection of properties that are defined on the `Post` type.

#### Traversing many nodes of a specific type using connections

Query information on all `Post` nodes of a certain `User` node:

```graphql
query {
  user(where: {
    id: "cixnekqnu2ify0134ekw4pox8"
  }) {
    id
    name
    postConnection {
      edges {
        node {
          id
          isPublished
        }
      }
    }
  }
}
```

## Query arguments

All queries and fields that return many nodes accept different query arguments to further control the query response. The response can be either of the following:

- [ordered by field](#ordering-by-field)
- [filtered by multiple fields](#filtering-by-field)
- [paginated](#pagination) into multiple chunks by fixing one specific node and either seeking forwards or backwards

These query arguments can be combined to achieve very specific query responses.

### Ordering by field

When querying all nodes of a type you can supply the `orderBy` argument for every scalar field of the type: `orderBy: <field>_ASC` or `orderBy: <field>_DESC`.

Order the list of all `Post` nodes ascending by `title`:

```graphql
query {
  posts(orderBy: title_ASC) {
    id
    title
    isPublished
  }
}
```

Order the list of all `Post` nodes descending by `isPublished`:

```graphql
query {
  posts(orderBy: isPublished_DESC) {
    id
    title
    isPublished
  }
}
```

> Note: The field you are ordering by does not have to be selected in the actual query. If you do not specify an ordering, the response is implicitely ordered ascending by the `id` field.

#### Limitations

It's currently not possible to order responses [by multiple fields](https://github.com/graphcool/feature-requests/issues/62) or [by related fields](https://github.com/graphcool/feature-requests/issues/95). Join the discussion in the feature requests if you're interested in these features!

### Filtering by field

When querying all nodes of a type you can supply different parameters to the `where` argument to filter the query response accordingly. The available options depend on the scalar and relational fields defined on the type in question.

You can also include filters when [traversing your data graph](#relation-queries).

#### Applying single filters

If you supply exactly one parameter to the `where` argument, the query response will only contain nodes that adhere to this constraint.

##### Filtering by value

The easiest way to filter a query response is by supplying a field value to filter by.

Query all `Post` nodes that are not yet `isPublished`:

```graphql
query {
  posts(where: {
    isPublished: false
  }) {
    id
    title
    isPublished
  }
}
```

##### Advanced filter criteria

Depending on the type of the field you want to filter by, you have access to different advanced criteria you can use to filter your query response. See how to [explore available filter criteria](#explore-available-filter-criteria).

Query all `Post` nodes whose `title` is in a given list of strings:

```graphql
query {
  posts(where: {
    title_in: ["My biggest Adventure", "My latest Hobbies"]
  }) {
    id
    title
    isPublished
  }
}
```

> Note: you have to supply a _list_ as the `<field>_in` argument: `title_in: ["My biggest Adventure", "My latest Hobbies"]`.

#### Relation filters

For to-one relations, you can define conditions on the related node by nesting the according argument in `where`.

Query all `Post` nodes where the `author` has the `USER` access role:

```graphql
query {
  posts(where: {
    author: {
      accessRole: USER
    }
  }) {
    title
  }
}
```

For to-many relations, three additional arguments are available: `every`, `some` and `none`, to define that a condition should match every, some or none related nodes.

Query all `User` nodes that have at least one `Post` node that's `isPublished`:

```graphql
query {
  users(where: {
    posts_some: {
      isPublished: true
    }
  }) {
    id
    posts {
      isPublished
    }
  }
}
```

Relation filters are also available in the nested arguments for to-one or to-many relations.

Query all `User` nodes that did not _like_ a `Post` of an `author` in the `ADMIN` access role:

```graphql
query {
  users(where: {
    likedPosts_none: {
      author: {
        accessRole: ADMIN
      }
    }
  }) {
    name
  }
}
```

#### Combining multiple filters

You can use the filter combinators `OR` and `AND` to create an arbitrary logical combination of filter conditions.

##### Using `OR` or `AND`

Let's start with an easy example:

Query all `Post` nodes that are `isPublished` _and_ whose `title` is in a given list of strings:

```graphql
query {
  posts(where: {
    AND: [{
      title_in: ["My biggest Adventure", "My latest Hobbies"]
    }, {
      isPublished: true
    }]
  }) {
    id
    title
    isPublished
  }
}
```

> Note: `OR` and `AND` accept a _list_ as input where each list item is an object and therefore needs to be wrapped with `{}`, for example: `AND: [{title_in: ["My biggest Adventure", "My latest Hobbies"]}, {isPublished: true}]`

##### Arbitrary combination of filters with `AND` and `OR`

You can combine and even nest the filter combinators `AND` and `OR` to create arbitrary logical combinations of filter conditions.

Query all `Post` nodes that are either `isPublished` _and_ whose `title` is in a list of given strings, _or_ have the specific `id` we supply:

```graphql
query($isPublished: Boolean) {
  posts(where: {
    OR: [{
      AND: [{
        title_in: ["My biggest Adventure", "My latest Hobbies"]
      }, {
        isPublished: $isPublished
      }]
    }, {
      id: "cixnen24p33lo0143bexvr52n"
    }]
  }) {
    id
    title
    isPublished
  }
}
```

> Notice how we nested the `AND` combinator inside the `OR` combinator, on the same level with the `id` value filter.

#### Explore available filter criteria

Apart from the filter combinators `AND` and `OR`, the available filter arguments for a query for all nodes of a type depend on the fields of the type and their types.

Let's consider the following schema:

```graphql
type MyType {
  id: ID! @unique
  createdAt: DateTime!
  text: String!
  number: Int!
  decimal: Float!
  flag: Boolean!
  date: DateTime!
  enum: SomeEnum!
  object: Json!
}
```

Based on this type, a `MyTypeFilter` type will be generated with the following fields (grouped by field type):

```graphql
input MyTypeFilter {
  # logical operators
  AND: [MyTypeFilter!] # combines all passed `MyTypeFilter` objects with logical AND
  OR: [MyTypeFilter!] # combines all passed `MyTypeFilter` objects with logical OR

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
  enum: MYTYPE_ENUM # matches all nodes with exact value
  enum_not: MYTYPE_ENUM # matches all nodes with different value
  enum_in: [MYTYPE_ENUM!] # matches all nodes with value in the passed list
  enum_not_in: [MYTYPE_ENUM!] # matches all nodes with value not in the passed list

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

### Pagination

When querying all nodes of a specific [model type](!alias-eiroozae8u#model-types), you can supply arguments that allow you to _paginate_ the query response.

Pagination allows you to request a certain amount of nodes at the same time. You can seek forwards or backwards through the nodes and supply an optional starting node:

- to seek forwards, use `first`; specify a starting node with `after`.
- to seek backwards, use `last`; specify a starting node with `before`.

You can also skip an arbitrary amount of nodes in whichever direction you are seeking by supplying the `skip` argument.

Consider a blog where only 3 `Post` nodes are shown at the front page. To query the first page:

```graphql
query {
  posts(first: 3) {
    id
    title
  }
}
```

To query the first two `Post` node after the first `Post` node:

```graphql
query {
  posts(
    first: 2
    after: "cixnen24p33lo0143bexvr52n"
  ) {
    id
    title
  }
}
```

We could reach the same result by combining `first` and `skip`:

```graphql
query {
  posts(
    first: 2
    skip: 1
  ) {
    id
    title
  }
}
```

Query the `last` 2 posts:

```graphql
query {
  posts(last: 2) {
    id
    title
  }
}
```

> Note: You cannot combine `first` with `before` or `last` with `after`. If you query more nodes than exist, your response will simply contain all nodes that actually do exist in that direction.

#### Limitations

Note that *a maximum of 1000 nodes* can be returned per pagination field on the shared demo cluster. This limit can be increased on other clusters using [the cluster configuration](https://github.com/graphcool/framework/issues/748).
