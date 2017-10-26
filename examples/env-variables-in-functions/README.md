# Environment Variables

## Overview

This directory contains the service definition and file structure for a simple Graphcool service that demonstrates how to use **environment variables inside _functions_**. Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

```
.
├── README.md
├── graphcool.yml
├── src
│   ├── hello.graphql
│   └── hello.js
└── types.graphql
```

> Read more about [service configuration](https://graph.cool/docs/reference/project-configuration/overview-opheidaix3) in the docs.

## Get started

### 1. Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/env-variables
cd env-variables
```

Next, you need to create your GraphQL server using the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go).

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

### 3. Create the GraphQL server

The next step will be to [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory.

To deploy the service and actually create your GraphQL server, invoke the following command:

```sh
graphcool deploy
```

When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.

## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground with the following command:

```sh
graphcool playground
```

### Send the `hello` query _with_ the `name` argument

To test the resolver function, you can send the following query:

```graphql
{
  hello(name: "Sarah") {
    message
  }
}
```

The `message` that's returned in the payload will be: `Hello Sarah`.

### Send the `hello` query _without_ the `name` argument

To test the resolver function, you can send the following query:

```graphql
{
  hello {
    message
  }
}
```

The `message` that's returned in the payload will be: `Hello Alice`. That's because the `NAME` environment variable that's referenced in [`hello.js`](./src/hello.js#L3) is currently [set to `Alice` inside `graphcool.yml`](./graphcool.yml#L18). If no `name` argument is passed in the query, the function will fall back to the value of the environment variable `NAME`.

## What's in this example?

### Setup

This function contains implementations for one [resolver](https://graph.cool/docs/reference/functions/resolvers-su6wu3yoo2) function called `hello`. The corresponding implementation can be found in [`hello.js`](./src/hello.js).

The `schema` which defines the API of the resolver is defined in [`hello.graphql`](./src/hello.graphql).

Both files are referenced from [`graphcool.yml`](./graphcool.yml):

```yml
hello:
  type: resolver
  schema: ./src/hello.graphql
  handler:
    code:
      src: ./src/hello.js
      environment:
        NAME: Alice
```


### Referencing environment variables in _functions_ at runtime

Notice that inside the function definition of `hello` in `graphcool.yml`, there's the `hello.handler.code.environment` property that let's you specify environment variables which can be accessed by your functions at runtime.

In this case, we're setting the value `Alice` for the environment variable `NAME` which is accessed by [`hello.js`](./src/hello.js#L4).












