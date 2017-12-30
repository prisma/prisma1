---
alias: phe8vai1oo
description: Get started with in 5 min Graphcool and Node.js by building a GraphQL backend and deploying it with Docker
github: https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic
---

# Node.js Graphcool Quickstart

In this quickstart tutorial, you'll learn how to build a GraphQL server with Node.js. You will use  [`graphql-yoga`](https://github.com/graphcool/graphql-yoga/) as your web server which is connected to a "GraphQL database" using [`graphcool-binding`](https://github.com/graphcool/graphcool-binding).

> The code for this project can be found as a _GraphQL boilerplate_ project on [GitHub](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic).

## Step 1: Install required command line tools

The first thing you need to is install the command line tools you'll need for this tutorial:

- `graphql-cli` is used initially to bootstrap the file structure for your server with `graphql create`
- `graphcool` is used continuously to manage your Graphcool database service

<Instruction>

```sh
npm install -g graphql-cli
npm install -g graphcool@beta
```

</Instruction>

## Step 2: Bootstrap your GraphQL server

<Instruction>

Now you can use `graphql create` to bootstrap your project. With the following command, you name your project `my-app` and choose to use the `node-basic` boilerplate:

```sh
graphql create my-app --boilerplate node-basic
cd my-app
```

</Instruction>

Here's the file structure of the project:

