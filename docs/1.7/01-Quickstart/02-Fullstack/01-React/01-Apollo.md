---
alias: tijghei9go
description: Get started in 5 min with [React](https://facebook.github.io/react/), [Apollo Client](https://github.com/apollographql/apollo-client) and [GraphQL](https://www.graphql.org) and learn how to build a simple Instagram clone.
github: https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic
---

# React & Apollo Quickstart

In this quickstart tutorial, you'll learn how to build a fullstack app with React, GraphQL and Node.js. You will use [`graphql-yoga`](https://github.com/graphcool/graphql-yoga/) as your web server. The server is connected to a Prisma database API using [`prisma-binding`](https://github.com/graphcool/prisma-binding).

To learn more about GraphQL server development and the required architecture, read the corresponding [Introduction](!alias-quohj3yahv) chapters.

![](https://imgur.com/g41vZah.png)

> The code for this project can be found as a _GraphQL boilerplate_ project on [GitHub](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic).

## Step 1: Install required command line tools

The first thing you need to do is install the command line tools you'll need for this tutorial:

- `prisma` is used continuously to manage your Prisma database API
- `graphql-cli` is used initially to bootstrap the file structure for your fullstack app with `graphql create`

<Instruction>

```sh
npm install -g prisma1 graphql-cli
```

</Instruction>

## Step 2: Bootstrap your React fullstack app

<Instruction>

Now you can use `graphql create` to bootstrap your project. With the following command, you name your project `my-app` and choose to use the `react-fullstack-basic` boilerplate:

```
graphql create my-app --boilerplate react-fullstack-basic
cd my-app
```

Feel free to get familiar with the code. The app contains the following React [`components`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/src/components):

- `Post`: Renders a single post item
- `ListPage`: Renders a list of post items
- `CreatePage`: Allows to create a new post item
- `DetailPage`: Renders the details of a post item and allows to update and delete it

Here is an overview of the generated files in the `server` directory and their roles in your server setup:

- `/server`
  - [`/server/.graphqlconfig.yml`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/.graphqlconfig.yml) GraphQL configuration file containing the endpoints and schema configuration. Used by the [`graphql-cli`](https://github.com/graphcool/graphql-cli) and the [GraphQL Playground](https://github.com/graphcool/graphql-playground). See [`graphql-config`](https://github.com/graphcool/graphql-config) for more information.
- `/server/database`
  - [`/server/database/prisma.yml`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/database/prisma.yml): The root configuration file for your database API ([documentation](https://www.prismagraphql.com/docs/reference/prisma.yml/overview-and-example-foatho8aip)).
  - [`/server/database/datamodel.graphql`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/database/datamodel.graphql) contains the data model that you define for the project (written in [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51)). We'll discuss this next.
  - [`/server/database/seed.graphql`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/database/seed.graphql): Contains mutations to seed the database with some initial data.
- `/server/src`
  - [`/server/src/schema.graphql`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/src/schema.graphql) defines your **application schema**. It contains the GraphQL API that you want to expose to your client applications.
  - [`/server/src/generated/prisma.graphql`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/src/generated/prisma.graphql) defines the **Prisma schema**. It contains the definition of the CRUD API for the types in your data model and is generated based on your `datamodel.graphql`. **You should never edit this file manually**, but introduce changes only by altering `datamodel.graphql` and run `prisma1 deploy`.
  - [`/server/src/index.js`](https://github.com/graphql-boilerplates/react-fullstack-graphql/tree/master/basic/server/src/index.js) is the entry point of your server, pulling everything together and starting the `GraphQLServer` from [`graphql-yoga`](https://github.com/graphcool/graphql-yoga).

Most important for you at this point are `database/datamodel.graphql` and `src/schema.graphql`.

`database/datamodel.graphql` is used to define your data model. This data model is the foundation for the API that's defined in `src/schema.graphql` and exposed to your React application.

Here is what the data model looks like:

```graphql(path="server/database/datamodel.graphql")
type Post {
  id: ID! @unique
  isPublished: Boolean! @default(value: "false")
  title: String!
  text: String!
}
```

Based on this data model Prisma generates the **Prisma database schema**, a [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) that defines a CRUD API for the types in your data model. In your case, this is only the `Post` type. The database schema is stored in `database/schema.generated.graphql` and will be updated every time you [`deploy`](!alias-kee1iedaov) changes to your data model.

You're now set to start the server! 🚀

## Step 3: Start the server

<Instruction>

Execute the `start` script that's define in `server/package.json`:

```bash(path="server")
yarn start
```

</Instruction>

The server is now running on [http://localhost:4000](http://localhost:4000).

## Step 5: Launch the React application

The last thing to do is actually launching the application 🚀

<Instruction>

Navigate back into the root directory of the project and run the app (note that you need to open another tab/window in the terminal because the current tab is used by the GraphQL server):

```sh(path="server")
cd ..
yarn start # open http://localhost:3000 in your browser
```

</Instruction>

## Next steps

- In this quickstart tutorial, you learned how to get started building a fullstack GraphQL app with React & Node.JS, using Prisma as a "GraphQL ORM" layer. If you want to learn about how the Prisma database layer actually works, you can check out [this](!alias-ouzia3ahqu) tutorial in our docs.
- Learn how to _deploy_ the GraphQL server with [Zeit Now](https://blog.graph.cool/deploying-graphql-servers-with-zeit-now-85f4757b79a7) or [Apex Up](https://blog.graph.cool/deploying-graphql-servers-with-apex-up-522f2b75a2ac).
- Learn how to build a fully-fledged GraphQL server with authentication, pagination, filters and realtime subscriptions in this in-depth [Node & GraphQL tutorial](https://www.howtographql.com/graphql-js/0-introduction/) on How to GraphQL.
