---
alias: jaeraegh6e 
description: Learn about the architecture of your GraphQL server when using Prisma.
---

# Architecture

## Separation of application & database layer

### Two GraphQL API layers

When building GraphQL servers with Prisma, you're dealing with two GraphQL APIs:

* The **database layer** which provides CRUD and realtime operations for interacting with the database and is entirely taken care of by Prisma.
* The **application layer** responsible for any functionality that's not directly related to writing or reading data from the database (like business logic, authentication and permissions, 3rd-party integrations,...).

The **database layer** is entirely configured through `prisma.yml` and managed with the Prisma CLI. It is your interface to the database which you can now manage with GraphQL instead of SQL or another database API. Note that this interface applies on two levels:

* reading and writing data using GraphQL queries, mutations and subscriptions
* managing the database schema and migrations using GraphQL's concise and intuitive Schema Definition Language (SDL)

The ***application layer*** defines the GraphQL API that's exposed to your clients. You're responsible for defining its schema, implementing its resolvers (which is straightforward if you're using GraphQL bindings to connect to Prisma) and deploying it to the web (e.g. using Now, Heroku or AWS). If you're building your server with JavaScript, the application layer is best to implement with `graphql-yoga` (a simple and flexible GraphQL server based on Express).

> In the rare cases where your backend doesn't require any sort of additional functionality but reading and writing data is all you need, you can also connect to the Prisma GraphQL API directly from the frontend and thus omit the application layer. Keep in mind that this means anyone who has access to the endpoint of your Prisma API will be able to see your entire GraphQL schema.

Another gem about building GraphQL servers that way is that thanks to `graphql-config`, you can interact with both GraphQL APIs side-by-side inside a GraphQL Playground! It's basically like having Postman and Sequel Pro in the same application.

### Prisma is not a database

While Prisma effectively represents the *database layer* in your backend stack, it is not actually a database! It is an *abstraction layer on top of a database* that lets you interact with the database using GraphQL instead of SQL or another database API. Using GraphQL as a database abstraction is the first step towards making GraphQL a universal query language.

This means you're keeping full ownership over your data while having the comfort of drastically simplified workflows around it.

Another core strength of Prisma is that it retains the individual "specializations" of the databases it's abstracting away. A good example for this are time series databases or geodatabases. When using Prisma to interact with these kinds of databases, you're still getting the performance you'd expect while being leveraging GraphQL as simple interface.

### Benfits of multi-layered architectures

Multi-layered architectures are an architectural trend that has emerged over the last years and by now has become a best practice for designing backend infrastructure. The core motivation behind it is a clear separation of concerns between the different layers of your stack.

The layers you find in today's backend architectures go into two directions:

* _Horizontal layers_ effectively correspond to microservices among which the functionality of your backend is split up.
* _Vertical layers_ are responsible for the *data flow* from the persistence layer to the HTTP server. This includes components like databases, ORMs or other data access layers, API gateways, various kinds of web servers and more.

The opposite of a layered architecture is a monolith where the entire backend is one gigantic server application. Note however that even with monoliths, you commonly have some sort of layers (like the database, ORM and HTTP server) - the major difference to layered architectures is that the layers in monoliths often don't have well-defined interfaces which leads to stronger coupling between the layers (which kind of defeats the idea of layered architectures).

In essence, a layered architecture allows to swap out a layer as long as it maintains the same interface as the previous one - only the implementation changes. Layered architectures introduce a lot more flexibility into your stack and make it a lot easier to maintain on the long-run.

## A real-world scenario

> If you're looking for a step-by-step tutorial that guides you through the following example, you can find it [here](https://blog.graph.cool/tutorial-how-to-build-a-graphql-server-with-graphql-yoga-6da86f346e68).

### The application layer

To get a better understanding of the architecture of your GraphQL server when using Prisma, let's a look at a practical example. Assume you're writing the backend for a simple blogging application. You might come up with the following schema:

```graphql
type Query {
  feed: [Post!]!
  post(id: ID!): Post
}

type Mutation {
  createDraft(title: String!): Post!
  publish(id: ID!): Post
  deletePost(id: ID!): Post
}

type Post {
  id: ID!
  title: String!
  published: Boolean!
}
```

This schema defines the API of your application layer. It is called **application schema** and the API it defines will be consumed by your client applications.

As an example, this schema will allow your clients to send the following query and mutation to the API:

```grapgql
query {
  feed {
    id
    title
  }
}

mutation {
  createDraft(title: "I like GraphQL") {
    id
  }
}
```

As a backend developer, it is now your task to implement the *resolver* functions for it. Because your schema has five root fields, you need to implement (at least) five resolvers.

Normally, in these resolvers you would now hit a database (or some other data source). This means you'd have to write SQL queries or use some other database API. When using Prisma, the implementation of your resolvers becomes straightforward because it drastically simplifies the connection from a GraphQL resolver to the actual database - all you need to is *forward* incoming queries to the underlying Prisma engine (which means your resolvers often end up as simple one-liners).

Here is what the implementation could look like:

```js
const resolvers = {
  Query: {
    feed: (parent, args, context, info) => {
      return context.db.query.posts({ where: { published: true } }, info)
    },
    post: (parent, args, context, info) => {
      return context.db.query.post({ where: { id: args.id } }, info)
    },
  },
  Mutation: {
    createDraft: (parent, args, context, info) => {
      return context.db.mutation.createPost(
        {
          data: {
            title: args.title,
            published: false,
          },
        },
        info,
      )
    },
    publish: (parent, args, context, info) => {
      return context.db.mutation.updatePost(
        {
          where: { id: args.id },
          data: { published: true },
        },
        info,
      )
    },
    deletePost: (parent, args, context, info) => {
      return context.db.mutation.deletePost({ where: { id: args.id } }, info)
    },
  },
}
```

Thanks to the Prisma bindings, the implementation of each resolver is almost trivial. But what is that `context.db` thing that gives you access to the Prisma API? Part of the answer is that `context` is one of the four standard resolver arguments. It is simply an object that each resolver in the resolver chain can write to and read from, so it is basically a means for resolvers to communicate. So, where does its `db` property come from?

To answer this question, let's actually see how the definition of the application schema and the resolver implementations tie together when using `graphql-yoga` as your GraphQL server. Assume the above schema definition is stored in a file called `schema.graphql`. You're then instantiating and starting the server as follows:

```js
const server = new GraphQLServer({
  typeDefs: './schema.graphql', // reference to the application schema
  resolvers,                    // the resolver implementations from above
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: prismaSchema,
      endpoint: prismaEndpoint,
      secret: prismaSecret,
    }),
  }),
})

server.start()
```

When instantiating the `GraphQLServer`, you can set an initial value for the `context`. In this case, you're attaching a `db` property to it which gets initialized with a `Prisma` binding instance. This `Prisma` instance is the interface to the Prisma API which then allows your resolvers to conveniently forward the incoming queries to Prisma by invoking the dedicated binding functions. When invoking these functions, the binding instance will assemble the corresponding GraphQL query under the hood and send it to Prisma via HTTP.

### The database layer

You now have a solid understanding of how the application layer is implemented. You define a GraphQL schema and implement its resolvers. The resolver implementation is straightforward since you're using Prisma bindings. This allows to simply *delegate* the execution of incoming queries to Prisma where the heavy-lifting of the query resolution is done (i.e. reading/writing from/to the database). For that approach to work you obviously need a Prisma service to be available, so how do you get there?

Every Prisma service starts with two files:

* A service configuration file called `prisma.yml`
* The definition of a data model (commonly in a file called `datamodel.graphql`)

The minimal version of `prisma.yml` to generate a Prisma API that works with the above example looks as follows:

```yml
# the name for the service
# (will be part of the service's HTTP endpoint)
service: blogr

# the cluster and stage the service is deployed to;
# you can choose any string for that
# (will be also part of the service's HTTP endpoint)
stage: dev

# protects your Prisma API;
# this is the value for the 'secret' argument when
# instantiating the 'Prisma' binding instance
secret: mysecret123

# the file path pointing to your data model
datamodel: datamodel.graphql
```

The corresponding `datamodel.graphql` could look as follows:

```graphql
type Post {
  id: ID! @unique
  title: String!
  published: Boolean!
}
```

Notice that despite using SDL, this file is not a proper GraphQL schema! It's missing the *root types* which would define the actual API operations - `datamodel.graphql` only contains the definition for a type in the _data model_. This data model is used as the foundation to generate the Prisma API.

With these two files in place, you're ready to deploy a Prisma service - all you need for that is to run `prisma deploy` from the directory that contains these files. The Prisma API that you now have access to provides CRUD and realtime operations for the `Post` type. Here is what the generated GraphQL schema looks like:

```graphql
type Query {
  posts(where: PostWhereInput, orderBy: PostOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [Post]!
  post(where: PostWhereUniqueInput!): Post
}

type Mutation {
  createPost(data: PostCreateInput!): Post!
  updatePost(data: PostUpdateInput!, where: PostWhereUniqueInput!): Post
  deletePost(where: PostWhereUniqueInput!): Post
}

type Subscription {
  post(where: PostSubscriptionWhereInput): PostSubscriptionPayload
}

type Post implements Node {
  id: ID!
  title: String!
  published: Boolean
}
```

Note that this is a simplified version of the schema, the `Input` and `Payload` types have been omitted for brevity. If you're curious, you can find the full schema [here](https://gist.github.com/nikolasburk/c0bbe8df73b78eefddc5240fa1a65f3e).

The Prisma API offers CRUD operations, this means you can now create, read, update and delete `Post` elements by sending corresponding queries and mutations. This API is wrapped on the application layer using Prisma bindings.