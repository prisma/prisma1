# Authentication

This example demonstrates how to implement a GraphQL server with an email-password-based authentication workflow based on Prisma & [graphql-yoga](https://github.com/graphcool/graphql-yoga).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/application-server | tar -xz --strip=2 prisma-master/examples/application-server
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd application-server
yarn install
```

### 2. Deploy the Prisma database service

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a demo server (rather than locally with Docker), you need to perform the following steps:

1.  Remove the `cluster` property from `prisma.yml`
1.  Run `yarn prisma deploy`
1.  When prompted by the CLI, select a demo server (e.g. `demo-eu1` or `demo-us1`)
1.  Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command

</details>

### 3. Explore the API

This example seeds some data into the database for us to explore some queries and features of the data model. Please take a look at `seed.graphql` for reference. Feel free to add/remove more data via mutations.

### To start the server, run the following command

`yarn start`

The easiest way to explore this deployed service and play with the API generated from the data model is by using the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
yarn playground
```

Or you can open a Playground by navigating to [http://localhost:4000](http://localhost:4000) in your browser.

#### Register a new user with the `signup` mutation

You can send the following mutation in the Playground to create a new `User` node and at the same time retrieve an authentication token for it:

```graphql
mutation {
  signup(email: "alice@graph.cool", password: "graphql") {
    token
  }
}
```

#### Logging in an existing user with the `login` mutation

This mutation will log in an _existing_ user by requesting a new authentication token for her:

```graphql
mutation {
  login(email: "alice@graph.cool", password: "graphql") {
    token
  }
}
```

#### Checking whether a user is currently logged in with the `me` query

For this query, you need to make sure a valid authentication token is sent along with the `Bearer`-prefix in the `Authorization` header of the request. Inside the Playground, you can set HTTP headers in the bottom-left corner:

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
