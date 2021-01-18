---
alias: ien5es6ok3
description: Learn how to add realtime functionality to your GraphQL server using subscriptions.
---

# Build a Realtime GraphQL Server with Subscriptions

> The finished project of this tutorial can be found on [GitHub](https://github.com/nikolasburk/subscriptions).

## Overview

### Subscriptions allow clients to receive event-based realtime updates

One convenient property of GraphQL subscriptions is that they’re using the exact same syntax as queries and mutations. From a client perspective, this means there’s nothing new to learn to benefit from this feature.

The major difference between subscriptions and queries/mutations lies in the *execution*. While queries and mutations follow typical request-response cycles (just like regular HTTP requests), subscriptions don’t return the requested data right away. Instead, when a GraphQL server receives a subscription request, it creates a *long-lived connection* to the client which sent the request.

With that request, the client expressed interest in data that’s related to a specific *event*, for example a specific user liking a picture. The corresponding subscription might look like this:

```graphql
subscription($userId: ID!) {
  likeCreated(userId: $userId) {
    user {
      name
    }
    picture {
      url
    }
  }
}
```

When the user in question now likes a picture, the server pushes the requested data to the subscribed client via their connection:

```json
{
  "data": {
    "likeCreated": {
      "user": {
        "name": "Alice"
      },
      "picture": {
        "url": "https://media.giphy.com/media/5r5J4JD9miis/giphy.gif"
      }
    }
  }
}
```

### Implementing subscriptions with WebSockets

Subscriptions are commonly implemented with [WebSockets](https://en.wikipedia.org/wiki/WebSocket). Apart from the realtime logic (which is typically handled via pub/sub-systems), you need to implement the official [communication protocol](https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md) for GraphQL subscriptions. Only if your server follows the flow defined in the protocol, clients will be able to properly initiate requests and receive event data.

Dealing with realtime logic and pub/sub-systems, properly accessing databases and taking care of implementing the subscription protocol can become fairly complex. Authentication and authorization logic further complicate the implementation of GraphQL subscriptions on the server. In these cases, it’s helpful to use proper abstractions that make your life easier.

One such abstraction is provided by [Prisma](https://www.prisma.io) in combination with [Prisma bindings](https://github.com/prisma/prisma-binding). Think of that combo as a [“GraphQL ORM”](https://github.com/prisma/prisma#is-prisma-an-orm) layer where realtime subscriptions are supported out-of-the-box, making it easy for you to add subscriptions to your API.

## 1. Project setup

### 1.1. Download and explore the starter project

The first step in this tutorial is to get access to the starter project. If you don’t want to actually follow the tutorial but are only interested in what the subscription code looks like, feel free to [skip ahead](#2-understanding-prismas-subscription-api).

<Instruction>

You can download the starter project from [this](https://github.com/nikolasburk/subscriptions) repository using the following terminal command. Also, directly install the npm dependencies of the project:

```sh
curl https://codeload.github.com/nikolasburk/subscriptions/tar.gz/starter | tar -xz subscriptions-starter
cd subscriptions-starter
yarn install # or npm install
```

</Instruction>

The project contains a very simple GraphQL API defined by the following schema:

```graphql
# import Post from "./generated/prisma.graphql"

type Query {
  feed: [Post!]!
}

type Mutation {
  writePost(title: String!): Post
  updateTitle(id: ID!, newTitle: String!): Post
  deletePost(id: ID!): Post
}
```

The `Post` type is defined via the [Prisma data model](https://github.com/nikolasburk/subscriptions/blob/starter/database/datamodel.graphql) and looks as follows:

```graphql
type Post {
  id: ID! @unique
  title: String!
}
```

The goal for this project will be to add two subscriptions to the API:

* A subscription that fires when a new `Post` is *created* or the `title` of an existing `Post` is *updated*.
* A subscription that fires when an existing `Post` is *deleted*.

### 1.2. Deploy the Prisma database API

Before starting the server, you need to ensure the Prisma database API is available and can be accessed by your GraphQL server (via Prisma bindings).

<Instruction>

To deploy the Prisma API, run the `yarn prisma1 deploy` command inside the `subscriptions-starter` directory.

The CLI will then prompt you with a few questions regarding *how* you want to deploy the API. For the purpose of this tutorial, choose the **Demo server** options, then simply hit **Enter** to select the suggested values for the *service name* and *stage*. (Note that if you have [Docker](https://www.docker.com) installed, you can also deploy *locally*).

</Instruction>

Once the API is deployed, the CLI prints the `HTTP endpoint` for the Prisma database API.

<Instruction>

Copy that endpoint and paste it into `index.js` where your `GraphQLServer` is instantiated. Note that you need to *replace* the current placeholder `__PRISMA_ENDPOINT__`.

</Instruction>

After you did this, the code will look similar to this:

```js
const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',
      endpoint: 'https://eu1.prisma.sh/jane-doe/subscriptions-example/dev',
      secret: 'mysecret123',
      debug: true,
    }),
  }),
})
```

### 1.3. Open a GraphQL Playground

You can now start the server and open up a [GraphQL Playground](https://github.com/prismagraphql/graphql-playground) by running the `yarn dev` command:

![](https://cdn-images-1.medium.com/max/5308/1*GoxKn35GhQpmHZ6bnKbDZA.png)

Feel free to explore the project and send a few queries and mutations.

> **Note:** The Playground shows you the two GraphQL APIs which are defined in [`.graphqlconfig.yml`](https://github.com/nikolasburk/subscriptions/blob/master/.graphqlconfig.yml). The **`app`** project represents the **application layer** and is defined by the GraphQL schema in `/src/schema.graphql`. The **`database`** project represents your **database layer** and is defined by the auto-generated Prisma GraphQL schema in `/src/generated/prisma.graphql`.
> **Learn more:** To learn more about this architecture, be sure to check out the corresponding [documentation](https://www.prisma.io/docs/reference/introduction/architecture-jaeraegh6e). For an in-depth learning experience, follow the [Node tutorial on How to GraphQL](https://www.howtographql.com/graphql-js/0-introduction/)

## 2. Understanding Prisma’s subscription API

### 2.1. Overview

Before starting to implement the subscriptions, let’s take a brief moment to understand the subscription API provided by Prisma since that’s the API you’ll be piggybacking with Prisma bindings.

In general, Prisma lets you *subscribe* to three different kinds of events (per type in your data model). Taking the `Post` type from this tutorial project as an example, these events are:

* a new `Post` is *created*
* an existing `Post` is *updated*
* an existing `Post` is *deleted*

The corresponding definition of the `Subscription` type looks as follows (this definition can be found in `/src/generated/prisma.graphql`):

```graphql
type Subscription {
  post(where: PostSubscriptionWhereInput): PostSubscriptionPayload
}
```

If not further constrained through the `where` argument, the `post` subscription will fire for all of the events mentioned above.

### 2.2. Filtering for specific events

The `where` argument allows clients to specify exactly what events they’re interested in. Maybe a client always only wants to receive updates when a `Post` gets *deleted* or when a `Post` where the `title` contains a specific keyword is *created*. These kinds of constraints can be expressed using the `where` argument.

The type of `where` is defined as follows:

```graphql
input PostSubscriptionWhereInput {
  # Filter for a specific mutation:
  # CREATED, UPDATED, DELETED
  mutation_in: [MutationType!]

  # Filter for a specific field being updated
  updatedFields_contains: String
  updatedFields_contains_every: [String!]
  updatedFields_contains_some: [String!]

  # Filter for concrete values of the Post being mutated
  node: PostWhereInput

  # Combine several filter conditions
  AND: [PostSubscriptionWhereInput!]
  OR: [PostSubscriptionWhereInput!]
}
```

The two examples mentioned above could be expressed with the following subscriptions in the Prisma API:

```graphql
# Only fire for _deleted_ posts
subscription {
  post(where: {
    mutation_in: [DELETED]
  }) {
    # ... we'll talk about the selection set in a bit
  }
}

# Only fire when a post whose title contains "GraphQL" is _created_
subscription {
  post(where: {
    mutation_in: [CREATED]
    node: {
      title_contains: "GraphQL"
    }
  }) {
    # ... we'll talk about the selection set in a bit
  }
}
```

### 2.3. Exploring the selection set of a subscription

You now have a good understanding how you can subscribe to the events that interest you. But how can you now ask for the data related to an event?

The `PostSubscriptionPayload` type defines the fields which you can request in a `post` subscription. Here is how that type is defined:

```graphql
type PostSubscriptionPayload {
  mutation: MutationType!
  node: Post
  updatedFields: [String!]
  previousValues: PostPreviousValues
}
```

Let’s discuss each of these fields in a bit more detail.

##### 2.3.1 `mutation: MutationType!`

`MutationType` is an `enum` with three values:

```graphql
enum MutationType {
  CREATED
  UPDATED
  DELETED
}
```

The `mutation` field on the `PostSubscriptionPayload` type therefore carries the information what *kind* of mutation happened.

##### 2.3.2 `node: Post`

This field represents the `Post` element which was *created*, *updated* or *deleted* and allows to retrieve further information about it.

Notice that for `DELETED`-mutations, `node` will always be `null`. If you need to know more details about the `Post` that was deleted, you can use the `previousValues` field instead (more about that soon).

> **Note**: The terminology of a **node** is sometimes used in GraphQL to refer to single elements. A node essentially corresponds to a **record** in the database.

##### 2.3.3 `updatedFields: [String!]`

One piece of information you might be interested in for `UPDATED`-mutations is which *fields* have been updated with a mutation. That’s what the `updatedFields` field is used for.

Assume a client has subscribed to the Prisma API with the following subscription:

```graphql
subscription {
  post {
    updatedFields
  }
}
```

Now, assume the server receives the following mutation to update the `title` of a given `Post`:

```graphql
mutation {
  updatePost(
    where: {
      id: "..."
    }
    data: {
      title: "Prisma is the best way to build GraphQL servers"
    }
  ) {
    id
  }
}
```

The subscribed client will then receive the following payload:

```json
{
  "data": {
    "post": {
      "updatedFields": ["title"]
    }
  }
}
```

This is because the mutation only updated the `Post`'s `title` field - nothing else.

##### 2.3.4 `previousValues: PostPreviousValues`

The `PostPreviousValues` type looks very similar to `Post` itself:

```graphql
type PostPreviousValues {
  id: ID!
  title: String!
}
```

It basically is a *helper* type that simply mirrors the fields from `Post`.

`previousValues` is only used for `UPDATED`- and `DELETED`-mutations. For `CREATED`-mutations, it will always be `null` (for the same reason that node is `null` for `DELETED`-mutations).

##### 2.3.5 Putting everything together

Consider again the sample `updatePost`-mutation from the section **2.3.3**. But let’s now assume, the subscription query includes *all* the fields we just discussed:

```graphql
subscription {
  post {
    mutation
    updatedFields
    node {
      title
    }
    previousValues {
      title
    }
  }
}
```

Here’s what the payload will look like that the server pushes to the client after it performed the mutation from before:

```json
{
  "data": {
    "post": {
      "mutation": "UPDATED",
      "updatedFields": ["title"],
      "node": {
        "title": "Prisma is the best way to build GraphQL servers",
      },
      "previousValues": {
        "title": "GraphQL servers are best built with conventional ORMs",
      }
    }
  }
}
```

Note that this assumes the updated `Post` had the following `title` before the mutation was performed: `“GraphQL servers are best built with conventional ORMs”`.

## 3. Add the `publication` subscription

Equipped with the knowledge about the Prisma’s subscription API, you’re now ready to consume precisely that API to implement your own subscriptions on the application layer. Let’s start with the subscription that should fire when a new `Post` is *created* or the `title` of an existing `Post` is *updated*.

### 3.1. Extend the application schema

The first step is to extend the GraphQL schema of your application layer and add the corresponding subscription definition.

<Instruction>

Open `schema.graphql` and add the following `Subscription` type to it:

```graphql
type Subscription {
  publications: PostSubscriptionPayload
}
```

</Instruction>

The referenced `PostSubscriptionPayload` is directly taken from the Prisma GraphQL schema.

<Instruction>

It thus also needs to be imported at the top of the file:

```graphql
# import Post, PostSubscriptionPayload from "./generated/prisma.graphql"
```

</Instruction>

> **Note:** The comment-based import syntax is used by the `[graphql-import](https://github.com/prismagraphql/graphql-import)` package. As of today, GraphQL SDL does not have an official way to import types across files. [This might change soon](https://github.com/graphql/graphql-wg/blob/master/notes/2018-02-01.md#present-graphql-import).

### 3.2. Implement the subscription resolver

Similar to queries and mutations, the next step when adding a new API feature is to implement the corresponding *resolver*. Resolvers for subscriptions however look a bit different.

Instead of providing only a single resolver function to resolve a subscription operation from your schema definition, you provide an *object* with at least one field called `subscribe`. This `subscribe` field is a function that returns an [`AsyncIterator`](https://jakearchibald.com/2017/async-iterators-and-generators/). That `AsyncIterator` is used to return the values for each individual event. Additionally, you might provide another field called `resolve` that we'll discuss in the next section — for now let’s focus on `subscribe`.

<Instruction>

Update the resolvers object in `index.js` to now also include `Subscription`:

```js
const resolvers = {
  Query: {
    // ... like before
  },
  Mutation: {
    // ... like before
  },
  Subscription: {
    publications: {
      subscribe: (parent, args, ctx, info) => {
        return ctx.db.subscription.post(
          {
            where: {
              mutation_in: ['CREATED', 'UPDATED'],
            },
          },
          info,
        )
      },
    },
  },
}
```

</Instruction>

Prisma bindings are doing the hard work for you here since `db.subscription.post(...)` returns the `AsyncIterator` that emits a new value upon every event on the `Post` type.

Note that you’re specifically filtering for `CREATED`- and `UPDATED`-mutations to ensure the `publications` subscription only fires for those events.

### 3.3. Test the subscription

For testing the subscription, you need to start the server and open up a Playground which you can do by running `yarn dev` in your terminal.

In the Playground that opened, run the following subscription:

```graphql
subscription {
  publications {
    node {
      id
      title
    }
  }
}
```

> **Note: **The GraphQL Playground sometimes shows this [bug](https://github.com/prismagraphql/graphql-playground/issues/646) where the subscription directly returns a payload of `null`. If this happens to you, try this [workaround](https://github.com/prismagraphql/graphql-playground/issues/646#issuecomment-382614189).

Once the subscription is running, you'll see a loading indicator in the response pane and the **Play**-button turns into a red **Stop**-button for you to stop the subscription.

![](https://cdn-images-1.medium.com/max/5288/1*9fDpnhWdRrgN-vxB3HmLeQ.png)

You can now open another tab and send a mutation to trigger the subscription:

```graphql
mutation {
  writePost(title: "GraphQL subscriptions are awesome") {
    id
  }
}
```

Navigating back to the initial tab, you’ll see that the subscription data now appeared in the response pane 🙌

![](https://cdn-images-1.medium.com/max/5288/1*c6bexq2J0bTeT5UioGjl_A.png)

Feel free to play around with the `updateTitle` mutation as well.

## 4. Add the `postDeleted` subscription

In this section, you’ll implement a subscription that fires whenever a `Post` gets *deleted*. The process will be largely similar to the `publications` resolver, except that you’re now going to return just the deleted `Post` instead of an object of type `PostSubscriptionPayload`.

### 4.1. Extend the application schema

The first step, as usual when adding new features to a GraphQL API, is to express the new operation as a [*root field*](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e#9e63) in the GraphQL schema.

<Instruction>

Open `/src/schema.graphql` and adjust the `Subscription type to look as follows:

```graphql
type Subscription {
  publications: PostSubscriptionPayload
  postDeleted: Post
}
```

</Instruction>

Instead of returning the `PostSubscriptionPayload` for `postDeleted`, you simply return the `Post` object that was deleted.

### 4.2. Implement the subscription resolver

In section **3.2.**, we briefly mentioned that the object that you use to implement subscription resolvers can hold a second function called `resolve` (next to `subscribe` which is required). In this section, you’re going to use it.

<Instruction>

Here is what the implementations of both `subscribe` and `resolve` look like to resolve the `postDeleted` subscription:

```js
const resolvers = {
  Query: {
    // ... like before
  },
  Mutation: {
    // ... like before
  },
  Subscription: {
    publications: {
      // ... like before
    },
    postDeleted: {
      subscribe: (parent, args, ctx, info) => {
        const selectionSet = `{ previousValues { id title } }`
        return ctx.db.subscription.post(
          {
            where: {
              mutation_in: ['DELETED'],
            },
          },
          selectionSet,
        )
      },
      resolve: (payload, args, context, info) => {
        return payload ? payload.post.previousValues : payload
      },
    },
  },
}
```

</Instruction>

The most important thing to realize about combining the `subscribe` and `resolve` functions is that the values emitted by the `AsyncIterator` (which is returned by `subscribe`) correspond to the `payload` argument that’s passed into `resolve`! This means you can use `resolve` to _transform_ and/or _filter_ the event data emitted by the `AsyncIterator` according to your needs.

Note that in this scenario, you’re also passing a *hardcoded* selection set to the `post` binding function instead of passing the `info` object along as you’re doing most of the time. The invocation of the binding function thus corresponds to the following subscription request against the Prisma API:

```graphql
subscription {
  post {
    previousValues {
      id
      title
    }
  }
}
```

The info object carries the [AST](https://medium.com/@cjoudrey/life-of-a-graphql-query-lexing-parsing-ca7c5045fad8) (and therefore the *selection set*) of the incoming GraphQL operations (queries, mutations and subscriptions alike). In this case however, the incoming selection set can’t be applied to the `post` subscription from the Prisma API. The reasons for that are the following:

* The return type of the incoming subscription is simply `Post` as you defined in `schema.graphql`.
* The return type of the `post` subscription from the Prisma GraphQL API is `PostSubscriptionPayload`.

This means the incoming `info` object does not match the shape that would be required for the `post` subscription. Hence, you’re specifying the selection set for the `post` subscription manually as a string.

> **Note**: This is a bit tricky to understand at first. If you have trouble following right now, be sure to check out [this](https://blog.graph.cool/graphql-server-basics-demystifying-the-info-argument-in-graphql-resolvers-6f26249f613a) technical deep-dive about the `info` object and its role within GraphQL resolvers.

In fact, this situation is not ideal either since for types with many fields, this approach can quickly get out of hand. Also, it might be that the incoming subscription doesn’t request *all* the fields of a type, so you’re *overfetching* at this point. The best solution would be to manually retrieve the requested fields from the `info` object and pass those along to the `post` subscription as described [here](https://github.com/graphql-binding/graphql-binding/issues/85).

In any case, by hardcoding the selection you’re guaranteed that the payload argument for `resolve` has the following structure:

```json
{
  "post": {
    "previousValues": {
      "id": "...",
      "title": "...",
    }
  }
}
```

That’s why inside `resolve` you can simply return `payload.post.previousValues` and what you get is an object that adheres to the structure of the `Post` type 💡 (Note that checking for payload with the *ternary operator* is just a sanity check to ensure it’s not `undefined`, since this might break the subscription.)

### 4.3. Test the subscription

Before testing the new subscription, you need to restart the server to ensure your changes get applied to the API. You can kill the server by pressing **CTRL+C** and then restart it using the `yarn dev` command.

![](https://cdn-images-1.medium.com/max/5248/1*TisP6j41cu4itqI__dDE-w.png)

Once the subscription is running, you can send the following mutation (you need to replace the `__POST_ID__` placeholder with the `id` of an actual `Post` from your database):

```graphql
mutation {
  deletePost(id: "__POST_ID__") {
    id
  }
}
```

Navigating back to the subscription tab, you’ll see that the `id` and `title` have been pushed in the response pane, as requested by the active subscription.

![](https://cdn-images-1.medium.com/max/5248/1*1vMUL8w54yIAxOPVSR0UjQ.png)

## Summary

In this tutorial, you learned how to add realtime subscriptions to a GraphQL API using [Prisma](https://www.prisma.io/) and [Prisma bindings](https://github.com/prisma/prisma-binding).

Similar to implementing queries and mutations with Prisma, you are piggybacking on Prisma’s GraphQL API, leaving the heavy-lifting of database access and pub/sub logic to the powerful Prisma query engine.

If you want to play around with the project yourself, you can check out the final result of the tutorial on [GitHub](https://github.com/nikolasburk/subscriptions).

<iframe src="https://upscri.be/38bbbd/" frameBorder="0"></iframe>
