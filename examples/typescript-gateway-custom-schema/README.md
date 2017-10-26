# API Gateway (Custom Schema)

## Overview

This directory contains an example implementation for an **API gateway on top of a Graphcool CRUD API**. The idea is to customize the API operations that are exposed to your clients by _hiding_ the original CRUD API and defining a custom schema on top of it. 

The API gateway uses dedicated tooling that allows to easily implement a mapping from the custom schema to the underlying CRUD API.

Try out the read-only [demo](https://graphqlbin.com/BrkcP).

## Get started

### 1. Download the example

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/typescript-gateway-custom-schema
cd typescript-gateway-custom-schema
```

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go):

```sh
npm install -g graphcool
```

### 3. Deploy the Graphcool service

The next step is to [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined inside the [`service`](./service) directory:

```sh
cd service
graphcool deploy
```

When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

Then copy the endpoint for the `Simple API`, you'll need it in the next step.

The service you just deployed provides a CRUD API for the `User` and `Post` model types that are defined in [`./service/types.graphql`](./service/types.graphql).

The goal of the gateway server is now to create a _custom_ GraphQL API that only exposes variants of the underlying CRUD API.

### 4. Configure and start the API gateway server

#### 4.1. Set the endpoint for the GraphQL CRUD API

You first need to connect the gateway to the CRUD API. 

Paste the the HTTP endpoint for the `Simple API` from the previous step into [`./gateway/index.ts`](./gateway/index.ts) as the value for `endpoint`, replacing the current placeholder `__SIMPLE_API_ENDPOINT__`:

```js
const endpoint = '__SIMPLE_API_ENDPOINT__' // looks like: https://api.graph.cool/simple/v1/__SERVICE_ID__
```

> **Note**: If you ever lose your API endpoint, you can get access to it again by running `graphcool info` in the root directory of your service (where [`graphcool.yml`](./service/graphcool.yml) is located).


#### 4.2. Start the server

Navigate into the [`gateway`](./gateway) directory, install the node dependencies and start the server:

```sh
cd ../gateway
yarn install
yarn start
```

#### 4.3. Open GraphQL Playground

The API that's exposed by the gateway is now available inside a GraphQL Playground under the following URL:

[`http://localhost:3000/playground`](http://localhost:3000/playground)


## Usage

### 1. Create dummy data using the CRUD API

Before you start running queries against the API gateway, you should create somme dummy data in your service's database. You'll do this with a GraphQL Playground that's running against the CRUD API (not the API gateway).

Navigate back into the [`service`](./service) directory and open a Playground:

```sh
cd ../server
graphcool playground
```

In the Playground, send the following mutation to create a new `User` node along with three `Post` nodes:

```graphql
mutation {
  createUser(
    name: "John", 
    alias: "john", 
    posts: [
      { title: "GraphQL is awesome" }, 
      { title: "Declarative data fetching with GraphQL" },
      { title: "GraphQL & Serverless" }
    ]
  ) {
    id
  }
}
```

> **Note**: It's important the `alias` of the `User` is set to `john`. Otherwise the API gateway won't return any data since the alias in this example is [hardcoded](./gateway/index.ts#L43).

### 2. Send queries to the API gateway

Now, that there's some initial data in the database, you can use the API gateway to fetch the data through the exposed API. Note that these queries have to be run in the Playground that's running on your localhost: [`http://localhost:3000/playground`](http://localhost:3000/playground).

Send the following query to fetch the posts that you just created:

```graphql
{
  viewer {
    me {
      id
      name
      posts(limit: 2) {
        title
      }
    }
  }
}
```


## What's in this example?

The API gateway is a thin layer on top of the Graphcool service's CRUD API. For this example, the CRUD API is based on the following data model defined in the service's [`types.graphql`](./service/types.graphql):

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  alias: String! @isUnique
  posts: [Post!]! @relation(name: "UserPosts")
}

type Post @model {
  id: ID! @isUnique
  title: String!
  author: User! @relation(name: "UserPosts")
}
```

Everyone who has access to the HTTP endpoint of the CRUD API can _see_ that it exposes the following operations:

```graphql
type Query {
  # Read operations for `Post`
  Post(id: ID): Post
  allPosts(filter: PostFilter, orderBy: PostOrderBy, skip: Int, after: String, before: String, first: Int, last: Int): [Post!]!

  # Read operations for `User`
  User(alias: String, id: ID): User
  allUsers(filter: UserFilter, orderBy: UserOrderBy, skip: Int, after: String, before: String, first: Int, last: Int): [User!]!
}

type Mutation {
  # Create, update, delete operations for `Post`
  createPost(title: String!, authorId: ID, author: PostauthorUser): Post
  updatePost(id: ID!, title: String, authorId: ID, author: PostauthorUser): Post
  deletePost(id: ID!): Post

  # Create, update, delete operations for `User`
  createUser(alias: String!, name: String!, postsIds: [ID!], posts: [UserpostsPost!]): User
  updateUser(alias: String, id: ID!, name: String, postsIds: [ID!], posts: [UserpostsPost!]): User
  deleteUser(id: ID!): User

  # Set relation between `Post` and `User` node
  addToUserPosts(postsPostId: ID!, authorUserId: ID!): AddToUserPostsPayload
}
```

The API gateway now creates another API that will be exposed to the clients. The server that exposes this API is executing its queries against the underlying CRUD API. The magic enabling this functionality is implemented in the [`run`](./gateway/index.ts#L11) function in [`index.ts`](./gateway/index.ts).

Here's the schema that defines the new API:

```graphql
type Query {
  viewer: Viewer!
}

type Viewer {
  me: User
  topPosts(limit: Int): [Post!]!
}
```

There are four major steps that are being performed to map the CRUD API to the new schema:

1. Create local version of the CRUD API using [`makeRemoteExecutableSchema`](http://dev.apollodata.com/tools/graphql-tools/remote-schemas.html#makeRemoteExecutableSchema). [See the code](./gateway/index.ts#L13).
2. Define schema for the new API (the one exposed by the API gateway). [See the code](./gateway/index.ts#L21).
3. Merge remote schema with new schema using [`mergeSchemas`](http://dev.apollodata.com/tools/graphql-tools/schema-stitching.html#mergeSchemas). [See the code](./gateway/index.ts#L33).
4. Limit exposed operations from merged schemas (hiding all root fields except `viewer`) using [`transformSchema`](https://github.com/graphcool/graphql-transform-schema). [See the code](./gateway/index.ts#L56).




