# Subscriptions

This example demonstrates how to use **subscriptions** and the Graphcool event system.

## Overview

The services contains is based on the data model (defined in [`types.graphql`](./types.graphql)) for a simple blogging application:

```graphql
type User @model {
  id: ID! @isUnique
  
  name: String!
  articles: [Article!]! @relation(name: "UserArticles")
}

type Article @model {
  id: ID! @isUnique

  title: String!
  author: User! @relation(name: "UserArticles")
}
```

Whenever a new `User` is created in the database, a _subscription_ is triggered that will automatically create an initial `Article` for the new `User`.

Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

```
.
├── README.md
├── graphcool.yml
├── node_modules
├── package.json
├── src
│   ├── createFirstArticle.graphql
│   └── createFirstArticle.js
└── types.graphql
```

> Read more about [service configuration](https://graph.cool/docs/reference/project-configuration/overview-opheidaix3) in the docs.

## Get started

### 1. Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/subscriptions
cd subscriptions
```

Next, you need to create your GraphQL server using the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go).

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

### 3. Create the GraphQL server

You can now [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory. Before that, you need to install the node dependencies for the subscription function:

```sh
yarn install      # install dependencies
graphcool deploy  # deploy service
```

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.

That's it, you're now ready to send queries and mutations against your GraphQL API! 🎉

## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground with the following command:

```sh
graphcool playground
```

### Retrieve `User` nodes from the database with the `allUsers`-query

In the Playground, you can now send a query to retrieve all the `User` nodes, including the related `Article` nodes:

```graphql
{
  allUsers {
    name
    articles {
      title
    }
  }
}
```

As expected, the server only responds with an empty list of `User` nodes:

```js
{
  "data": {
    "allUsers": []
  }
}
```

### Create a new `User` node with the `createUser`-mutation

Now, you're going to create a new `User` node in the database. Since you also have a `subscription` configured (in [`graphcool.yml`](./graphcool.yml#L4)) which fires upon `User.create`-events, this operation will invoke the corresponding function (implemented in [`createFirstArticle.js`](./src/createFirstArticle.js)):

```graphql
mutation {
  createUser(name: "Sarah") {
    id
  }
}
```

After you've sent this mutation, you can go back and send the previous `allUsers`-query again. This time, the response will look like this:

```js
{
  "data": {
    "allUsers": [{
      "name": "Sarah",
      "articles": [{
        "title": "My name is Sarah, and this is my first article!"  
      }]
    }]
  }
}
```


## What's in this example?

This example demonstrates how to use Graphcool's subscription functions. There's a single `subscription` configured inside the service definition file [`graphcool.yml`](./graphcool.yml#L4).

The subscription `query` defined in [`createFirstArticle.graphql`](./src/createFirstArticle.graphql) determines:
  - _when_ the corresponding `handler` function will be invoked (i.e. what kind of _event_ you're actually _subscribing_ to).
  - the _input type_ for the `handler` function. All fields that are specified in the selection set of the subscription query will be carried by the [`event`](./src/createFirstArticle.js#L18) that's passed into the `handler` function. 

Here's what the subscription query looks like:

```graphql
subscription {
  User(filter: {
    mutation_in: [CREATED]  
  }) {
  node {
    id
    name
  }
}
```

When the subscription gets deployed, it will wait for `CREATED`-events of the `User` type. Whenever that event occurs (triggered through a `createUser`-mutation), the corresponding `handler` function is invoked.

This is the implementation of the `handler`:

```js
const { fromEvent } = require('graphcool-lib')

module.exports = event => {

  // Retrieve payload from event
  const { id, name } = event.data.User.node

  // Create Graphcool API (based on https://github.com/graphcool/graphql-request)
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1') // `api` has a connection to your service's API 

  // Create variables for mutation
  const title = `My name is ${name}, and this is my first article!`
  const variables = { authorId: id title }

  // Create mutation
  const createArticleMutation = `
    mutation ($title: String!, $authorId: ID!) {
      createArticle(title: $title, authorId: $authorId) {
        id
      }
    }
  `

  // Send mutation with variables
  return api.request(createArticleMutation, variables)
}
```


























