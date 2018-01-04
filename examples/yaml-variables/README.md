# Variables in `graphcool.yml`

## Overview

This directory contains the service definition and file structure for a simple Graphcool service that makes use of **variables inside `graphcool.yml`**. Read the [last section](#whats-in-this-example) of this README to learn how the different components fit together.

```
.
├── README.md
├── graphcool.yml
└── mymodel.graphql
```

> Read more about [service configuration](https://graph.cool/docs/reference/project-configuration/overview-opheidaix3) in the docs.

## Get started

### 0. Prerequisites: Graphcool CLI

If you haven't already, go ahead and install the Graphcool CLI:

```sh
npm install -g graphcool
# or
# yarn global add graphcool
```

### 1. Download the example

Clone the Graphcool monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/yaml-variables
cd yaml-variables
```

### 2. Deploy the Graphcool database service

The next step will be to [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory.

Before you do so, you can set the environment variable `SERVICE_NAME` which is referenced in [`graphcool.yml`](./graphcool.yml#L7).

#### 2.1. Set environment variable `SERVICE_NAME`

Depending on your shell, you can set the environment variable in different ways:

##### bash

If you're using plain [bash](https://en.wikipedia.org/wiki/Bash_(Unix_shell)), use either of the following commands inside this directory:

```sh
export SERVICE_NAME=my-service
# or
# export SERVICE_NAME=hey
```

##### fish shell

If you're using [fish shell](https://fishshell.com/), use either of the following commands inside this directory:

```sh
set -x SERVICE_NAME my-service
# or
# set -x SERVICE_NAME hey
```

##### direnv

If you're using [direnv](https://direnv.net/), create a `.envrc`-file in this directory and add either of the following lines to it:

```sh
export SERVICE_NAME=my-service
# or
# export SERVICE_NAME=hey
```

#### 2.2. Deploying the service

To deploy the service, invoke the following command:

```sh
graphcool deploy
```

You can also pass the `--stage` option to adjust the suffix of the service name, as specified in [`graphcool.yml`](./graphcool.yml#L7):

```sh
graphcool deploy --stage prod
```

The second command will deploy a service named `my-service-prod`. When the `--stage` option is not set, the CLI will fall back to the default value `dev` that's also specified in [`graphcool.yml`](./graphcool.yml#L7).

## Testing the service

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can open a Playground with the following command:

```sh
graphcool playground
```

## What's in this example?

TODO