# Resolver Forwarding

This example demonstrates how to use **resolver forwarding (which creates a 1-to-1 mapping from application schema to Prisma database schema)** when building a GraphQL server based on Prisma & graphql-yoga.

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/resolver-forwarding
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd resolver-forwarding
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

### Run the following query

```graphql
query {
  posts(where: { author: { name_in: ["Prisma"] } }) {
    id
    title
    status
    author {
      id
      name
      handle
    }
  }
}
```

### Run the following mutation

```graphql
mutation {
  createPost(
    data: {
      title: "Second Post"
      content: "Second Post Content"
      status: DRAFT
      author: { connect: { handle: "prisma" } }
    }
  ) {
    id
  }
}
```

Notice that how we are able to associate a `User` with this newly created post. The user was created using `seed.graphql` when the first deploy happened.

We are able to associate the user using their `handle` field as it is decorated with `@unique` directive.

Feel free to play around with the API.
