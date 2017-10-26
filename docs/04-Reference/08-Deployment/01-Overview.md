---
alias: ohs4asd0pe
description: Information about local deployment workflows with Docker.
---

# Overview

Graphcool services can be deployed in two ways:

- to the [Graphcool Cloud](https://www.graph.cool/cloud)
- locally with [Docker](https://www.docker.com)

## Graphcool Cloud 

### Shared Clusters (Backend-as-a-Service)

Services can be deployed to _shared_ clusters in the Graphcool Cloud. When deploying to a shared cluster, there is a **free developer plan** as well as a convienent and efficient **pay-as-you-go pricing** model for production applications. 

The Graphcool Cloud currently supports three [regions](https://blog.graph.cool/new-regions-and-improved-performance-7bbc0a35c880):

- `eu-west-1` (EU, Ireland)
- `asia-northeast-1` (Asia Pacific, Tokyo)
- `us-west-1` (US, Oregon)

### Private Clusters (coming soon)

The other option when deploying to the Graphcool Cloud is a private cluster that runs on your own AWS account.


## Deployment with Docker

It is possible to have a fully self-contained installation of Graphcool running with Docker on your machine in minutes, to quickly iterate on your implementation without requiring internet access.

Graphcool is a complex system of multiple services working together.
However, we condensed this complexity down to three core services necessary to run Graphcool for an optimal local development experience:

- A **Development server**: Contains all APIs and core business logic.
- A **Database**: A MySQL database containing your data.
- A **local function runtime environment** ("local function-as-a-service"): Allows you to quickly deploy and run your functions locally, without the need for an external FaaS provider.

### Prerequisites

#### Docker 

You need to have [Docker](https://www.docker.com) installed on your machine. If you don't have Docker yet, you can download it under these links:

- For [Mac](https://store.docker.com/editions/community/docker-ce-desktop-mac)
- For [Windows](https://store.docker.com/editions/community/docker-ce-desktop-windows)

You need to configure Docker to have **at least 1 GB of RAM** available: 

![](https://imgur.com/8QysZhe.png)

#### Graphcool CLI

You also need to have the [Graphcool CLI](!alias-zboghez5go) installed:

```sh
npm install -g graphcool
```


### Deploying to a local cluster

#### Creating a Graphcool service

Create a new Graphcool service using the following command:

```sh
# Create a new service definition in the current directory
graphcool init
```

#### Create a local cluster

If you don't have a local cluster defined in your global [`.graphcoolrc`](!alias-zoug8seen4#managing-clusters-in-the-global-.graphcoolrc), you first need to create one using [`graphcool local up`](!alias-aiteerae6l#graphcool-local-up).

This creates a new entry in the `clusters` list in your global `.graphcoolrc` (which is located in your _home_ directory):

```yml
clusters:
  default: shared-eu-west-1
  # Created by `graphcool local up`
  local:
    host: 'http://localhost:60001'
    clusterSecret: >-
      eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDgwODI3NjMsImNsaWVudElkIjoiY2o4bmJ5bjE3MDAvMDAxNzdmNHZzN3FxNCJ9.sOyzwJplYF2x9YHXGVtnd-GneMuzEQauKQC9vLxBag0
```

#### Deploying the service to a local cluster

To [deploy](!alias-aiteerae6l#graphcool-deploy) the service, first run the following command:

```sh
graphcool deploy
```

When prompted which cluster you want to deploy to, choose `local`:

![](https://imgur.com/dP8dSyS.png)

> Note: If you already have a local `.graphcoolrc` file for your service that contains a target, `graphcool deploy` will not prompt you to select a cluster. You can however add the `--interactive` option to the command to enforce the prompt: `graphcool deploy --interactive`.

That's it, your service is now deployed to a local Docker container . Consequently, endpoints that are printed in the output of the `graphcool deploy` command are all targetting `localhost`:

```
Simple API:        http://localhost:60001/simple/v1/__SERVICE_ID__
Relay API:         http://localhost:60001/relay/v1/__SERVICE_ID__
Subscriptions API: ws://localhost:60001/subscriptions/v1/__SERVICE_ID__
```

### Under the hood

With `docker ps` and `docker images` it is possible for you to inspect Graphcool's Docker setup.

You will see three images in your local docker image repository, where `VERSION` will be something like `0.8.1`: 

- `graphcool/graphcool-dev:<VERSION>`: The core apis.
- `graphcool/localfaas:<VERSION>`: The local funtion runtime.
- `mysql:5.7`: The database.
- `rabbitmq:3-management`: A message broker we currently use. (Will be deprecated for local Graphcool soon.)

We use these four images to spin up four containers, which you can see with the aforementioned `docker ps` command. All those containers are in a custom docker network to leverage docker DNS for service discovery.

### More info

- By default, the core APIs bind to port 60000, and the local function runtime binds to 60001. If you start more than one local intance of Graphcool, the CLI will search for the next open port, e.g. 60002, to bind to. You can change the binding of the core APIs by setting a `PORT` env var!
- Your data in the database container and function deployments in the localfaas container are persisted using named docker volumes.
- `docker tail -f CONTAINER_NAME` is useful for peeking into the containers and debugging issues, especially the localfaas container prints plenty of debug output for you to see what is going on, separate from the actual function logs that you can still retrieve with the `graphcool logs` command.


### Debugging

#### Accessing function logs

When deploying [functions](!alias-aiw4aimie9) in a local Docker setup, you can access the function logs directly through [`docker logs`](https://docs.docker.com/engine/reference/commandline/logs/) instead of [`graphcool logs`](!alias-aiteerae6l#graphcool-logs):

```sh
docker logs -f local_localfaas_1
```

#### Accessing Graphcool runtime logs

If your queries and mutation fails, it might be helpful to see what's happening in the Graphcool runtime. You can do so with the following command:

```sh
docker logs -f local_graphcool_1
```

#### Common issues

##### I'm getting a client ID does not exist error

You need a new token in you global `.graphcoolrc` file, which is located in your homefolder by default. 

Locate the `clusterSecret` entry under `local` and replace it with a new token that you can, for example, retrieve via curl: `curl 'http://127.0.0.1:60000/system' -H 'Content-Type: application/json' -d '{"query":"mutation {authenticateCustomer(input:{auth0IdToken:\"MuchTokenSuchMasterWow\"}){token, user{id}}}"}' -sS`

## I'm getting a client ID does not exist error

You need a new token in you global `.graphcoolrc` file, which is located in your homefolder by default.
Locate the `clusterSecret` entry under `local` and replace it with a new token that you can, for example, retrieve via curl: `curl 'http://127.0.0.1:60000/system' -H 'Content-Type: application/json' -d '{"query":"mutation {authenticateCustomer(input:{auth0IdToken:\"MuchTokenSuchMasterWow\"}){token, user{id}}}"}' -sS`


