---
alias: thohp1zaih
description: Learn how to implement permissions and access rights with Prisma and graphql-yoga
---

# Permissions

In this tutorial, you'll learn how to implement permissions rules when building a GraphQL server with Prisma and `graphql-yoga`.

For the purpose of this tutorial, you'll use the [`node-advanced`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/advanced) GraphQL boilerplate project (which already comes with out-of-the-box authentication) to get started. You'll then gradually adjust the existing resolvers to account for the permission requirements of the API. Let's jump right in!

## Bootstrapping the GraphQL server

Before you can bootstrap the GraphQL server with `graphql create`, you need to install the [GraphQL CLI](https://github.com/graphql-cli/graphql-cli/).

<Instruction>

Open your terminal and install the GraphQL CLI with the following command:

```sh
npm install -g graphql-cli
```

</Instruction>

 > **Note**: For the purpose of this tutorial you don't explicitly have to install the Prisma CLI because `prisma` is listed as a _development dependency_ in the `node-advanced` boilerplate, which allows to run its commands by prefixing it with `yarn`, e.g. `yarn prisma deploy` or `yarn prisma playground`.
 > If you have `prisma` installed globally on your machine (which you can do with `npm install -g prisma`), you don't need to use the `yarn` prefix throughout this tutorial.

Once the CLI is installed, you can create your GraphQL server.

<Instruction>

In your terminal, navigate to a directory of your choice and run the following command:

```sh
graphql create permissions-example --boilerplate node-advanced
```

</Instruction>

<Instruction>

When prompted where (i.e. to which _cluster_) to deploy your Prisma service, choose one of the _public cluster_ options: `prisma-eu1` or `prisma-us1`.

</Instruction>

> **Note**: You can also deploy the Prisma service locally, this however requires you to have [Docker](https://www.docker.com) installed on your machine. For the purpose of this tutorial, we'll go with a public demo cluster to keep things simple and straightforward .

This will create a new directory called `permissions-example` where it places the source files for the GraphQL server (based on `graphql-yoga`) and the required configuration for the belonging Prisma database service.

The GraphQL server is based on the following data model:

```graphql
type Post {
  id: ID! @unique
  createdAt: DateTime!
  updatedAt: DateTime!
  isPublished: Boolean! @default(value: "false")
  title: String!
  text: String!
  author: User!
}

type User {
  id: ID! @unique
  email: String! @unique
  password: String!
  name: String!
  posts: [Post!]!
}
```

## Adding an `ADMIN` role to the app

In this tutorial, a `User` can be either an admin (with special access rights) or a simple customer. To distinguish these types of users, you need to make a modification to the data model and add an enum that defines these roles.

<Instruction>

Open `database/datamodel.graphql` and update the `User` type in the data model to look as follows, note that you also need to add the `Role` enum:

```graphql
type User {
  id: ID! @unique
  email: String! @unique
  password: String!
  name: String!
  posts: [Post!]!
  role: Role! @default(value: "CUSTOMER")
}

enum Role {
  ADMIN
  CUSTOMER
}
```

</Instruction>

Note that the `role` field is not exposed through the API of your GraphQL server (just like the `password` field) because the `User` type defined in the application schema does not have it. The application schema ultimately defines what data will be exposed to your client applications.

To apply the changes, you need to deploy the database.

<Instruction>

In the `permissions-example` directory, run the following command:

```sh
yarn prisma deploy
```

</Instruction>

Now your data model and the Prisma API are updated and include the `role` field for the `User` type.

## Defining permission requirements

The **application schema** defined in `src/schema.graphql` exposes the following queries and mutations:

```graphql
type Query {
  feed: [Post!]!
  drafts: [Post!]!
  post(id: ID!): Post!
  me: User
}

type Mutation {
  signup(email: String!, password: String!, name: String!): AuthPayload!
  login(email: String!, password: String!): AuthPayload!
  createDraft(title: String!, text: String!): Post!
  publish(id: ID!): Post!
  deletePost(id: ID!): Post!
}
```

At the moment, we're only interested in the resolvers that relate to the `Post` type. Here is an overview of the permission requirements we have for them:

- `feed`: No permissions requirements. Everyone (not only authenticated users) should be able to access the `feed` of published `Post` nodes.
- `drafts`: Every user should only be able to access their own drafts (i.e. where they're set as the `author` of the `Post`).
- `post`: Only the `author` of a `Post` or an `ADMIN` user should be able to access `Post` nodes using the `post` query.
- `publish`: Only the `author` of a `Post` should be able to publish it.
- `deletePost`: Only the `author` of a `Post` node or an `ADMIN` user should be able to delete it.

## Implementing permissions rules with `graphql-yoga` and Prisma

When implementing permission rules with Prisma and `graphql-yoga`, the basic idea is to implement a "data access check" in each resolver. Only if that check succeeds, the operation (query, mutation or subscription) is forwarded to the Prisma service using the available `prisma-binding`.

You're now going to gradually add these checks to the existing resolvers.

### `feed`

Since everyone is able to access the `feed` query, no check needs to be implemented here.

### `drafts`

For the `drafts` query, we have the following requirement:

> Every user should only be able to access their own drafts (i.e. where they're set as the `author` of the `Post`

Currently, the `drafts` resolver is implemented as follows:

```js
drafts(parent, args, ctx, info) {
  const id = getUserId(ctx)

  const where = {
    isPublished: false,
    author: {
      id
    }
  }

  return ctx.db.query.posts({ where }, info)
},
```

In fact, this already accounts for the requirement because it filters the `posts` and only retrieves the one for the authenticated `User`. So, there's nothing to do for you here.

### `post`

For the `post` query, we have the following requirement:

> Only the `author` of a `Post` or an `ADMIN` user should be able to access `Post` nodes using the `post` query.

Here is how the `post` resolver is currently implemented:

```js
post(parent, { id }, ctx, info) {
  return ctx.db.query.post({ where: { id } }, info)
}
```

It's very simple and straightforward! But now, you need to make sure that it only returns a `Post` if the `User` that sent the request is either the `author` of it _or_ and `ADMIN` user.

You'll use the `exists` function of the `prisma-binding` package for that.

<Instruction>

Update the implementation of the resolver in `src/resolvers/Query.js` as follows:

```js
async post(parent, { id }, ctx, info) {
  const userId = getUserId(ctx)
  const requestingUserIsAuthor = await ctx.db.exists.Post({
    id,
    author: {
      id: userId,
    },
  })
  const requestingUserIsAdmin = await ctx.db.exists.User({
    id: userId,
    role: 'ADMIN',
  })

  if (requestingUserIsAdmin || requestingUserIsAuthor) {
    return ctx.db.query.post({ where: { id } }, info)
  }
  throw new Error(
    'Invalid permissions, you must be an admin or the author of this post to retrieve it.',
  )

}
```

</Instruction>

With the two `exists` invocations, you gather information as to whether:

- the `User` who sent the request is in fact the `author` of the `Post` that was requested
- the `User` who sent the request is an `ADMIN`

If either of these conditions is true, you simply return the `Post`, otherwise you return an insufficient permissions error.

### `publish`

The `publish` mutation has the following requirement:

> Only the `author` of a `Post` should be able to publish it.

The `publish` resolver is implemented in  `src/resolvers/Mutation/post.js` and currently looks as follows:

```js
async publish(parent, { id }, ctx, info) {
  const userId = getUserId(ctx)
  const postExists = await ctx.db.exists.Post({
    id,
    author: { id: userId },
  })
  if (!postExists) {
    throw new Error(`Post not found or you're not the author`)
  }

  return ctx.db.mutation.updatePost(
    {
      where: { id },
      data: { isPublished: true },
    },
    info,
  )
},
```

The current `exists` invocation already ensures that the `User` who send the request is set as the `author` of the `Post` to be published. So again, you don't actually have to make any changes and the requirement is already taken care of.

### `deletePost`

The `deletePost` mutation has the following requirement:

> Only the `author` of a `Post` node or an `ADMIN` user should be able to delete it.

The current resolver is implemented in `src/resolvers/Mutation/post.js` and looks as follows:

```js
async deletePost(parent, { id }, ctx, info) {
  const userId = getUserId(ctx)
  const postExists = await ctx.db.exists.Post({
    id,
    author: { id: userId },
  })
  if (!postExists) {
    throw new Error(`Post not found or you're not the author`)
  }

  return ctx.db.mutation.deletePost({ where: { id } })
},
```

Again, the `exists` invocation already ensures that the requesting `User` is the `author` of the `Post` to be deleted. However, if that `User` is an `ADMIN`, the `Post` should still be deleted.

<Instruction>

Adjust the `deletePost` resolver in `src/resolvers/Mutation/post.js` to look as follows:

```js
async deletePost(parent, { id }, ctx, info) {
  const userId = getUserId(ctx)
  const postExists = await ctx.db.exists.Post({
    id,
    author: { id: userId },
  })

  const requestingUserIsAdmin = await ctx.db.exists.User({
    id: userId,
    role: 'ADMIN',
  })

  if (!postExists && !requestingUserIsAdmin) {
    throw new Error(`Post not found or you don't have access rights to delete it.`)
  }

  return ctx.db.mutation.deletePost({ where: { id } })
},
```

</Instruction>

## Testing permissions

You can the permissions inside a GraphQL Playground. Here's the general flow:

- In the Playground, create a new `User` with the `signup` mutation and specify the `token` in the selection set so it's returned by the server (if you already created a `User` before, you can of course also use the `login` mutation)
- Save the `token` from the server's response and set it as the `Authorization` header in the Playground (you'll learn how to do this in a bit)
- All subsequent requests are now sent _on behalf_ of that `User`

### 1. Creating a new `User`

You first need to open a GraphQL Playground, but before you can do that you need to start the server!

<Instruction>

In the `permissions-example` directory, run the following command in your terminal:

```sh
yarn start
```

</Instruction>

The server is now running on [`http://localhost:4000`](http://localhost:4000).

<Instruction>

Open [`http://localhost:4000`](http://localhost:4000) in your browser.

</Instruction>

<Instruction>

In the **default** Playground in the **app** section, send the following mutation:

```graphql
mutation {
  signup(
    email: "sarah@graph.cool"
    password: "graphql"
    name: "Sarah"
  ) {
    token
  }
}
```

</Instruction>

![](https://imgur.com/gbnUlkr.png)

<Instruction>

Copy the `token` and set it as the `Authorization` header in the bottom-left corner of the Playground. You need to set the header as JSON as follows (note that you need to replace the `__TOKEN__` placeholder with the authentication `token` that was returned from the `signup` mutation):

```json
{
  "Authorization": "__TOKEN__"
}
```

</Instruction>

From now on, all requests sent through the Playground are sent _on behalf_ of the `User` you just created.

Equipped with that knowledge, you can now play around with the available queries and mutations nd verify if the permission rules work.

For example, you can go through the following flow:

1. Create a new _draft_ with the `createDraft` mutation on behalf `Sarah` (the `User` you just created).
1. Create another `User` with the `signup` mutation and ask for a `token` for them.
1. Use the new authentication token to try and publish Sarah's draft. This should return the following error: `Post not found or you're not the author`.

![](https://imgur.com/hZSC1gy.png)
