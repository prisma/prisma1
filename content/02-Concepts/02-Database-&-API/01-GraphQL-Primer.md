---
alias: leum4poh1e 
description: An introduction to GraphQL.
---

# GraphQL Primer

> In case you're already familiar with GraphQL, please continue with the [next section](!alias-sai7aes3iv).


## GraphQL Overview

[GraphQL](http://www.graphql.org/) is a new API standard that provides a more efficient, powerful and flexible [alternative to RESTful APIs](https://www.howtographql.com/basics/1-graphql-is-the-better-rest/). It was developed and [open-sourced by Facebook](https://facebook.github.io/react/blog/2015/02/20/introducing-relay-and-graphql.html) and is now maintained by a large community of companies and individuals from all over the world.

At its core, GraphQL enables **declarative data fetching** where a client can specify exactly what data it needs from an API. Instead of multiple endpoints that return fixed data structures (like with REST APIs), a GraphQL server only exposes a single endpoint and returns exactly the data the client asked for.

GraphQL can be used to either complement or even entirely replace REST APIs. If you want to learn more, visit the fullstack GraphQL tutorial [How to GraphQL](https://www.howtographql.com/). 


## The GraphQL Schema Definition Language

GraphQL has its own syntax to define data types and express relationships between them, it's called the GraphQL [Schema Definition Language](https://www.graph.cool/docs/faq/graphql-sdl-schema-definition-language-kr84dktnp0/) (SDL). As an example, let's consider a simple blogging application where we might define two simple model types using the SDL:

```graphql
type Person {
  id: ID!
  name: String!
  posts: [Post!]!
}

type Post {
  id: ID!
  title: String!
  content: String
  author: Person!
}
```

The `type` keyword denotes the definition of a new data type. Each type has a number of *fields* that represent the properties and relationships of the type. The `Person` type in the example above has the `name` field (of type `String`) as well as a `posts` field that represents a one-to-many relation to the `Post` type. Notice that the exclamation point following the type of a field means that it cannot be `null`.

Besides types, the SDL offers constructs like enums, interfaces, union types as well as custom directives so you have a powerful foundation to build your data model from the ground up. You can learn more about all the language constructs of the SDL on the official [GraphQL website](http://graphql.org/learn/schema/#scalar-types).


## Fetching Data with Queries

With GraphQL, a client can request data from a server by sending a [query](http://graphql.org/learn/queries) which contains the client's data needs. Let's again consider the above example of a blogging application where the date model consists of a `Person` and a `Post` type.

Here is a sample query to ask for all the `Person` objects that are currently stored in the database, for each object this query only requests the `name`:

```graphql
{
  allPersons {
    name
  }
}
```

> Notice that queries are usually sent in the body of an HTTP request instead of `GET` requests as would be the case when requesting data from a REST API.

`allPersons` is called the *root field* of the query. Everything that follows the root field is called the *selection set *of the query and defines exactly what data the client requests from the server.

When processing this request, the server will only respond with the information that's specified in the query. So, here it will return an array of `Person` objects which could look like this:

```json
{
  "data": {
    "allPersons": [
      { "name": "Johnny" },
      { "name": "Sarah" },
      { "name": "Alice" }
    ]
  }
}
```

Notice that with all queries that a GraphQL server receives, the JSON response will always match the shape of the query. Therefore, you can think of a GraphQL query as an *empty JSON object* that only contains *keys* and where the server fills in the *values* appropriately. The server further wraps all the requested information with a key called `data`, as [defined in the official GraphQL specification](https://facebook.github.io/graphql/#sec-Response-Format).

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

Here is where GraphQL unfolds its real power, that is to traverse *relationships* in the data graph and retrieve information of multiple related entities with a single request. 

![](https://imgur.com/oE2mF7D.png)


## Creating, Updating & Deleting Data with Mutations

Most of the times when working with an API, you'll not only want to *fetch *data, but also *create*, *update* or *delete* some data. With GraphQL, these changes are made through so-called [mutations](http://graphql.org/learn/queries/#mutations).

> You can think of mutations as the GraphQL equivalent for `POST`, `PUT` and `DELETE` requests you know from REST APIs.

Mutations follow the exact same syntactical structure as queries, except that they *need *to start with the `mutation` keyword to specify the GraphQL *operation type*. 

A simple mutation to tell the GraphQL server to create a new `Person` object could look as follows:

```graphql
mutation {
  createPerson(name: "Bob") {
    id
  }
}
```

Similar to the above query, `createPerson` is called the *root field*. Root fields actually play a special role when it comes to the GraphQL schema, we'll cover this in a bit.

Notice that the `createPerson` field also accepts an argument, here that's the `name` of the `Person` to be created. Though we haven't seen an example of this yet, a *query* can also specify arguments for its fields, this is commonly used for filters, ordering and pagination functionality.

Similar to a query, a mutation also allows to specify a selection set to ask for data that should be returned by the server. In that sense, you can think of a mutation as a request to *write* to the database and is immediately followed by a *read*. In the case above, only the `id` of the newly created person is included in the mutation's selection set, but we might as well include more fields of the `Person`.

The corresponding server response could look like this: 

```json
{
  "createPerson": {
    "id": "aih0liehogeewiec"
  }
}
```

Again, the server response follows the structure of the JSON object and fills in the values for the fields that have been specified in the selection set of the mutation.

![](https://imgur.com/oE2mF7D.png)

### Realtime with subscriptions

GraphQL further offers a mechanism for clients to receive *event*-*based* realtime updates from the server. Therefore, the client needs to *subscribe* to a specific event (e.g. when a new user was created). Every time that particular event then happens, the server will push the event data to the client. In contrast to queries and mutations that follow a typical HTTP request/response-cycle, GraphQL subscriptions require a permanent connection from the server to the client (commonly implemented using websockets).

> You can learn more about events and their role in the Graphcool system in the next chapter.

The client can subscribe by sending a *subscription query* which establishes a websocket connection to the server. A subscription query is based on the exact same syntax as queries and mutations. Here is an example to subscribe to the event of a new `Post` being created. With this subscription query, the server will push the `title` as well as the `author`'s `name` to the client:

```graphql
subscription {
  Post(filter: {
    operation_in_in: [CREATED]
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

> If you want to learn more about subscription queries and why the selection set starts with the `node` keyword, you can read the [reference documentation](http:/#) for subscriptions.

![](https://imgur.com/oE2mF7D.png)
