# Permissions (Authorization & Authentication)

## Overview

This example demonstrates how to configure **permission rules** for your application, in combination with an email-password based authentication workflow. It contains the service definition for a simple "Instagram"-app where users can share posts with their friends.

> The focus of this example is explaining how the Graphcool permission system works. If you're looking for a dedicated authentication example, check out the [auth](../auth) directory.

This is the data model for the application (defined in [`types.graphql`](./types.graphql)):

```graphql
enum Role {
  ADMIN
  CUSTOMER
}

type User @model {
  id: ID! @isUnique
  role: Role!
  posts: [Post!]! @relation(name: "UserPosts")
  email: String @isUnique
  password: String
}

type Post @model {
  id: ID! @isUnique
  description: String!
  imageUrl: String!
  author: User! @relation(name: "UserPosts")
}
```

Here is an overview of the file structure in this example:

```
.
├── README.md
├── graphcool.yml
├── node_modules
├── package.json
├── src
│   ├── email-password
│   │   ├── authenticate.graphql
│   │   ├── authenticate.js
│   │   ├── loggedInUser.graphql
│   │   ├── loggedInUser.js
│   │   ├── signup.graphql
│   │   ├── signup.js
│   │   └── signup.ts
│   └── permissions
│       ├── Post.graphql
│       └── User.graphql
├── types.graphql
└── yarn.lock
```

## Get started

### 1. Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/permissions
cd permissions
```

Next, you need to create your GraphQL server using the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go).

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

### 3. Create the GraphQL server

You can now [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory. Before that, you need to install the node [dependencies](package.json#L14) for the defined functions:

```sh
yarn install      # install dependencies
graphcool deploy  # deploy service
```

When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.

That's it, the service is now deployed and you can start sending queries and mutation to its API 🎉


## Usage

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open by pasting the HTTP endpoint for the GraphQL API into the address bar of a browser. To get access to the endpoint, use the following command in the terminal:

```sh
graphcool info
```

From the printed endpoints, chose the one for the **Simple API** and paste it into the address bar of your browser.

> Note: You can not properly verify permission rules inside the _standalone_ Playground app, since it gives you root access to all API operations.

### Creating a new user and getting an authentication token with the `signupUser` mutation

You can send the following mutation in the Playground to create a new `User` node and at the same time retrieve an authentication token for it:

```graphql
mutation {
  signupUser(
    email: "alice@graph.cool" 
    password: "graphql"
    role: CUSTOMER
  ) {
    id
    token
  }
}
```

### Authenticate requests

To authenticate requests that you're sending through the Playground, you need to set the token you just received as the `Authorization` HTTP header. You can do so in the bottom-left corner of the Playground:

![](https://imgur.com/kfvBcW1.png)

### Veriyfing permission rules

See [below](#permission-rules) for a list of all permission rules configured for this service. Here are instructions to test a few of them:

##### Testing `Post.create`

Only authenticated `User`s are able to create `Posts` (see the [rule](./graphcool.yml#L30)).

Send the following mutation to test the permission, **without having the HTTP `Authorization` header set**:

```graphql
mutation {
  createPost(
    description: "Great sunset"
    imageUrl: "http://example.org/sunset.png"
    authorId: "__AUTHOR_ID__" # replace with the `id` of the `User` you created before
  ) {
    id
  }
}
``` 

This will return an error with the following `message`: "Insufficient permissions for this mutation".

When setting the HTTP `Authorization` header as described above, the same mutation will create a new `Post` node. 

##### Testing `Post.delete`

To delete a node of type `Post`, a `User` must be authenticated and either the `author` of the `Post` or an `ADMIN` (see the [rule](./graphcool.yml#L30)):

```graphql
mutation {
  deletePost(
    id: "__POST_ID__" # replace with the `id` of the `Post` you created before
  ) {
    id
  }
}
```

This mutation will only work if the `User` that's sending the request (i.e. who the token in the `Authorization` HTTP header belongs to) is the `author` of the `Post` to be deleted, or if it's an `ADMIN`.

## What's in this example?

### Permission rules

Here's a list of all [permission rules](./graphcool.yml#L21) that are configured for this service:

##### `Post`

- `Post.read`: Everyone can **read** the fields `description` and `imageUrl` on nodes of type `Post`
- `Post.create`: Only authenticated users can **create** nodes of type `Post`
- `Post.update`: To **update** a node of type `Post`, a `User` must be:
  - authenticated
  - the `author` of the `Post` (see the permission query `UpdatePost` in [`src/permissions/Post.graphql`](./src/permissions/Post.graphql))
- `Post.delete`: To **delete** a node of type `Post`, a `User` must be:
  - authenticated
  - either the `author` of the `Post` or an `ADMIN` (see the permission query `DeletePost` in [`src/permissions/Post.graphql`](./src/permissions/Post.graphql))

##### `User`

- `User.read`: Everyone can the fields `email` and `posts` **read** nodes of type `User`
- `User.create`: `User` nodes can only be **created** with a [root token](https://graph.cool/docs/reference/auth/authentication/authentication-tokens-eip7ahqu5o#root-tokens) (see the code for the [`signup`](./src/email-password/signup.js) function)
- `User.update`: To **update** the fields `email`, `password` and `posts` on a node of type `User`, a `User` must be:
  - authenticated
  - either the "owner" of the `User` or an `ADMIN` (see the permission query `UpdateUserData` in [`src/permissions/.graphql`](./src/permissions/User.graphql))
- `User.update`: To **update** the field `role` on a node of type `User`, a `User` must be:
  - authenticated
  - an `ADMIN` (see the permission query `UpdateUserRole` in [`src/permissions/User.graphql`](./src/permissions/User.graphql))
- `User.delete`: To **delete** a node of type `User`, a `User` must be:
  - authenticated
  - either the "owner" of the `User` or an `ADMIN` (see the permission query `DeleteUser` in [`src/permissions/deleteUser.graphql`](./src/permissions/deleteUser.graphql))


##### `UsersPosts` (relation)

- `UsersPosts.connect`: To **connect** a `Post` node with a `User` node via the `UserPosts` relation, a `User` must be authenticated
- `UsersPosts.disconnect`: To **disconnect** a `Post` node from a `User` node in the `UserPosts` relation, a `User` must be authenticated


### Permission queries

Graphcool uses the concept of [permission queries](https://graph.cool/docs/reference/auth/authorization/permission-queries-iox3aqu0ee) to configure permission rules for a service.

Permission queries are special GraphQL queries that only return `true` or `false` (it's thus unnecessary to specify the _selection set_ of the query when writing it).

Before an operation (e.g. `Post.create`) is performed against the GraphQL API of a Graphcool service, Graphcool will check the permission rules for that operation specified in [`graphcool.yml`](./graphcool.yml). If the permission rules reference one or more permission queries, Graphcool will first execute these permission queries. The requested operation will only be performed if all permission queries return `true`.   









