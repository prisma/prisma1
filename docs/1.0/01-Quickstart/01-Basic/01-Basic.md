---
alias: xil8ahdayo
description: Learn the fundamentals of using Graphcool.
---

# Graphcool Basics

In this tutorial, you'll learn how to get started with Graphcool as a "GraphQL database" service.

Here's an overview of what we are going to cover:

- Creating a new Graphcool service
- Changing the data model (schema) of a Graphcool service
- Understanding the relationship between your data model and the generated GraphQL API
- Send queries and mutations to the service's API in a GraphQL Playground

> Note that this tutorial really only covers the basics of using Graphcool. To learn how to use Graphcool with a specific framework or programming language (like React, TypeScript or Node.js), please refer to the other quickstart tutorials.

## Installing the Graphcool CLI

Graphcool services are managed with the [Graphcool CLI](!alias-ieshoo5ohm). You can install it using `npm` (or `yarn`).

<Instruction>

Open your terminal and run the following command to install the Graphcool CLI:

```sh
npm install -g graphcool
# or
# yarn global add graphcool
```

</Instruction>

## Bootstraping a Graphcool service

A Graphcool service requires at least two files so it can be deployed:

- [`graphcool.yml`](!alias-foatho8aip): The root configuration file for your service. It contains information about your service, like the name (which is used to generate the service's HTTP endpoint), a secret to secure the access to the endpoint and about where it should be deployed.
- `datamodel.graphql` (can also be called differently, e.g. `types.graphql`): This file contains the definition of your [data model](!alias-eiroozae8u), written in [GraphQL SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51).

To get started with your own Graphcool service, you can either create these files yourself and configure them accordingly - or you can use the `graphcool init` command from the Graphcool CLI to bootstrap a minimal setup for your GraphQL server.

<Instruction>

Open a terminal and navigate to a folder of your choice. Then bootstrap your Graphcool service with the following command:

```sh
graphcool init hello-world
```

</Instruction>

This will create a new directory called `hello-world` as well as the two files mentioned above to provide a minimal setup for the service.

> **Note**: The `hello-world` directory actually contains a third file as well: `.graphqlconfig`. This file follows the industry standard for configuring and structuring GraphQL projects (based on [`graphql-config`](https://github.com/graphcool/graphql-config)). If present, it is used by GraphQL tooling (such as the GraphQL Playground, the [`graphql-cli`](https://github.com/graphql-cli/graphql-cli/), text editors, build tools and others) to improve your local developer workflows.

Let's take a look at the contents of the generated files:

**`graphcool.yml`**

```yml
service: hello-world
stage: dev

datamodel: datamodel.graphql

# to enable auth, provide
# secret: my-secret
disableAuth: true
```

Here's an overview of the properties in the generated `graphcool.yml`:

- `service`: Defines the service name which will be part of the service's HTTP endpoint
- `stage`: A service can be deployed to multiple stages (e.g. a _development_ and a _production_ environment)
- `datamodel`: The path to the file which contains your data model
- `disableAuth`: If set to true, everyone who knows the endpoint of your Graphcool service has full read and write access. If set to `false`, you need to specify a `secret` which is used to generate JWT authentication tokens. These tokens need to be attached to the `Authorization` header of the requests sent to your service.

> **Note**: We'll keep `disableAuth` set to `true` for this tutorial. In production applications, you'll always want to require authentication for your service! You can read more about this topic [here](!alias-pua7soog4v).

**`datamodel.graphql`**

```graphql
type User {
  id: ID! @unique
  name: String!
}
```

The datamodel contains type definitions for the entities in your application domain. In this case, you're starting out with a very simple `User` type with an `id` and a `name`.

The `@unique` directive here expresses that no two users in the database can have the same `id`, Graphcool service will ensure this requirement is met at all times.

## Deploying your Graphcool service

`graphcool.yml` and `datamodel.graphql` are your abstract _service definition_. To actually create an instance of this service that can be invoked via HTTP, you need to _deploy_ it.

<Instruction>

Inside the `hello-world` directory in your terminal, run the following command:

```sh
graphcool deploy
```

</Instruction>

Since `graphcool.yml` doesn't yet contain the information about _where_ (meaning to which `cluster`) your service should be deployed, the CLI triggers a prompt for you to provide this information. At this point, you can choose to either deploy it locally with [Docker](https://www.docker.com) (which of course requires you to have Docker installed on your machine) or to a public Graphcool cluster. You'll use a public cluster for the purpose of this tutorial.

<Instruction>

Select the `shared-public-demo` option in the **Shared Clusters** section.

</Instruction>

Your Graphcool service is now deployed and ready to accept your queries and mutations 🎉