![](https://cdn-images-1.medium.com/max/900/1*xBvxqgFDJrUGxDZChPObHA.png)

Let's investigate the generated files and understand their roles:

- `/` (_root directory_)
  - [`.graphqlconfig.yml`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic/.graphqlconfig.yml) GraphQL configuration file containing the endpoints and schema configuration. Used by the [`graphql-cli`](https://github.com/graphcool/graphql-cli) and the [GraphQL Playground](https://github.com/graphcool/graphql-playground). See [`graphql-config`](https://github.com/graphcool/graphql-config) for more information.
  - [`graphcool.yml`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic/graphcool.yml): The root configuration file for your database service ([documentation](https://www.graph.cool/docs/1.0/reference/graphcool.yml/overview-and-example-foatho8aip)).
- `/database`
  - [`database/datamodel.graphql`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic/database/datamodel.graphql) contains the data model that you define for the project (written in [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51)). We'll discuss this next.
  - [`database/schema.generated.graphql`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic/database/schema.generated.graphql) defines the **database schema**. It contains the definition of the CRUD API for the types in your data model and is generated based on your `datamodel.graphql`. **You should never edit this file manually**, but introduce changes only by altering `datamodel.graphql` and run `graphcool deploy`.
- `/src`
  - [`src/schema.graphql`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic/src/schema.graphql) defines your **application schema**. It contains the GraphQL API that you want to expose to your client applications.
  - [`src/index.js`](https://github.com/graphql-boilerplates/node-graphql-server/tree/master/basic/src/index.js) is the entry point of your server, pulling everything together and starting the `GraphQLServer` from [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

Most important for you at this point are `database/datamodel.graphql` and `src/schema.graphql`. `database/datamodel.graphql` is used to define your data model. This data model is the foundation for the API that's defined in `src/schema.graphql` and exposed to your client applications.

Here is what the data model looks like:

```graphql(path="database/datamodel.graphql")
type Post {
  id: ID! @unique
  isPublished: Boolean!
  title: String!
  text: String!
}
```

Based on this data model Graphcool generates the **database schema**, a [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) that defines a CRUD API for the types in your data model. This schema is stored in `database/schema.generated.graphql` and will be updated every time you [`deploy`](!alias-kee1iedaov) changes to your data model.

## Step 3: Deploy the Graphcool database service

Before you can start the server, you first need to make sure your GraphQL database is available. You can do so by deploying the correspdonding Graphcool service that's responsible for the database.

In this case, you'll deploy the Graphcool database service locally with [Docker](https://www.docker.com/).

> Another option would be to deploy it to the **free development cluster** of Graphcool Cloud. This cluster is not intended for production use, but rather for development and demo purposes. When deploying to the development cluster, you need to authenticate with Graphcool Cloud.

<Instruction>

If you don't have Docker installed on your machine yet, go and download it now from the official website:

- [Mac OS](https://www.docker.com/docker-mac)
- [Windows](https://www.docker.com/docker-windows)

</Instruction>

With Docker installed, you now need to fetch the required Docker images that provide the functionality for Graphcool and start the corresponding containers. In general, the `graphgcool local <subcommand>` commands are used to manage your Docker setup. However, when first getting started you don't need to worry about that. Everything that needs to be done will be handled by the `graphcool deploy` command.

<Instruction>

Deploy the database service from the root directory of the project:

```bash(path="")
graphcool deploy
```

</Instruction>

This command created a new entry in the `clusters` list in the global `.graphcool/config.yml` file (which is located in your _home_ directory), looking similar to the following:

<!--```yml(path="~/.graphcool/config.yml"&nocopy) -->

```yml(path=".../.graphcool/config.yml"&nocopy)
clusters:
  local:
    host: 'http://localhost:60000'
    clusterSecret: >-
      eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDgwODI3NjMsImNsaWVudElkIjoiY2o4bmJ5bjE3MDAvMDAxNzdmNHZzN3FxNCJ9.sOyzwJplYF2x9YHXGVtnd-GneMuzEQauKQC9vLxBag0
```

This cluster called `local` is exactly the cluster you want to deploy your service to.

<Instruction>

When prompted which cluster you want to deploy to, choose the `local` cluster from the **Custom clusters** section. This section lists all your local clusters that are defined in the global `~/.graphcool/config.yml`.

</Instruction>

Once you selected the cluster, the database is deployed and will be accessible under [`http://localhost:60000/my-app/dev`](http://localhost:60000/my-app/dev).

As you might recognize, the HTTP endpoint for the database service is composed of the following components:

- The **cluster's domain** (specified as the `host` property in `~/.graphcool/config.yml`): `http://localhost:60000/my-app/dev`
- The **name** of the Graphcool service specified in `graphcool.yml`: `my-app`
- The **stage** to which the service is deployed, by default this is calleds: `dev`

Note that the endpoint is referenced in `src/index.js`. There, it is used to instantiate `Graphcool` in order to create a binding between the application schema and the database schema:

```js(path="src/index.js"&nocopy)
const server = new GraphQLServer({
  typeDefs,
  resolvers,
  context: req => ({
    ...req,
    db: new Graphcool({
      schemaPath: './database/schema.generated.graphql',
      endpoint: 'http://localhost:60000/api/my-app/dev',
      secret: 'your-graphcool-secret',
    }),
  }),
})
```

You're now set to start the server! 🚀

## Step 4: Start the server

<Instruction>

Execute the `start` script that's define in `package.json`:

```bash(path="")
yarn start
```

</Instruction>

## Step 5: Open a GraphQL playground to send queries and mutations

Now that the server is running, you can use a [GraphQL Playground](https://github.com/graphcool/graphql-playground) to interact with it.

<Instruction>

Open a GraphQL Playground by executing the following command:

```bash(path="")
graphcool playground
```

</Instruction>

Note that the Playground let's you interact with two GraphQL APIs side-by-side:

- `app`: The web server's GraphQL API defined in the **application schema** (from `./server/src/schema.graphql`)
- `database`: The CRUD GraphQL API of the Graphcool database service defined in the **database schema** (from `./server/database/schema.generated.graphql`)

![](https://imgur.com/z7MWZA8.png)

> Note that each Playground comes with auto-generated documentation which displays all GraphQL operations (i.e. queries, mutations as well as subscriptions) you can send to its API. The documentation is located on the rightmost edge of the Playground.

Once the Playground opened, you can send queries and mutations.

### Sending queries and mutations against the application schema

The GraphQL API defined by your application schema (`src/schema.graphql`) can be accessed using the `app` Playground.

<Instruction>

Paste the following mutation into the left pane of the `app` Playground and hit the _Play_-button (or use the keyboard shortcut `CMD+Enter`):

```grahpql
mutation {
  createDraft(
    title: "GraphQL is awesome!",
    text: "It really is."
  ) {
    id
  }
}
```

</Instruction>

If you now send the `feed` query, the server will still return an empty list. That's because `feed` only returns `Post` nodes where `isPublished` is set to `true` (which is not the case for `Post` nodes that were created using the `createDraft` mutation). You can publish a `Post` by calling the `publish` mutation for it.

<Instruction>

Copy the `id` of the `Post` node that was returned by the `createDraft` mutation and use it to replace the `__POST_ID__` placeholder in the following mutation:

```graphql
mutation {
  publish(id: "__POST_ID__") {
    id
    isPublished
  }
}
```

</Instruction>

<Instruction>

Now you can finally send the `feed` query and the published `Post` will be returned:

```graphql
query {
  feed {
    id
    title
    text
  }
}
```

</Instruction>

### Sending queries and mutations against the database schema

The GraphQL CRUD API defined by the database schema (`database/schema.generated.graphql`) can be accessed using the `database` Playground.

As you're now running directly against the database API, you're not limited to the operations from the application schema any more. Instead, you can take advantage of full CRUD capabilities to directly create a _published_ `Post` node.

<Instruction>

Paste the following mutation into the left pane of the `database` Playground and hit the _Play_-button (or use the keyboard shortcut `CMD+Enter`):

```graphql
mutation {
  createPost(
    title: "What I love most about GraphQL",
    text: "That it is declarative.",
    isPublished: true
  ) {
    id
  }
}
```

</Instruction>

The `Post` node that was created from this mutation will already be returned by the `feed` query from the application schema since it has the `isPublished` field set to `true`.

In the `database` Playground, you can also send mutations to _update_ and _delete_ existing posts. In order to do so, you must know their `id`s.

<Instruction>

Send the following query in the `database` Playground:

```graphql
{
  posts {
    id
    title
  }
}
```

</Instruction>

<Instruction>

From the returned `Post` nodes, copy the `id` of the one that you just created (where the `title` was `What I love most about GraphQL`) and use it to replace the `__POST_ID__` placeholder in the following mutation:

```graphql
mutation {
  updatePost({
    by: { id: "__POST_ID__" },
    data: { text: "The awesome community." }
  }) {
    id
    title
    text
  }
}
```

</Instruction>

With this mutation, you're updating the `text` from `That it is declarative.` to `The awesome community.`.

<Instruction>

Finally, to delete a `Post` node, you can send the following mutation (where again `__POST_ID__` needs to be replaced with the actual `id` of a `Post` node):

mutation {
  deletePost({
    by: { id: "__POST_ID__" }
  }) {
    id
    title
    text
  }
}

</Instruction>
