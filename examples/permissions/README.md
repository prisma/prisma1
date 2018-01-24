# Permissions

This example demonstrates how to implement a GraphQL server with authentication and **permission rules** based on Prisma & [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

For more information on implementing a permissions system with GraphQL, you can also follow [this tutorial](https://www.prismagraphql.com/docs/tutorials/graphql-server-development/permissions-thohp1zaih).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/permissions
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd permissions
yarn install
```

### 2. Deploy the Prisma database service

You can now [deploy](https://www.prismagraphql.com/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1. Remove the `cluster` property from `prisma.yml`
1. Run `yarn prisma deploy`
1. When prompted by the CLI, select a public cluster (e.g. `prisma-eu1` or `prisma-us1`)
1. Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command

</details>

### 3. Start the GraphQL server

The Prisma database service that's backing your GraphQL server is now available. This means you can now start the server:

```sh
yarn start
```

The server is now running on [http://localhost:4000](http://localhost:4000).

## Testing the API

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
yarn playground
```

Or you can open a Playground by simply navigating to [http://localhost:4000](http://localhost:4000) in your browser.

> **Note**: You can also invoke the `yarn dev` script (instead of `yarn start`) which starts the server _and_ opens a Playground in parallel. This will also give you access to the Prisma API directly.

### Register users with the `signup` mutation

You can send the following mutation in the Playground to create a new `User` node and at the same time retrieve an authentication `token` for that `User`:

```graphql
mutation {
  signup(
    email: "alice@graph.cool"
    password: "graphql"
  ) {
    token
  }
}
```

If no `admin` field is set, the role defaults to `CUSTOMER`. You can create users with the `ADMIN` role by setting `admin` to `true`:

```graphql
mutation {
  signup(
    email: "super_admin@graph.cool"
    password: "12345"
    admin: true
  ) {
    token
  }
}
```

### Logging in an existing user with the `login` mutation

This mutation will log in an _existing_ `User` node by requesting a new authentication `token`:

```graphql
mutation {
  login(
    email: "alice@graph.cool"
    password: "graphql"
  ) {
    token
  }
}
```

### Checking whether a user is currently logged in with the `me` query

For this query, you need to make sure a valid authentication token is sent along with the `Bearer `-prefix in the `Authorization` header of the request. Inside the Playground, you can set HTTP headers in the bottom-left corner:

![](https://imgur.com/bEGUtO0.png)

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

### Change the password with the `updatePassword` mutation

This mutation changes the password of the authenticated `User`. Make sure the `Authorization` header is set:

```graphql
mutation {
  updatePassword(
    oldPassword: "graphql"
    newPassword: "GraphQL42"
  ) {
    id
  }
}
```

You can verify the password change by trying the `login` mutation with the new password.

Admin users can also change the password of other users. Make sure the provided `Authorization` token is obtained from a `login` mutation of a `User` with the `ADMIN` role (you need to replace the placeholder `__USER_ID__` with the `id` of an actual `User`):

```graphql
mutation {
  updatePassword(
    userId: "__USER_ID__"
    newPassword: "test"
  ) {
    id
    email
  }
}
```

### Create posts with the `createPost` mutation

With this mutation, authenicated users can create a new `Post`. Make sure the `Authorization` header is set:

```graphql
mutation {
  createPost(
    title: "GraphQL is awesome"
  ) {
    id
  }
}
```

### Update posts with the `updateTitle` mutation

With this mutation users with the default `CUSTOMER` role can change their own posts (i.e. the one for which they're the `author`) and users with the `ADMIN` role can also change posts of other users (replace the `__POST_ID__` placeholder with the `id` of the `Post` to be updated):

```graphql
mutation {
  updateTitle(
    id: "__POST_ID__"
    newTitle: "Prisma makes building GraphQL servers a breeze"
  ) {
    title
  }
}
```
