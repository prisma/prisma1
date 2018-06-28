---
alias: quohj3yahv 
description: Learn how GraphQL server development works with Prisma.
---

# GraphQL Server Development

## Core to every GraphQL API: The GraphQL schema

### The types in your schema define the API operations

At the core of every GraphQL API there is *GraphQL schema* that clearly defines all available API operations and data types. The schema is written using a dedicated syntax called _Schema Definition Language_ (SDL). SDL is simple, concise and straightforward to use.

Here is an example demonstrating how to define a simple type `User` that has two fields, `id` and `name`.

```graphql
type User {
  id: ID!
  name: String!
}
```

Every GraphQL schema has three special *root types*, called `Query`, `Mutation` and `Subscription`. The fields on these root types define the operations accepted by the API.

As an example, consider the following `Query` and `Mutation` types:

```graphql
type Query {
  users: [User!]!
}

type Mutation {
  createUser(name: String!): User!
}
```

A GraphQL API that's defined by this schema would allow for the following two operations:

```graphql
# query list of users
query {
  users {
    id
    name
  }
}

# create new user
mutation {
  createUser(name: "Sarah") {
    id
  }
}
```

The collection of all fields and their arguments inside a query/mutation is called the *selection set* of the operation.

### Root fields define the entry-points for the API

The fields on the root types are also called *root fields* and provide the *entry-points* for the API. This means a query or mutation that's sent to the API always needs to start with one of the root fields.

The *type* of the root field determines which fields can be further included in the query's selection set. In the case of the above example, the types are `User` and `[User!]!` which in both cases allows to include any fields of the `User` type.

