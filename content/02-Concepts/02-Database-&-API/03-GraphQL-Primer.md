# GraphQL Primer

> In case you're already familiar with GraphQL, you can skip to the [next chapter](http:/#)

## GraphQL Overview

[GraphQL](http://www.graphql.org/) is a new API standard that provides a more efficient, powerful and flexible [alternative to RESTful APIs](https://www.howtographql.com/basics/1-graphql-is-the-better-rest/). It was developed and [open-sourced by Facebook](https://facebook.github.io/react/blog/2015/02/20/introducing-relay-and-graphql.html) and is now maintained by a large community of companies and individuals from all over the world.

At its core, GraphQL enables **declarative data fetching** where a client can specify exactly what data it needs from an API. Instead of multiple endpoints that return fixed data structures (like with REST APIs), a GraphQL server only exposes a single endpoint and returns exactly the data the client asked for.

To learn more about GraphQL, visit the fullstack GraphQL tutorial [How to GraphQL](https://www.howtographql.com/). 

## Fetching Data with Queries

In GraphQL, a client can request data from a server using a [`query`](http://graphql.org/learn/queries). GraphQL is *transport-layer agnostic*, which means the query can be transferred to the server over any available network protocol. That said, the most common implementations of GraphQL server use HTTP POST and append the query to the *body* of the request.

Here is a sample query to ask for all the `Person` objects that are currently stored in the database, for each object this query only requests the `name`:

```graphql
{
  allPersons {
    name
  }
}
```

`allPersons` is called a *root field* of the query. Everything that follows the root field is called the *payload *of the query.

When processing this request, the server will only respond with the information that's specified in the query. So, here it will return an array of `Person` objects which could look as follows:

```js
{
  "allPersons": [
    { "name": "Johnny" },
    { "name": "Sarah" },
    { "name": "Alice" }
  ]
}
```

Notice that with all queries that a GraphQL server receives, the JSON response will always follow the shape of the query. Therefore, another perspective of looking at a GraphQL query is simply as an *empty JSON object* that currently only contains *keys* and where the server will fill in the *values* appropriately.

In the above example, you might as well ask for all the `posts` that have been authored by the users. In that case, you would end up with a query similar to this:

```graphql
{
  allPersons {
    name
    posts {
      title
    }
  }
}
```

## Creating, Updating & Deleting Data with Mutations

Most of the times when working with an API, you'll not only want to *fetch* data, but also change it. With GraphQL, these changes are made through so-called [mutations](http://graphql.org/learn/queries/#mutations).

Mutations follow the exact same syntactical structure as queries, except that they *need* to start with the `mutation` keyword to specify the GraphQL operation type. 

A simple mutation to tell the GraphQL server to create a new `Person` object could look as follows:

```graphql
mutation {
  createPerson(name: "Bob") {
    id
  }
}
```


Similar to the above query, `createPerson` is called the *root field* as well. Root fields actually play a special role when it comes to the GraphQL schema, we'll cover this in a bit.

Notice that the `createPerson` field also accepts an argument, here that's the `name` of the `Person` to be created. Though we haven't seen an example of this yet, a query can also specify arguments for its fields, this is commonly used for filters, ordering and pagination functionality.

Similar to a query, a mutation also allows to specify a payload. In the case above, only the `id` of the newly created person is included in the mutation's payload, but we might as well include more fields of the `Person`.

The corresponding server response could look like this: 

```js
{
  "createPerson": {
    "id": "aih0liehogeewiec"
  }
}
```

Again, the server response follows the structure of the JSON object and fills in the values for the fields that have been specified in the mutation.

## Realtime with Subscriptions

GraphQL further offers a mechanism for clients to receive event-based realtime updates from the server. Therefore, the client needs to *subscribe* to a specific event by sending a *subscription query*. Whenever that event now happens, the server will push the data that was specified in the subscription query to the client.

Again, a subscription query is based on the exact same syntax as the queries and mutations that we saw before. Here is an example to subscribe to the event of a new `Post` being created. With this subscription query, the server will push the `title` as well as the `author`'s `name` to the client:

```graphql
subscription {
  Post(filter: {
    mutation_in: [CREATED]
  }) {
    node {
      title
      author {
        name
      }
    }
  }
}
```

In this case, `Post` is the root field of the subscription query. We further specify a `filter` object that allows us to define that we're only interested in *created*-mutations. If we also wanted to be notified about *updated*- and *deleted*-mutations, we would only have to update the array to also include these values: `[CREATED, UPDATED, DELETED]`.

### GraphQL Schema vs Database Schema

When using Graphcool, there is an important distinction to make between the* GraphQL Schema* that defines the capabilities actual API and the *Database Schema* that's used for the data model.

The *Database Schema* only defines the *data model*, i.e. it represents the objects from your *application domain*. An example for this is the schema for the blogging application that we saw above. `Person` and `Post` are part of the Database Schema and are called *Model Types*. 

Generally when using GraphQL though, the schema has an even broader role and will contain additional definitions that don't directly model entities from the application domain. In particular, there are three *root types* that represent the entry-points to the API. These root types are called: `Query`, `Mutation` and `Subscription`.

The fields of these root types correspond precisely to the *root fields* that can be used in queries, mutations and subscriptions that the server receives. So, if we wanted to make the three examples work that we've seen in the previous sections, we'd have to define the following schema:

```graphql
###################################################################
# Root Types: Define the entry points for the API
###################################################################

type Query {
  allPersons: [Person!]!
}

type Mutation {
  createPerson(name: String!): Person
}

type Subscription {
  Post(filter: PostSubscriptionFilter): PostSubscriptionPayload
}


###################################################################
# Model Types: Represent entities from the application domain
###################################################################

type Person {
  name: String!
  posts: [Post!]!
}

type Post {
  title: String!
  content: String
  author: Person!
}


###################################################################
# Free Types: Remaining types to complete the API
###################################################################

type PostSubscriptionFilter {
  AND: [PostSubscriptionFilter!]
  OR: [PostSubscriptionFilter!]
  mutation_in: [_ModelMutationType!]
  node: PostSubscriptionFilterNode
}

type PostSubscriptionPayload {
  mutation: _ModelMutationType!
  node: Person
  updatedFields: [String!]
}

enum _ModelMutationType {
  CREATED
  UPDATED
  DELETED
}
```

As you can see, the schema not only contains the Model Types that represent the entities from the application domain (`Person` and `Post`), but also includes the mentioned Root Types, plus a number of Free Types to refine the API.
