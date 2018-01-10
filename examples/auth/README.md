# Authentication

This example demonstrates how to implement an **email-password-based authentication** workflow with Graphcool. Feel free to use it as a template for your own project!

## Overview

This directory contains a GraphQL server (based on [`graphql-yoga`](https://github.com/graphcool/graphql-yoga/)) which connects to a Graphcool database service.

```
.
├── README.md
├── database
│   ├── datamodel.graphql
│   └── schema.generated.graphql
├── graphcool.yml
├── src
│   ├── schema.graphql
│   ├── index.js
│   ├── auth.js
│   └── utils.js
├── package.json
└── yarn.lock
```

## Get started

### 0. Prerequisites

You need to have the following things installed: 

* Node.js 8 (or higher)
* yarn

### 1. Download the example

Clone the Graphcool monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/auth
cd auth
```

### 2. Deploy the Graphcool database service

You can now [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in `database/graphcool.yml`:

```sh
yarn graphcool deploy
```

> Note: Whenever you make changes to files in the `database` directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the running service.

### 3. Deploy the GraphQL server

Your GraphQL web server that's powered by [`graphql-yoga`](https://github.com/graphcool/graphql-yoga/) is now ready to be deployed. This is because the Graphcool database service it connects to is now available.

```sh
yarn start
```

The server is now running on [http://localhost:4000](http://localhost:4000).

## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
graphcool playground
```

Or you can open a Playground by navigating to [http://localhost:4000](http://localhost:4000) in your browser.

### Register a new user with the `signup` mutation

You can send the following mutation in the Playground to create a new `User` node and at the same time retrieve an authentication token for it:

```graphql
mutation {
  signup(email: "alice@graph.cool" password: "graphql") {
    token
    user {
      id
    }
  }
}
```

### Logging in an existing user with the `login` mutation

This mutation will log in an _existing_ user by requesting a new authentication token for her:

```graphql
mutation {
  login(email: "alice@graph.cool" password: "graphql") {
    token
  }
}
```

### Checking whether a user is currently logged in with the `me` query

For this query, you need to make sure a valid authentication token is sent in the `Authorization` header of the request. Inside the Playground, you can set HTTP headers in the bottom-left corner:

![](https://imgur.com/kfvBcW1.png)

Once you've set the header, you can send the following query to check whether the token is valid:

```graphql
{
  me {
    id
    email
  }
}
```

If the token is valid, the server will return the `id` and `email` of the `User` node that it belongs to.