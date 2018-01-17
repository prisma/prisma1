---
alias: ahwee4zaey
description: Queries
---

# Queries

The Prisma API offers two kinds of queries:

* **Model Queries**, to fetch single or multiple nodes of a certain model
* **Connection Queries**, that expose advanced features like aggregations and [Relay compliant connections](https://facebook.github.io/relay/graphql/connections.htm)

When working with the Prisma API, the following features are also useful to keep in mind:

* **Hierarchical Queries**, allowing to fetch data across relations
* **Query arguments**, for filters, sorting, pagination and more

In general, the Prisma API of a service is structured according to [its data model](!alias-eiroozae8u). To explore the concrete operations in your Prisma API, use the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

In the following, we will explore example queries based on a Prisma service with this data model:

```graphql
type Post {
  id: ID! @unique
  title: String!
  isPublished: Boolean!
  author: User!
}

type User {
  id: ID! @unique
  age: Int
  email: String! @unique
  name: String!
  posts: [Post!]!
}
```

## Model Queries

We can use **model queries** to fetch either a single node, or a list of nodes for a certain model.

Here, we use the `posts` query to fetch a list of posts:

```graphql
# Fetch all posts
query {
  posts {
    id
    title
  }
}
```

We can also query a specific post using `post`. Note that we're using the `where` argument to select the node:

```graphql
# Fetch a post by id
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

Because `User` is a model in our data model, `users` is another available query. Again, we can use the `where` argument to specify conditions for the returned users:

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

This also works across relations:

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

You can read more about [node selection here](!alias-utee3eiquo#node-selection).

## Connection Queries

Model queries directly return list of nodes. In special cases or when using advanced features, using **connection queries** is the preferred option. They are an extension of [Relay connections](https://facebook.github.io/relay/graphql/connections.htm).

Here, we fetch all posts using the `postsConnection` query:

```graphql
# Fetch all posts
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

> Note that additional aggregations will be added over time. You can find more information about that [here](https://github.com/graphcool/graphcool/issues/1312).

## Querying data across relations

Every available [relation](!alias-eiroozae8u#relations) in your data model adds a new field to the queries of the two models it connects.

Here, we are fetching a specific user, and all her related posts using the `posts` field:

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

`user.posts` acts exactly like the top-level `posts`.

## Query arguments

Throughout the Prisma API, you'll find query arguments that you cam provide to further control the query response. It can be either of the following:

- sorting nodes in by field value using `orderBy`
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

> Note: The field you are ordering by does not have to be selected in the actual query. If you do not specify an ordering, the response is automatically ordered ascending by the `id` field.

#### Limitations

It's currently not possible to order responses [by multiple fields](https://github.com/graphcool/feature-requests/issues/62) or [by related fields](https://github.com/graphcool/feature-requests/issues/95). Join the discussion in the feature requests if you're interested in these features!

### Filtering by field

When querying all nodes of a type you can supply different parameters to the `where` argument to filter the query response accordingly. The available options depend on the scalar and relational fields defined on the type in question.

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

Use the [GraphQL Playground](https://github.com/graphcool/graphql-playground) to explore available filter conditions.

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

> Note: You cannot combine `first` with `before` or `last` with `after`. You can also query for more nodes than exist without an error message.

#### Limitations

Note that *a maximum of 1000 nodes* can be returned per pagination field on the shared demo cluster. This limit can be increased on other clusters using [the cluster configuration](https://github.com/graphcool/framework/issues/748).
