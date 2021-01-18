---
alias: rohd6ipoo4
description: Get started with in 5 min Prisma and TypeScript by building a GraphQL backend and deploying it with Docker
github: https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic
---

# Bootstrap a TypeScript GraphQL server based on Prisma

In this quickstart tutorial, you'll learn how to build a GraphQL server with TypeScript. You will use [`graphql-yoga`](https://github.com/graphcool/graphql-yoga/) as your web server. The server is connected to a Prisma database API using [`prisma-binding`](https://github.com/graphcool/prisma-binding).

To learn more about the core concepts of GraphQL server development with Prisma and the architecture, read the corresponding [Introduction](!alias-quohj3yahv) chapters.

![](https://imgur.com/g41vZah.png)

> The code for this project can be found as a _GraphQL boilerplate_ project on [GitHub](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic).

## Step 1: Install required command line tools

Throughout the course of this tutorial, you'll use the Prisma CLI to create and manage your Prisma database API. You'll also use the [GraphQL CLI](https://github.com/graphql-cli/graphql-cli) for certain workflows around your GraphQL server.

<Instruction>

Open your terminal and globally install both CLIs via npm:

```sh
npm install -g prisma graphql-cli
```

</Instruction>

## Step 2: Bootstrap your GraphQL server

You'll now bootstrap the code for your GraphQL server using the `graphql create` command from the GraphQL CLI.

<Instruction>

Open your terminal and run the following command:

```sh
graphql create my-app --boilerplate typescript-basic
```

</Instruction>

You're passing two arguments to the command:

- `my-app`: This is the directory name where the CLI is going to put all files for your project.
- `--boilerplate typescript-basic`: This specifies which [GraphQL boilerplate](https://github.com/graphql-boilerplates) you want to use as a starter kit for your GraphQL server.

After `graphql create` has finished, your Prisma database API is deployed and will be accessible under the `endpoint` that's specified in `/my-app/database/prisma.yml`.

Note that the `endpoint` is also referenced in `src/index.ts`. There, it is used to instantiate `Prisma` in order to create a _GraphQL binding_ from the application schema and the Prisma database schema (think of the binding as a "GraphQL ORM" layer):

```ts(path="src/index.ts"&nocopy)
const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: new Prisma({
      endpoint: '__PRISMA_ENDPOINT__',  // the endpoint of the Prisma API
      debug: true,                      // log all GraphQL queries & mutations sent to the Prisma API
      // secret: 'mysecret123',         // only needed if specified in `database/prisma.yml`
    }),
  }),
})
```

Here's the file structure of the project:

![](https://imgur.com/95faUsa.png)

Let's investigate the generated files and understand their roles:

- `/` (_root directory_)
  - [`.graphqlconfig.yml`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/.graphqlconfig.yml) GraphQL configuration file containing the endpoints and schema configuration. Used by the [`graphql-cli`](https://github.com/graphcool/graphql-cli) and the [GraphQL Playground](https://github.com/graphcool/graphql-playground). See [`graphql-config`](https://github.com/graphcool/graphql-config) for more information.
- `/database`
  - [`database/prisma.yml`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/database/prisma.yml): The root configuration file for your Prisma database API ([documentation](https://www.prismagraphql.com/docs/reference/prisma.yml/overview-and-example-foatho8aip)).
  - [`database/datamodel.graphql`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/database/datamodel.graphql) contains the data model that you define for the project (written in [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51)). We'll discuss this next.
  - [`database/seed.graphql`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/database/seed.graphql): Contains mutations to seed the database with some initial data.
- `/src`
  - [`src/schema.graphql`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/src/schema.graphql) defines your **application schema**. It contains the GraphQL API that you want to expose to your client applications.
  - [`src/generated/prisma.graphql`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/src/generated/prisma.graphql) defines the **Prisma schema**. It contains the definition of the CRUD API for the types in your data model and is generated based on your `datamodel.graphql`. **You should never edit this file manually**, but introduce changes only by altering `datamodel.graphql` and run `prisma deploy`.
  - [`src/index.ts`](https://github.com/graphql-boilerplates/typescript-graphql-server/tree/master/basic/src/index.ts) is the entry point of your server, pulling everything together and starting the `GraphQLServer` from [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

Most important for you at this point are `database/datamodel.graphql` and `src/schema.graphql`. `database/datamodel.graphql` is used to define your data model. This data model is the foundation for the API that's defined in `src/schema.graphql` and exposed to your client applications.

Here is what the data model looks like:

```graphql(path="database/datamodel.graphql"&nocopy)
type Post {
  id: ID! @unique
  isPublished: Boolean! @default(value: "false")
  title: String!
  text: String!
}
```

Based on this data model Prisma generates the **Prisma database schema**, a [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) that defines a CRUD API for the types in your data model. This schema is stored in `src/generated/prisma.graphql` and will be updated by the CLI every time you [`deploy`](!alias-kee1iedaov) changes to your data model.

The TypeScript type definitions for the Prisma database schema are auto-generated using the `graphql prepare` command from the GraphQL CLI. They're stored in `src/generated/prisma.ts`.

You're now set to start the server! 🚀

## Step 4: Start the server

<Instruction>

Invoke the `dev` script that's defined in `package.json`. It will start the server and open a [GraphQL Playground](https://github.com/graphcool/graphql-playground) for you.

```bash(path="")
cd my-app
yarn dev
```

</Instruction>

Note that the Playground let's you interact with two GraphQL APIs side-by-side:

- `app`: The web server's GraphQL API defined in the **application schema** (from `./src/schema.graphql`)
- `database`: The CRUD GraphQL API of the Prisma database API defined in the **Prisma schema** (from `./src/generated/prisma.graphql`)

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

### Sending queries and mutations against the Prisma database API

The GraphQL CRUD API defined by the Prisma schema (`src/generated/prisma.graphql`) can be accessed using the `database` Playground.

As you're now running directly against the database API, you're not limited to the operations from the application schema any more. Instead, you can take advantage of full CRUD capabilities to directly create a _published_ `Post` node.

<Instruction>

Paste the following mutation into the left pane of the `database` Playground and hit the _Play_-button (or use the keyboard shortcut `CMD+Enter`):

```graphql
mutation {
  createPost(
    data: {
      title: "What I love most about GraphQL",
      text: "That it is declarative.",
      isPublished: true
    }
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
  updatePost(
    where: { id: "__POST_ID__" },
    data: { text: "The awesome community." }
  ) {
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

```graphql
mutation {
  deletePost(
    where: { id: "__POST_ID__" }
  ) {
    id
    title
    text
  }
}
```

</Instruction>

## Next steps

- In this quickstart tutorial, you learned how to get started building a GraphQL server using TypeScript with Prisma as a "GraphQL ORM". If you want to learn about how the Prisma database layer actually works, you can check out [this](!alias-ouzia3ahqu) tutorial in our docs.
- Learn how to _deploy_ the GraphQL server with [Zeit Now](https://blog.graph.cool/deploying-graphql-servers-with-zeit-now-85f4757b79a7) or [Apex Up](https://blog.graph.cool/deploying-graphql-servers-with-apex-up-522f2b75a2ac).
- Learn how to build a fully-fledged GraphQL server with authentication, pagination, filters and realtime subscriptions in this in-depth [Node & GraphQL tutorial](https://www.howtographql.com/graphql-js/0-introduction/) on How to GraphQL.