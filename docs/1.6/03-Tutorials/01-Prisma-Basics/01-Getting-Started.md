---
alias: ouzia3ahqu
description: Learn how to generate a GraphQL API for your database with Prisma.
---

# Getting started with Prisma

In this tutorial, you'll learn how to get started with Prisma to generate a GraphQL API for your database.

Here are the steps you're going to perform:

- Install the Prisma CLI
- Bootstrapping a Prisma service with `prisma init`
- Explore the API in a GraphQL Playground and send queries & mutations

> To ensure you're not accidentally skipping an instruction in the tutorial, all required actions on your end are highlighted with a little counter on the left.
>
> **Pro tip**: If you're only keen on getting started but don't care so much about the explanations of what's going on, you can simply jump from instruction to instruction.

## Installing the Prisma CLI

Prisma services are managed with the [Prisma CLI](!alias-ieshoo5ohm). You can install it using `npm` (or `yarn`).

<Instruction>

Open your terminal and run the following command to install the Prisma CLI:

```
npm install -g prisma
# or
# yarn global add prisma
```

</Instruction>

## Bootstrapping a Prisma service

<Instruction>

Open a terminal and navigate to a folder of your choice. Then bootstrap your Prisma service with the following command:

```sh
prisma init hello-world
```

</Instruction>

This will create a new directory called `hello-world` as well as the two files which provide a minimal setup for your service:

- [`prisma.yml`](!alias-foatho8aip): The root configuration file for your service. It contains information about your service, like the name (which is used to generate the service's HTTP endpoint), a secret to secure the access to the endpoint and about where it should be deployed.
- `datamodel.graphql` (can also be called differently, e.g. `types.graphql`): This file contains the definition of your [data model](!alias-eiroozae8u), written in [GraphQL SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51).

> **Note**: The `hello-world` directory actually contains a third file as well: `.graphqlconfig.yml`. This file follows the industry standard for configuring and structuring GraphQL projects (based on [`graphql-config`](https://github.com/graphcool/graphql-config)). If present, it is used by GraphQL tooling (such as the GraphQL Playground, the [`graphql-cli`](https://github.com/graphql-cli/graphql-cli/), text editors, build tools and others) to improve your local developer workflows.

Let's take a look at the contents of the generated files:

**`prisma.yml`**

```yml
service: hello-world
stage: dev

datamodel: datamodel.graphql

# to enable auth, provide
# secret: my-secret
disableAuth: true
```

Here's an overview of the properties in the generated `prisma.yml`:

- `service`: Defines the service name which will be part of the service's HTTP endpoint
- `stage`: A service can be deployed to multiple stages (e.g. a _development_ and a _production_ environment)
- `datamodel`: The path to the file which contains your data model
- `disableAuth`: If set to true, everyone who knows the endpoint of your Prisma service has full read and write access. If set to `false`, you need to specify a `secret` in `prisma.yml` which is used to generate JWT authentication tokens. These tokens need to be attached to the `Authorization` header of the requests sent to the API of your service. The easiest way to obtain such a token is the `prisma token` command from the Prisma CLI.

> **Note**: We'll keep `disableAuth` set to `true` for this tutorial. In production applications, you'll always want to require authentication for your service! You can read more about this topic [here](!alias-pua7soog4v).

**`datamodel.graphql`**

```graphql
type User {
  id: ID! @unique
  name: String!
}
```

The data model contains type definitions for the entities in your application domain. In this case, you're starting out with a very simple `User` type with an `id` and a `name`.

The `@unique` directive here expresses that no two users in the database can have the same `id`, Prisma will ensure this requirement is met at all times.

## Deploying your Prisma service

`prisma.yml` and `datamodel.graphql` are your abstract _service definition_. To actually create an instance of this service that can be invoked via HTTP, you need to _deploy_ it.

<Instruction>

Inside the `hello-world` directory in your terminal, run the following command:

```sh
prisma deploy
```

</Instruction>

<!-- TODO: Enter screenshot of terminal -->

Since `prisma.yml` doesn't yet contain the information about _where_ (meaning to which `cluster`) your service should be deployed, the CLI triggers a prompt for you to provide this information. At this point, you can choose to either deploy it locally with [Docker](https://www.docker.com) (which of course requires you to have Docker installed on your machine) or to a development Prisma cluster. You'll use a development cluster for the purpose of this tutorial.

<Instruction>

When prompted where (i.e. to which _cluster_) to deploy your Prisma service, choose one of the _public cluster_ options: `prisma-eu1` or `prisma-us1`.

</Instruction>

Your Prisma service is now deployed and ready to accept your queries and mutations 🎉

## Exploring your service in a GraphQL Playground

So your service is deployed - but how do you know how to interact with it? What does its API actually look like?

In general, the generated API allows to perform CRUD operations on the types in your data model. It also exposes GraphQL subscriptions which allow clients to _subscribe_ to certain _events_ and receive updates in realtime.

It is important to understand that the data model is the foundation for your API. Every time you make changes to your data model, the GraphQL API gets updated accordingly.

Because your datamodel contains the `User` type, the Prisma API now allows for its clients to create, read, update and delete instances, also called _nodes_, of that type. In particular, the following GraphQL operations are now generated based on the `User` type:

- `user`: Query to retrieve a single `User` node by its `id` (or another `@unique` field).
- `users`: Query to retrieve a list of `User` nodes.
- `createUser`: Mutation to create a new `User` node.
- `updateUser`: Mutation to update an existing `User` node.
- `deleteUser`: Mutation to delete an existing `User` node.

> **Note**: This list of generated operations is not complete. The Prisma API exposes a couple of more convenience operations that, for example, allow to batch update/delete many nodes. However, all operations either create, read, update or delete nodes of the types defined in the data model.

To actually use these operations, you need a way to [send requests to your service's API](ohm2ouceuj). Since that API is exposed via HTTP, you could use tools like [`curl`](https://en.wikipedia.org/wiki/CURL) or [Postman](https://www.getpostman.com/) to interact with it. However, GraphQL actually comes with much nicer tooling for that purpose: [GraphQL Playground](https://github.com/graphcool/graphql-playground), an interactive GraphQL IDE.

<Instruction>

To open a GraphQL Playground, you can use the Prisma CLI again. Simply run the following command inside the `hello-world` directory:

```sh
prisma playground
```

</Instruction>

This will open a Playground looking as follows:

![](https://imgur.com/HuJfglj.png)

> **Note**: The Playground can be installed on your machine as a [standalone desktop application](https://github.com/graphcool/graphql-playground/releases). If you don't have the Playground installed, the command automatically opens a Playground in your default browser.

One really cool property of GraphQL APIs is that they're effectively _self-documenting_. The [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) defines all the operations of an API, including input arguments and return types. This allows for tooling like the GraphQL Playground to auto-generate API documentation.

<Instruction>

To see the documentation for your service's API, click the green **SCHEMA**-button on the right edge of the Playground window.

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
