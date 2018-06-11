---
alias: ahwee4zaey
description: Queries
---

# Queries

The Prisma API offers two kinds of queries:

* **Object queries**: Fetch single or multiple nodes of a certain [object type](!alias-eiroozae8u#object-types)
* **Connection queries**: Expose advanced features like aggregations and [Relay compliant connections](https://facebook.github.io/relay/graphql/connections.htm) enabling a powerful pagination model

When working with the Prisma API, the following features are also useful to keep in mind:

* **Hierarchical queries**: Fetch data across relations
* **Query arguments**: Allow for filtering, sorting, pagination and more

In general, the Prisma API of a service is generated based on its [data model](!alias-eiroozae8u). To explore the operations in your Prisma API, you can use a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

In the following, we will explore example queries based on a Prisma service with this data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  published: Boolean!
  author: User!
}

type User {
  id: ID! @unique
  age: Int
  email: String! @unique
  name: String!
  accessRole: AccessRole
  posts: [Post!]!
}

enum AccessRole {
  USER
  ADMIN
}
```

## Object queries

We can use **object queries** to fetch either a single node, or a list of nodes for a certain object type.

Here, we use the `posts` query to fetch a list of `Post` nodes. In the response, we include only the `id` and `title` of each `Post` node:

```graphql
query {
  posts {
    id
    title
  }
}
```

We can also query a specific `Post` node using the `post` query. Note that we're using the `where` argument to select the node:

```graphql
query {
  post(where: {
    id: "cixnen24p33lo0143bexvr52n"
  }) {
    id
    title
    published
  }
}
```

Because `User` is another type in our data model, `users` is another available query. Again, we can use the `where` argument to specify conditions for the returned users. In this example, we filter for all `User` nodes that have their `age` higher than `18`:

```graphql
query {
  users(where: {
    age_gt: 18
  }) {
    id
    name
  }
}
```

This also works across relations, here we're fetching those `Post` nodes that have an `author` with the `age` higher than `18`:

```graphql
query {
  posts(where: {
    author: {
      age_gt: 18
    }
  }) {
    id
    title
    author {
      name
      age
    }
  }
}
```

You can read more about [node selection here](!alias-utee3eiquo#node-selection).

## Connection queries

Object queries directly return a list of nodes. In special cases, or when using advanced features, using **connection queries** is the preferred option. They are an extension of (and fully compliant with) [Relay connections](https://facebook.github.io/relay/graphql/connections.htm). The core idea of Relay connections is to provide meta-information about the _edges_ in the data graph. For example, each edge not only has access to information about the corresponding object (the `node`) but also is associated with a `cursor` that allows to implement powerful pagination.

Here, we fetch all `Post` nodes using the `postsConnection` query. Notice that we're also asking for the `cursor` of each edge:

```graphql
# Fetch all posts
query {
  postsConnection {
    edges {
      cursor
      node {
        id
        title
      }
    }
  }
}
```

Connection queries also expose **aggregation** features via `aggregate`:

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

> Note that more aggregations will be added over time. Find more information about the roadmap [here](https://github.com/graphcool/graphcool/issues/1312).

## Querying data across relations

Every available [relation](!alias-eiroozae8u#relations) in your data model adds a new field to the queries of the two models it connects.

Here, we are fetching a specific `User`, and all her related `Post` nodes using the `posts` field:

```graphql
query {
  user(where: {
    id: "cixnekqnu2ify0134ekw4pox8"
  }) {
    id
    name
    posts {
      id
      published
    }
  }
}
```

`user.posts` acts exactly like the top-level `posts` query in that it lets you specify which fields of the `Post` type you're interested in.

## Query arguments

Throughout the Prisma API, you'll find query arguments that you can provide to further control the query response. It can be either of the following:

- sorting nodes by any field value using `orderBy`
- selecting nodes in a query by scalar or relational filters using `where`
- paginating nodes in a query using `first` and `before`, `last` and `after`, and `skip`

These query arguments can be combined to achieve very specific query responses.

### Ordering by field

When querying all nodes of a type you can supply the `orderBy` argument for every scalar field of the type: `orderBy: <field>_ASC` or `orderBy: <field>_DESC`.

Order the list of all `Post` nodes ascending by `title`:

```graphql
query {
  posts(orderBy: title_ASC) {
    id
    title
    published
  }
}
```

Order the list of all `Post` nodes descending by `published`:

```graphql
query {
  posts(orderBy: published_DESC) {
    id
    title
    published
  }
}
```

> **Note**: The field you are ordering by does not have to be selected in the actual query. If you do not specify an ordering, the response is automatically ordered ascending by the `id` field.

#### Limitations

It's currently not possible to order responses [by multiple fields](https://github.com/graphcool/feature-requests/issues/62) or [by related fields](https://github.com/graphcool/feature-requests/issues/95). Join the discussion in the feature requests if you're interested in these features!

### Filtering by field

When querying all nodes of a type you can supply different parameters to the `where` argument to constrain the data in the response according to your requirements. The available options depend on the scalar and relational fields defined on the type in question.

#### Applying single filters

If you supply exactly one parameter to the `where` argument, the query response will only contain nodes that adhere to this constraint. Multiple filters can be combined using `AND` and/or `OR`, see [below](#arbitrary-combination-of-filters-with-and-and-or) for more.

##### Filtering by value

The easiest way to filter a query response is by supplying a field value to filter by.

Query all `Post` nodes that are not yet `published`:

```graphql
query {
  posts(where: {
    published: false
  }) {
    id
    title
    published
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
    published
  }
}
```

> **Note**: you have to supply a _list_ as the `<field>_in` argument: `title_in: ["My biggest Adventure", "My latest Hobbies"]`.

#### Relation filters

For _to-one_ relations, you can define conditions on the related node by nesting the according argument in `where`.

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

For _to-many_ relations, three additional arguments are available: `every`, `some` and `none`, to define that a condition should match `every`, `some` or `none` related nodes.

Query all `User` nodes that have _at least_ one `Post` node that's `published`:

```graphql
query {
  users(where: {
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
```

Relation filters are also available in the nested arguments for _to-one_ or _to-many_ relations.

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

> **Note**: `likedPosts` is not part of the above mentioned data model but can easily be added by adding the corresponding field to the `User` type: `likedPosts: [Post!]! @relation(name: "LikedPosts")`. Note that we also provide a `name` for the relation to resolve the ambiguity we would otherwise create because there are two relation fields targetting `Post` on the `User` type.

#### Combining multiple filters

You can use the filter combinators `OR`, `AND`, `NOT` to create an arbitrary logical combination of filter conditions.
For `AND` all of the nested conditions have to be true. For `OR` one of the nested conditions have to be true. For `NOT` all of the nested conditions have to be false since they are combined by and internally.

##### Using `OR`, `AND` and `NOT`

Let's start with an easy example:

Query all `Post` nodes that are `published` _and_ whose `title` is in a given list of strings:

```graphql
query {
  posts(where: {
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
```

> **Note**: `OR`, `AND` and `NOT`and  accept a _list_ as input where each list item is an object and therefore needs to be wrapped with `{}`, for example: `AND: [{title_in: ["My biggest Adventure", "My latest Hobbies"]}, {published: true}]`

##### Arbitrary combination of filters with `AND`, `OR` and `NOT`

You can combine and even nest the filter combinators `AND`, `OR` and `NOT` to create arbitrary logical combinations of filter conditions.

Query all `Post` nodes that are either `published` _and_ whose `title` is in a list of given strings, _or_ have the specific `id` we supply:

```graphql
query($published: Boolean) {
  posts(where: {
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
```

> Notice how we nested the `AND` combinator inside the `OR` combinator, on the same level with the `id` value filter.

#### Explore available filter criteria

Apart from the filter combinators `AND`, `OR` and `NOT`, the available filter arguments for a query for all nodes of a type depend on the fields of the type and their types.

Use the [GraphQL Playground](https://github.com/graphcool/graphql-playground) to explore available filter conditions.

#### Limitations

Currently, neither [**scalar list filters**](https://github.com/graphcool/feature-requests/issues/60) nor [**JSON filters**](https://github.com/graphcool/feature-requests/issues/148) are available. Join the discussion in the respective feature requests on GitHub.

### Pagination

When querying all nodes of a specific [object type](!alias-eiroozae8u#object-types), you can supply arguments that allow you to _paginate_ the query response.

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

To query the first two `Post` nodes after the first `Post` node:

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

> **Note**: You cannot combine `first` with `before` or `last` with `after`. You can also query for more nodes than exist without an error message.

#### Limitations

Note that by default, Prisma returns *a maximum of 1000 nodes*. This can be overridden by setting the pagination parameters accordingly. If you do not set any pagination parameters, Prisma will set a limit of 1000.
