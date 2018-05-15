# CLI Tool

This example demonstrates how to implement a data store with Prisma for a CLI tool (In this case a simple todo list). Note that typically you would deploy Prisma behind an [application server](../application-server).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/cli-tool
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd cli-tool
yarn install
```

### 2. Deploy the Prisma database service

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1.  Remove the `cluster` property from `prisma.yml`
1.  Run `yarn prisma deploy`
1.  When prompted by the CLI, select a demo cluster (e.g. `prisma-eu1` or `prisma-us1`)
1.  Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command

</details>

### 3. Explore the CLI Tool

The Prisma database service that's backing your GraphQL server is now available. This means you can now start to test the CLI Tool:

#### Add a Todo item

```sh
node index.js add First todo item
```

#### List all Todo items

```sh
node index.js list
```

#### Delete a Todo item

```sh
node index.js delete First todo item
```

### 3. Explore the generated Prisma API

The easiest way to explore this deployed service and play with the API generated from the data model is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
yarn playground
```

Or you can open a Playground by navigating to [http://localhost:4466/cli-tool](http://localhost:4466/cli-tool) in your browser.

### Run the following query

```graphql
query Todoes {
  todoes(orderBy: id_DESC) {
    id
    title
  }
}
```
