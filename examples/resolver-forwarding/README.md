# Resolver forwarding

This example demonstrates how to use **resolver forwarding (which creates a 1-to-1 mapping from application schema to Prisma database schema)** when building a GraphQL server based on Prisma & [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

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

You can now [deploy](https://www.prismagraphql.com/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1. Remove the `cluster` property from `prisma.yml`.
1. Run `yarn prisma deploy`.
1. When prompted by the CLI, select a public cluster (e.g. `prisma-eu1` or `prisma-us1`).
1. Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command.

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

Or you can open a Playground by navigating to [http://localhost:4000](http://localhost:4000) in your browser.

> **Note**: You can also invoke the `yarn dev` script (instead of `yarn start`) which starts the server _and_ opens a Playground in parallel. This will also give you access to the Prisma API directly.

### Examples

### Deleting posts

The following mutation can be executed against the application schema, but _not_ against Prisma schema (note that the placeholder `__POST_ID__` needs to be replaced with the `id` of an actual `Post` node):

```graphql
mutation {
  deletePost(id: "__POST_ID__") {
    id
  }
}
```

If you wanted to delete a `Post` against the Prisma schema directly:

```graphql
mutation {
  deletePost(by: {
    id: "__POST_ID__"
  }) {
    id
  }
}
```

### Creating posts

Since the `createPost` mutation is mapped 1-to-1 from the application schema to the Prisma schema and implemented using `forwardTo`, the following mutation works against both APIs:

```graphql
mutation {
  createPost(data: {
    title: "GraphQL is awesome"
  }) {
    id
  }
}
```

## Troubleshooting

<details>
 <summary><strong>I'm getting the error message <code>[Network error]: FetchError: request to http://localhost:4466/resolver-forwarding-example/dev failed, reason: connect ECONNREFUSED</code> when trying to send a query or mutation</strong></summary>

This is because the endpoint for the Prisma service is hardcoded in [`index.js`](index.js#L23). The service is assumed to be running on the default port for a local cluster: `http://localhost:4466`. Apparently, your local cluster is using a different port.

You now have two options:

1. Figure out the port of your local cluster and adjust it in `index.js`. You can look it up in `~/.prisma/config.yml`.
1. Deploy the service to a public cluster. Expand the `I don't have Docker installed on my machine`-section in step 2 for instructions.

Either way, you need to adjust the `endpoint` that's passed to the `Prisma` constructor in `index.js` so it reflects the actual cluster domain and service endpoint.

</details>