# Authentication

This example demonstrates how to implement an **email-password-based authentication** workflow with Graphcool. Feel free to use it as a template for your own project!

## Overview

This directory contains the service definition and file structure for a simple Graphcool authentication service. Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

```
.
├── README.md
├── graphcool.yml
├── package.json
├── src
│   ├── authenticate.graphql
│   ├── authenticate.js
│   ├── loggedInUser.graphql
│   ├── loggedInUser.js
│   ├── signup.graphql
│   └── signup.js
└── types.graphql
```

> Read more about [service configuration](https://graph.cool/docs/reference/project-configuration/overview-opheidaix3) in the docs.

## Get started

### 1. Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/auth
cd auth
```

Next, you need to create your GraphQL server using the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go).

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

### 3. Create the GraphQL server

You can now [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory. Before that, you need to install the node [dependencies](package.json#L11) for the defined functions:

```sh
yarn install      # install dependencies
graphcool deploy  # deploy service
```

When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.

That's it, you're now ready to offer a email-password based login to your users! 🎉


## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground with the following command:

```sh
graphcool playground
```

### Creating a new user with the `signupUser` mutation

You can send the following mutation in the Playground to create a new `User` node and at the same time retrieve an authentication token for it:

```graphql
mutation {
  signupUser(email: "alice@graph.cool" password: "graphql") {
    id
    token
  }
}
```

### Logging in an existing user with the `authenticateUser` mutation

This mutation will log in an _existing_ user by requesting a new [temporary authentication token](https://graph.cool/docs/reference/auth/authentication/authentication-tokens-eip7ahqu5o#temporary-authentication-tokens) for her:

```graphql
mutation {
  authenticateUser(email: "alice@graph.cool" password: "graphql") {
    token
  }
}
```

### Checking whether a user is currently logged in with the `loggedInUser` query

For this query, you need to make sure a valid authentication token is sent in the `Authorization` header of the request. Inside the Playground, you can set HTTP headers in the bottom-left corner:

![](https://imgur.com/kfvBcW1.png)

Once you've set the header, you can send the following query to check whether the token is valid:

```graphql
{
  loggedInUser {
    id
  }
}
```

If the token is valid, the server will return the `id` of the `User` node that it belongs to.


## What's in this example?

### Types

This example demonstrates how you can implement an email-password-based authentication workflow. It defines a single type in [`types.graphql`](./types.graphql):

```graphql
type User @model {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!

  email: String! @isUnique
  password: String!
}
```

### Functions

We further define three [resolver](https://graph.cool/docs/reference/functions/resolvers-su6wu3yoo2) functions in the service definition file [`graphcool.yml`](./graphcool.yml):

- [`signup`](./graphcool.yml#L5): Allows users to signup for the service with their email address and a password. Uses the `signupUser(email: String!, password: String!)` mutation defined in [`./src/signup.graphql`](./src/signup.graphql) and is implemented in [`./src/signup.js`](./src/signup.js).
- [`authenticate`](./graphcool.yml#L12): Allows already existing users to log in, i.e. requesting a new [temporary authentication token](https://graph.cool/docs/reference/auth/authentication/authentication-tokens-eip7ahqu5o#temporary-authentication-tokens). Uses the `authenticateUser` mutation defined in [`authenticate.graphql`](./authenticate.graphql)  and is implemented in [`./src/authenticate.js`](./src/authenticate.js).
- [`loggedInUser`](./graphcool.yml#L19): Allows to check whether a user is currently logged in by sending a request with an attached authentication token. If the token is valid for a particular user, the user's `id` will be returned. It uses the `loggedInUser` query defined in [`./src/loggedInUser.graphql`](./src/loggedInUser.graphql) and is implemented in [`./src/loggedInUser.js`](./src/loggedInUser.js).

The `signup` and `authenticate` resolvers each use [graphcool-lib](https://github.com/graphcool/graphcool-lib) to [generate an authentication token](https://github.com/graphcool/graphcool-lib/blob/master/src/index.ts#L37) for an existing `User` node. 

