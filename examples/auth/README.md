# auth

This example demonstrates how to implement an **email-password-based authentication** workflow with Graphcool. Feel free to use it as a template for your own project!

## Overview

This directory contains the service definition and file structure for a simple Graphcool authentication service. Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

```
.
├── README.md
├── graphcool.yml
├── src
│   ├── authenticate.graphql
│   ├── authenticate.js
│   ├── loggedInUser.graphql
│   ├── loggedInUser.js
│   ├── signup.graphql
│   └── signup.js
└── types.graphql
```

> Read more about [service configuration](https://docs-next.graph.cool/reference/project-configuration/overview-opheidaix3) in the docs.

## Get started

### Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download only this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/examples/tar.gz/master | tar -xz --strip=1 examples/auth
cd auth
```

### Create your GraphQL server

Next, you need to create your GraphQL server using the [Graphcool CLI](https://docs-next.graph.cool/reference/graphcool-cli/overview-zboghez5go).

#### Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

#### Create the GraphQL server

You can now [deploy](https://docs-next.graph.cool/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory:

```sh
graphcool deploy
```

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.


## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground with the following command:

```sh
graphcool playground
```

### Creating a new user with the `signupEmailUser` mutation

You can send the following mutation in the Playground to create a new `EmailUser` node and at the same time retrieve an authentication token for it:

```graphql
mutation {
  signupEmailUser(email: "alice@graph.cool" password: "graphql") {
    id
    token
  }
}
```

### Logging in an existing user with the `authenticateEmailUser` mutation

This mutation will log in an existing user by requesting a new [temporary authentication token]((https://docs-next.graph.cool/reference/auth/authentication/authentication-tokens-eip7ahqu5o#temporary-authentication-tokens) for her:

```graphql
mutation {
  authenticateEmailUser(email: "alice@graph.cool" password: "graphql") {
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

If the token is valid, the server will return the `id` of the `EmailUser` node that it belongs to.


## What's in this example?

### Types

This example demonstrates how you can implement an email-password-based authentication workflow. It defines a single type in [`types.graphql`](./types.graphql):

```graphql
type EmailUser @model {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!

  # Must be unique
  email: String! @isUnique
  password: String!
}
```

### Functions

We further define three [resolver](https://docs-next.graph.cool/reference/functions/resolvers-su6wu3yoo2) functions in the service definition file [`graphcool.yml`](./graphcool.yml):

- `signup`: Allows users to signup for the service with their email address and a password. Uses the `signupEmailUser(email: String!, password: String!)` mutation defined in [`./code/signup.graphql`](./code/signup.graphql) and is implemented in [`signup.js`](./signup.js).
- `authenticate`: Allows already existing users to log in, i.e. requesting a new [temporary authentication token](https://docs-next.graph.cool/reference/auth/authentication/authentication-tokens-eip7ahqu5o#temporary-authentication-tokens). Uses the `authenticateEmailUser` mutation defined in [`authenticate.graphql`](./authenticate.graphql)  and is implemented in [`./src/authenticate.js`](./src/authenticate.js).
- `loggedInUser`: Allows to check whether a user is currently logged in by sending a request with an attached authentication token. If the token is valid for a particular user, the user's `id` will be returned. It uses the `loggedInUser` query defined in [`./src/loggedInUser.graphql`](./src/loggedInUser.graphql) and is implemented in [`./src/loggedInUser.js`](./src/loggedInUser.js).

The `signup` and `authenticate` resolvers each use [graphcool-lib](https://github.com/graphcool/graphcool-lib) to [generate an authentication token](https://github.com/graphcool/graphcool-lib/blob/master/src/index.ts#L370) for an existing `EmailUser` node. 










