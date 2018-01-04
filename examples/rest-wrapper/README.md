# node-basic

🚀 Basic starter code for a scalable, production-ready GraphQL server for Node.js.

![](https://imgur.com/LG6r1q1.png)

## Features

- **Scalable GraphQL Server:** `graphql-yoga` based on Apollo Server & Express
- **GraphQL-native database:** Includes GraphQL database binding to Graphcool (running on MySQL)
- Out-of-the-box support for [GraphQL Playground](https://github.com/graphcool/graphql-playground) & [Tracing](https://github.com/apollographql/apollo-tracing)
- Simple data model – easy to adjust
- Preconfigured [`graphql-config`](https://github.com/graphcool/graphql-config) setup

## Requirements

You need to have the following things installed:

* Node 8+
* Graphcool CLI: `npm i -g graphcool@beta`
* GraphQL CLI: `npm i -g graphql-cli`
* GraphQL Playground desktop app (optional): [Download](https://github.com/graphcool/graphql-playground/releases)

## Getting started

```sh
# Bootstrap GraphQL server in directory `my-app`, based on `node-basic` boilerplate
graphql create my-app --boilerplate node-basic

# Navigate to the new project
cd my-app

# Deploy the Graphcool database
graphcool deploy

# Start server (runs on http://localhost:4000)
yarn start

# Open Playground to explore GraphQL API
yarn playground
```

<details>

<summary>Alternative: Clone repo</summary>

```sh
# Clone the repo and navigate into project directory
git clone https://github.com/graphql-boilerplates/node-graphql-server.git
cd node-graphql-server/basic

# Deploy the Graphcool database
graphcool deploy

# Install node dependencies
yarn install

# Start server (runs on http://localhost:4000)
yarn start

# Open Playground to explore GraphQL API
yarn playground
```

</details>

## Docs

### Commands

* `yarn start` starts GraphQL server
* `yarn playground` opens the GraphQL Playground
* `yarn deploy` deploys GraphQL server to [`now`](https://now.sh)

### Project structure

#### `/` (_root directory_)

- [`.graphqlconfig.yml`](./.graphqlconfig.yml) GraphQL configuration file containing the endpoints and schema configuration. Used by the [`graphql-cli`](https://github.com/graphcool/graphql-cli) and the [GraphQL Playground](https://github.com/graphcool/graphql-playground). See [`graphql-config`](https://github.com/graphcool/graphql-config) for more information.
- [`graphcool.yml`](./graphcool.yml): The root configuration file for your database service ([documentation](https://www.graph.cool/docs/1.0/reference/graphcool.yml/overview-and-example-foatho8aip)).

#### `/database`

- [`database/datamodel.graphql`](./database/datamodel.graphql) contains the data model that you define for the project (written in [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51)).
- [`database/schema.generated.graphql`](./database/schema.generated.graphql) defines the **database schema**. It contains the definition of the CRUD API for the types in your data model and is generated based on your `datamodel.graphql`. **You should never edit this file manually**, but introduce changes only by altering `datamodel.graphql` and run `graphcool deploy`.

#### `/src`

- [`src/schema.graphql`](src/schema.graphql) defines your **application schema**. It contains the GraphQL API that you want to expose to your client applications.
- [`src/index.js`](src/index.js) is the entry point of your server, pulling everything together and starting the `GraphQLServer` from [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

### Common questions

#### I'm getting a 'Schema could not be fetched.' error after deploying, what gives?

Access to the Graphcool API is secured by a secret. This also applies to the introspection query. Using the latest version of GraphQL Playground, the `Authorization` header should automatically be setup with a proper JWT signing the secret. If that's not the case, you can follow these steps to access your API:

1. Visit http://jwtbuilder.jamiekurtz.com/
1. Replace the `Key` at the bottom of the page with your `secret` from the [`graphcool.yml`](./graphcool.yml#L5)
1. Click `Create signed JWT` and copy the obtained token
1. Now, to access the schema, use the `Authorization: Bearer <token>` header, or in the GraphQL Playground set it as JSON:
    ```json
    {
      "Authorization": "Bearer <token>"
    }
    ```
1. Reload the schema in the Playground (the _refresh_-button is located right next to the URL of the server)

> Note: Currently, no content of the signed JWT is verified by the database! This will be implemented [according to this proposal](https://github.com/graphcool/framework/issues/1365) at a later stage.

## Contributing

Your feedback is **very helpful**, please share your opinion and thoughts! If you have any questions, join the [`#graphql-boilerplate`](https://graphcool.slack.com/messages/graphql-boilerplate) channel on our [Slack](https://graphcool.slack.com/).
