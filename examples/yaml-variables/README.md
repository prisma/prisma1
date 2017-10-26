# Variables in `graphcool.yml`

## Overview

This directory contains the service definition and file structure for a simple Graphcool service that makes use of **variables inside `graphcool.yml`**. Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

```
.
├── README.md
├── graphcool.yml
├── src
│   ├── greeting.graphql
│   ├── hello.js
│   └── hey.js
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

However, before you do so, you need to set the environment variable that is referenced in [`graphcool.yml`](./graphcool.yml#L14). This environment variable is called `GREETING` and needs to be set to either `hello` or `hey` to determine whether [`./src/hello.js`](./src/hello.js) or [`./src/hey.js`](./src/hey.js) will be invoked.

#### 3.1. Set environment variable `GREETING`

Depending on your shell, you can set the environment variable in different ways:

##### bash

If you're using [bash](https://en.wikipedia.org/wiki/Bash_(Unix_shell)), use either of the following commands inside this directory:

```sh
export GREETING=hello
# or 
# export GREETING=hey
```

##### fish shell

If you're using [fish shell](https://fishshell.com/), use either of the following commands inside this directory:

```sh
set -x GREETING hello
# or
# set -x GREETING hey
```

##### direnv

If you're using [direnv](https://direnv.net/), create a `.envrc`-file in this directory and add either of the following lines to it:

```sh
export GREETING=hello
# or
# export GREETING=hey
```


#### 3.2. Deploying the service

To deploy the service and actually create your GraphQL server, invoke the following command:

```sh
graphcool deploy
```

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.


## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground with the following command:

```sh
graphcool playground
```

### Send the `greeting` query _with_ the `name` argument

To test the resolver function, you can send the following query:

```graphql
{
  greeting(name: "Sarah") {
    message
  }
}
```

The `message` that's returned in the payload will be: `Hello Sarah`.

### Send the `greeting` query _without_ the `name` argument

To test the resolver function, you can send the following query:

```graphql
{
  greeting {
    message
  }
}
```

The `message` that's returned in the payload will be: `Hello World`. That's because no `name` argument is passed in the query. The function will thus [fall back](./src/hello.js#L5) to the value of the string `World` instead of a concrete name.

## What's in this example?

This function contains implementations for two [resolver](https://graph.cool/docs/reference/functions/resolvers-su6wu3yoo2) functions:

- [`hello.js`](./src/hello.js): Greets the caller with `Hello`
- [`hey.js`](./src/hey.js): Greets the caller with `Hey`


The `schema`s which defines the APIs of both resolver is defined in [`greeting.graphql`](./src/greeting.graphql).

This schema is reference in [`graphcool.yml`](./graphcool.yml):

```yml
greeting:
  type: resolver
  schema: ./src/greeting.graphql
  handler:
    code:
      src: ./src/${env:GREETING}.js
  ```

### Referencing environment variables in `graphcool.yml` at deployment time

Despite the fact that this service contains two function implementations, only _one_ of them will be deployed at any given time! Which one that is depends on the value of the environment variable `GREETING` when `graphcool deploy` is invoked. That's because the `greeting.handler.code.src` property refers to this environment variable: `./src/${env:GREETING}.js`. 

When `graphcool deploy` is called, the CLI will read the value of the environment variable and replace `${env:GREETING}` with it. If the value of `GREETING` is something other than `hello` or `hey`, `graphcool deploy` will fail with the message that the referenced source file does not exist.

