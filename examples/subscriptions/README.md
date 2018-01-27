# Susbcriptions

This example demonstrates how to implement **event-based realtime updates with GraphQL subscriptions** when building a GraphQL server based on Prisma & [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/subscriptions
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd subscriptions
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

### Triggering events and observing subscriptions

To try out this example and see subscriptions, it's best to open GraphQL Playgrounds (in two separate windows) side-by-side. You can use one for send a subscription and waiting to data to arrive. The second one is used to send a mutation (which _triggers the event_ you subscribed to in the other window). You can also do it in a single window, using multiple tabs in the Playground.

Inside the first Playground, start a subscription for the [`publications`](./src/schema.graphl#L13) subscription. This subscriptions fires when a new `Post` is _created_ or an existing one is _updated_ or _deleted_.

```graphql
subscription {
  publications {
    node {
      id
      title
      text
    }
  }
}
```

After starting the subscription, the **Play**-button turns into a red **Stop**-button. The results pane of the Playground also shows a loading spinner to indicate that it's now awaiting events.

You'll now trigger and event in the second Playground so that the subscription fires. Use the following mutation to create a new unpublished `Post`:

```graphql
mutation {
  writePost(
    title: "Secret Post"
    text: "This is my biggest secret"
    isPublished: false
  ) {
    id
  }
}
```

You can now observe that the subscription in the first Playgorund fired and you received the `id`, `title` and `text` of the newly created `Post`.

Next, switch to another tab and create a new published post:

```graphql
mutation {
  writePost(
    title: "Public Service Announcement"
    text: "GraphQL is awesome!"
    isPublished: true
  ) {
    id
  }
}
```

Note that a new event has again been fired in the first Playground.

You can further test the API by sending the `updateTitle` mutation for an existing `Post` (note that you need to replace the placeholder `__POST_ID__` with the `id` of an actual `Post` you previously created):

```graphql
mutation {
  updateTitle(
    id: "__POST_ID__"
    newTitle: "GraphQL makes frontend and backend development a breeze"
  ) {
    id
    title
  }
}
```

## Troubleshooting

<details>
 <summary><strong>I'm getting the error message <code>[Network error]: FetchError: request to http://localhost:4466/subscriptions-example/dev failed, reason: connect ECONNREFUSED</code> when trying to send a query or mutation</strong></summary>

This is because the endpoint for the Prisma service is hardcoded in [`index.js`](index.js#L23). The service is assumed to be running on the default port for a local cluster: `http://localhost:4466`. Apparently, your local cluster is using a different port.

You now have two options:

1. Figure out the port of your local cluster and adjust it in `index.js`. You can look it up in `~/.prisma/config.yml`.
1. Deploy the service to a public cluster. Expand the `I don't have Docker installed on my machine`-section in step 2 for instructions.

Either way, you need to adjust the `endpoint` that's passed to the `Prisma` constructor in `index.js` so it reflects the actual cluster domain and service endpoint.

</details>