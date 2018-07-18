# Authentication

This example demonstrates how to implement a GraphQL server with an email-password-based authentication workflow using the [GraphQL Authentication](https://github.com/Volst/graphql-authentication) library. This package handles things links password reset, signup, inviting a user etc. for you. It also shows you how to use the [GraphQL Shield](https://github.com/maticzav/graphql-shield) library for protecting mutations and queries from unauthorized access. See the [*Permissions with GraphQL Shield*](https://github.com/prismagraphql/prisma/tree/master/examples/permissions-with-shield) example for a more detailed example for this.

## Get started

### 1. Install the Prisma CLI
The `prisma` cli is the core component of your development workflow. `prisma` should be installed as a global dependency, you can install this with `npm install -g prisma`

### 2. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/prismagraphql/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/graphql-authentication
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd graphql-authentication
yarn install
```

### 3. Deploy the Prisma database service

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
# Ensure docker is running the server's dependencies
docker-compose up
# Deploy the server
cd prisma
prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a demo server (rather than locally with Docker), please follow [this link](https://www.prisma.io/docs/quickstart/).

</details>

### 4. Explore the API

### To start the server, run the following command

`yarn start`

The easiest way to explore this deployed service and play with the API generated from the data model is by using the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground by navigating to [http://localhost:4000](http://localhost:4000) in your browser.

#### Register a new user with the `signup` mutation

You can send the following mutation in the Playground to create a new `User` node and at the same time retrieve an authentication token for it:

```graphql
mutation {
 signup(data: {name: "Alice", email: "alice@prisma.io", password: "graphqliscool"}) {
  token
 }
}
```

#### Logging in an existing user with the `login` mutation

This mutation will log in an _existing_ user by requesting a new authentication token for her:

```graphql
mutation {
 login(email: "alice@prisma.io", password: "graphqliscool") {
  token
 }
}
```

#### Checking whether a user is currently logged in with the `currentUser` query

For this query, you need to make sure a valid authentication token is sent along with the `Bearer`-prefix in the `Authorization` header of the request. Inside the Playground, you can set HTTP headers in the bottom-left corner:

![](https://imgur.com/bEGUtO0.png)

Once you've set the header, you can send the following query to check whether the token is valid:

```graphql
{
 currentUser {
  id
  email
 }
}
```

If the token is valid, the server will return the `id` and `email` of the `User` node that it belongs to.
