# Resolver Forwarding

This example demonstrates how to use **resolver forwarding (which creates a 1-to-1 mapping from application schema to Prisma database schema)** when building a GraphQL server based on Prisma & graphql-yoga.

## Get started

### 1. Install the Prisma CLI
The `prisma` cli is the core component of your development workflow. `prisma` should be installed as a global dependency, you can install this with `npm install -g prisma`

### 2. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/prisma/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/resolver-forwarding
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd resolver-forwarding
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