If the root field had a [scalar](http://graphql.org/learn/schema/#scalar-types) type, it wouldn't be possible to include any further fields in the selection set. As an example, consider the following GraphQL schema:

```graphql
type Query {
  hello: String!
}
```

A GraphQL API defined by this query only accepts a single operation:

```graphql
query {
  hello
}
```

## Resolver functions implement the schema

### Structure vs behaviour in GraphQL Servers

GraphQL has a clear separation of *structure* and *behaviour*. While the SDL schema definition only describes the *abstract structure* of the API, the *concrete implementation* is achieved by means of so-called *resolver* functions. The combination of both the schema definition and resolver implementations is often referred to as an *executable schema*.

Every field in the GraphQL schema is backed by one resolver function, meaning there are precisely as many resolver functions as fields in the GraphQL schema (this also includes fields on types other than root types).

The resolver function for a field is responsible for fetching the data for precisely that field. For example, the resolver for the `users` root field above knows how to fetch a list of users.

The GraphQL query resolution process therefore merely becomes an action of invoking the resolver functions for the fields contained in the query, because each resolver returns the data for its field.

### Anatomy of a Resolver Function

A resolver function always takes four arguments (in the following order):

1. `parent` (also sometimes called `root`): Queries are resolved by the GraphQL engine which invokes the resolvers for the fields contained in the query. Because queries can contain *nested* fields, there might be multiple *levels* of resolver execution. The `parent` argument always represents the return value from the *previous* resolver call. See [here](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e#9d03) for more info.
2. `args`: Potential arguments that were provided for that field (e.g. the `name` of the `User` in the example of the `createUser` mutation above).
3. `context`: An object that gets passed through the resolver chain that each resolver can write to and read from (basically a means for resolvers to communicate and share information).
4. `info`: An AST representation of the query or mutation. You can read more about in details in this article: [Demystifying the `info` Argument in GraphQL Resolvers](https://blog.graph.cool/graphql-server-basics-demystifying-the-info-argument-in-graphql-resolvers-6f26249f613a).

Here is a possible way how we could implement resolvers for the above schema definition (the implementation assumes there's some global object `db` that provides an interface to a database):

```js
const Query = {
  users: (parent, args, context, info) => {
    return db.users()
  }
}

const Mutation = {
  createUser: (parent, args, context, info) => {
    return db.createUser(args.name)
  }
}

const User = {
  id: (parent, args, context, info) => parent.id,
  name: (parent, args, context, info) => parent.name,
}
```

The sample schema definition from above has exactly four fields. This resolver implementation now provides the four corresponding resolver functions. Notice that the resolvers for the `User` type can actually be omitted since their implementation is trivial and is inferred by the GraphQL execution engine.

## How Prisma helps with GraphQL server development

### The difficult part in building GraphQL servers is implementing resolvers

As you can see above, the main development tasks when implementing GraphQL servers circulate around defining a schema and implementing the corresponding resolver functions. This is also referred to as *schema-driven development*.

When it comes to implementing resolver functions, you need to hit some kind of data source to fetch the data that should be part of the query response. This data source can be anything - it might be a database (SQL or NoSQL), a REST API, some 3rd party service or any sort of legacy system.

The above example made it easy and assumed the availability of a global `db` object that provides a simple interface to some data source. In the real world, you're likely to encounter a lot more complicated scenarios. Particularly because GraphQL queries can be deeply nested, translating them to SQL (or other DB APIs) is cumbersome and error-prone.

### Prisma makes your resolvers simple and straightforward

When using Prisma, the general idea is that your resolvers merely *delegate* the execution of incoming queries to the underlying Prisma engine instead of hitting a database directly. Most of your resolver implementations therefore will be simple one-liners. The heavy-lifting of translating incoming queries to your database API is then done by Prisma.

The way how this works is by using *GraphQL bindings*. These allow to interact with your Prisma GraphQL API by invoking dedicated functions in JavaScript (or any other programming language you use for your backend development). Using this approach, the resolver implementation becomes equally simple as the fake `db` example above.

## GraphQL bindings - The better ORM

### Bindings allow to send queries and mutations by invoking functions in your programming language

GraphQL bindings, to some extent, can be compared to traditional ORMs. They basically allow to talk to a GraphQL API by invoking functions in your programming language rather than constructing raw query strings that you send to the API directly.

Consider the `createUser` mutation from above. Whenever you want to send it to a GraphQL API, you need to spell out the entire mutation like so:

```graphql
mutation {
  createUser(name: "Sarah") {
    id
  }
}
```

Then you're putting this string into the *body* of HTTP POST request (at least that's common for the majority of GraphQL server implementations) and send it over to the API. One major drawback in this scenario is that the query is represented as a *string*. This removes one of the core advantages of GraphQL: Its strong type system! The fact that the API communication layer is actually strongly typed is not leveraged at all with this string-based approach.

GraphQL bindings change that by allowing you to invoke dedicated functions in order to send queries and mutations to the API, rather than constructing strings and sending them to the server via HTTP manually. These functions are named after the *root fields* of your GraphQL schema.

With bindings, the above `createUser` mutation could be sent to the server by invoking a function of the same name:

```js
binding.mutation.createUser({ name: "Sarah" }, '{ id }')
```

In the same way, you could translate the `users` query from above to a function call:

```js
binding.query.users({}, '{ id name }')
```

As you might see, the first parameter in these function calls is an object that carries the *arguments* for the query/mutation, the second one is the *selection set* that determines what data should be contained in the response.

When invoking these methods, the `binding` instance under the hood will take care of translating the operations into a GraphQL query, sending the query to the server and making the response available as an object in your programming language.

### Static vs Dynamic Bindings

Bindings can be used in two flavors: *static* and *dynamic*.

**Static bindings** are used when interacting with GraphQL APIs from statically and strongly typed programming languages (like TypeScript or Scala) where the types of all expressions need to be known at compile-time. In that scenario, the binding functions are generated at build-time (using code generation). Therefore, all invocations of these binding functions can be validated by the compiler and typos as well as structural errors (like passing arguments of the wrong type) are caught at compile-time.

Another advantage is that your editor can now help you with making API requests, e.g. with auto-completion for the available operations and query arguments! This is a game changer for backend development and brings the developer experience to a new level. No SQL strings or other brittle DB APIs any more - thanks to Prisma bindings you're using a strongly typed layer to interact with your database!

**Dynamic bindings** are commonly used in dynamic programming languages (such as JavaScript). They don't require an additional build step (as is the case for static bindings). The method invocations on the `binding` instance are translated to GraphQL queries only at runtime. This still provides the major benefit of using the concise and simple binding syntax. Benefits like build-time error checking and auto-completion can still be achieved using appropriate build tools.

## Architecture primer: Two GraphQL API layers

When building GraphQL servers with Prisma, you're dealing with two GraphQL APIs:

* the **database layer** which is taken care of by Prisma
* the **application layer** responsible for any functionality that's not directly related to writing or reading data from the database (like business logic, authentication and permissions, 3rd-party integrations,...)

The database layer is entirely configured through `prisma.yml` and managed with the Prisma CLI. The application layer is the GraphQL server that you're implementing yourself in your favorite programming language. You can read more about this in the next section.
