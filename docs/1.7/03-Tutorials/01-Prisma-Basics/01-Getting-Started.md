---
alias: ouzia3ahqu
description: Learn how to generate a GraphQL API for your database with Prisma.
---

# Getting started with Prisma

In this tutorial, you'll learn how to get started with Prisma to generate a GraphQL API for your database.

Here are the steps you're going to perform:

- Install the Prisma CLI
- Bootstrapping a Prisma service with `prisma1 init`
- Explore the API in a GraphQL Playground and send queries & mutations

> To ensure you're not accidentally skipping an instruction in the tutorial, all required actions on your end are highlighted with a little counter on the left.
>
> **Pro tip**: If you're only keen on getting started but don't care so much about the explanations of what's going on, you can simply jump from instruction to instruction.

## Installing the Prisma CLI

Prisma services are managed with the [Prisma CLI](!alias-ieshoo5ohm). You can install it using `npm` (or `yarn`).

<Instruction>

Open your terminal and run the following command to install the Prisma CLI:

```
npm install -g prisma1
# or
# yarn global add prisma
```

</Instruction>

## Bootstrapping a Prisma service

<Instruction>

Open a terminal and navigate to a folder of your choice. Then bootstrap your Prisma service with the following command:

```sh
prisma1 init hello-world
```

</Instruction>

<Instruction>

After running the command, the CLI will ask you if you want to use an existing [Prisma server](!alias-eu2ood0she) or set up a new one. To get started quickly in this tutorial, you'll use a Prisma Sandbox. So choose either the `sandbox-eu1` or `sandbox-us1` and hit **Enter**.

</Instruction>

<Instruction>

The CLI will then prompt you to configure the _name_ and _stage_ for your Prisma API. You can just choose the suggested values by hitting **Enter** two times.

</Instruction>

This will create a new directory called `hello-world` as well as the two files which provide a minimal setup for your service:

- [`prisma.yml`](!alias-foatho8aip): The root configuration file for your service. It contains information about your service, like the name (which is used to generate the service's HTTP endpoint), a secret to secure the access to the endpoint and about where it should be deployed.
- `datamodel.graphql` (can also be called differently, e.g. `types.graphql`): This file contains the definition of your [data model](!alias-eiroozae8u), written in [GraphQL SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51).

Let's take a look at the contents of the generated files:

**`prisma.yml`**

```yml
endpoint: https://eu1.prisma.sh/public-mountainninja-311/hello-world/dev
datamodel: datamodel.graphql
```

Note that the endpoint will look slightly different for you as `public-mountainninja-311` is a randomly generated ID that will be different for every Prisma API you deploy to a Sandbox.

Here's an overview of the properties in the generated `prisma.yml`:

- `endpoint`: Defines HTTP endpoint of the Prisma API ([learn more](!alias-ufeshusai8#endpoint-optional)).
- `datamodel`: The path to the file which contains your data model ([learn more](!alias-ufeshusai8#datamodel-required)).

**`datamodel.graphql`**

```graphql
type User {
  id: ID! @unique
  name: String!
}
```

The data model contains type definitions for the entities in your application domain. In this case, you're starting out with a very simple `User` type with an `id` and a `name`.

The `@unique` directive here expresses that no two users in the database can have the same `id`, Prisma will ensure this requirement is met at all times.

Your Prisma API is now deployed and ready to receive your queries, mutations and subscriptions 🎉

## Exploring your service in a GraphQL Playground

So your API is deployed - but how do you know how to interact with it? What does its API actually look like?

In general, the generated API allows to perform CRUD operations on the types in your data model. It also exposes GraphQL subscriptions which allow clients to _subscribe_ to certain _events_ and receive updates in realtime.

It is important to understand that the data model is the foundation for your API. Every time you make changes to your data model (and run `prisma1 deploy` afterwards), the GraphQL API gets updated accordingly.

Because your data model contains the `User` type, the Prisma API now allows for its clients to create, read, update and delete instances, also called _nodes_, of that type. In particular, the following GraphQL operations are now generated based on the `User` type:

- `user`: Query to retrieve a single `User` node by its `id` (or another `@unique` field).
- `users`: Query to retrieve a list of `User` nodes.
- `createUser`: Mutation to create a new `User` node.
- `updateUser`: Mutation to update an existing `User` node.
- `deleteUser`: Mutation to delete an existing `User` node.

> **Note**: This list of generated operations is not complete. The Prisma API exposes a couple of more operations that, for example, allow to batch update/delete many nodes. However, all operations either create, read, update or delete nodes of the types defined in the data model.

To actually use these operations, you need a way to [send requests to your service's API](!alias-ohm2ouceuj). Since that API is exposed via HTTP, you could use tools like [`curl`](https://en.wikipedia.org/wiki/CURL) or [Postman](https://www.getpostman.com/) to interact with it. However, GraphQL actually has with much nicer tooling for that purpose: [GraphQL Playground](https://github.com/graphcool/graphql-playground), an interactive GraphQL IDE.

The GraphQL Playground is based on [`graphql-config`](https://github.com/graphcool/graphql-config), so before opening it you need to create a `.graphqlconfig.yml`-file where you specify your Prisma project:

<Instruction>

Create a new file inside the `hello-world` directory and call it `.graphqlconfig.yml`. Then add the following contents to it:

```yml
projects:
  prisma:
    extensions:
      prisma: prisma.yml
```

</Instruction>

<Instruction>

To open a GraphQL Playground, you can now use the Prisma CLI again. Simply run the following command inside the `hello-world` directory:

```sh
prisma1 playground
```

</Instruction>

This will open a Playground looking as follows:

![](https://imgur.com/HuJfglj.png)

> **Note**: The Playground can be installed on your machine as a [standalone desktop application](https://github.com/graphcool/graphql-playground/releases). If you don't have the Playground installed, the command automatically opens a Playground in your default browser.

One really cool property of GraphQL APIs is that they're effectively _self-documenting_. The [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) defines all the operations of an API, including input arguments and return types. This allows for tooling like the GraphQL Playground to auto-generate API documentation.

<Instruction>

To see the documentation for your Prisma API, click the green **SCHEMA**-button on the right edge of the Playground window.

</Instruction>

This brings up the Playground's documentation pane. The left-most column is a list of all the operations the API accepts. You can then drill down to learn the details about the input arguments or return types that are involved with each operation.

![](https://imgur.com/l82HjFR.png)

## Sending queries and mutations

All right! With everything you learned so far, you're ready to fire off some queries and mutations against your API. Let's start with the `users` query to retrieve all the `User` nodes currently stored in the database.

<Instruction>

Enter the following query into the left Playground pane and click the **Play**-button (or use the hotkey **CMD+Enter**):

```graphql
query {
  users {
    name
  }
}
```

</Instruction>

At this point, the server only returns an empty list. This is no surprise as we haven't actually created any `User` nodes so far. So, let's change that and use the `createUser` mutation to store a first `User` node in the database.

<Instruction>

Open a new tab in the Playground, enter the following mutation into the left Playground pane and send it:

```graphql
mutation {
  createUser(data: {
    name: "Sarah"
  }) {
    id
  }
}
```

</Instruction>

This time, the response from the server actually contains some data (note that the `id` will of course vary as the server generates a globally unique ID for every new node upon creation):

```json
{
  "data": {
    "createUser": {
      "id": "cjc69nckk31jx01505vgwmgch"
    }
  }
}
```

<Instruction>

You can now go back to the previous tab with the `users` query and send that one again.

</Instruction>

This time, the `User` node that was just created is returned in the server response:

![](https://imgur.com/LfXtmbc.png)

Note that the API also offers powerful filtering, ordering and pagination capabilities. Here are examples for queries that provide the corresponding input arguments to the `users` query.

**Retrieve all `User` nodes where the `name` contains the string `"ra"`**

```graphql
query {
  users(where: {
    name_contains: "ra"
  }) {
    id
    name
  }
}
```

**Retrieve all `User` nodes sorted descending by their names**

```graphql
query {
  users(orderBy: name_DESC) {
    id
    name
  }
}
```

**Retrieve a chunk of `User` nodes (position 20-29 in the list)**

```graphql
query {
  users(skip: 20, first: 10) {
    id
    name
  }
}
```

## Next steps

At this point, you can either move on the [**next chapter**](!alias-va4ga2phie) and learn how you can update your data model and API or visit one of the following resources:

- **Quickstart Tutorials (Backend & Frontend)**: The remaining quickstart tutorials explain how to use Prisma together with conrete languages and frameworks, like [React](!alias-tijghei9go), [Node.js](!alias-phe8vai1oo) or [TypeScript](!alias-rohd6ipoo4).
- [How to GraphQL tutorial for Node.js](https://www.howtographql.com/graphql-js/0-introduction/): In-depth tutorial teaching you how to build a GraphQL server implementing features lielke authentication, pagiation, filters and realtime subscriptions.
