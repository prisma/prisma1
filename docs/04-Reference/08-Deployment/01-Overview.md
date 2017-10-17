---
alias: ohs4asd0pe
description: Information about local deployment workflows with Docker.
---

# Overview

## Prerequisites

### Docker 

You need to have [Docker](https://www.docker.com) installed on your machine. If you don't have Docker yet, you can download it under these links:

- For [Mac](https://store.docker.com/editions/community/docker-ce-desktop-mac)
- For [Windows](https://store.docker.com/editions/community/docker-ce-desktop-windows)

You need to configure Docker to have **at least 1 GB of RAM** available: 

![](https://imgur.com/8QysZhe.png)

### Graphcool CLI

You also need to have the [Graphcool CLI](!alias-zboghez5go) installed:

```sh
npm install -g graphcool@next
```


## Deploying to a local cluster

### Creating a Graphcool service

Create a new Graphcool service using the following command:

```sh
# Create a new service definition in the current directory
graphcool init
```

### Create a local cluster

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

### Deploying the service to a local cluster

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

## Debugging

### Accessing function logs

When deploying [functions](!alias-aiw4aimie9) in a local Docker setup, you can access the function logs directly through [`docker logs`](https://docs.docker.com/engine/reference/commandline/logs/) instead of [`graphcool logs`](!alias-aiteerae6l#graphcool-logs):

```sh
docker logs -f local_localfaas_1
```

### Accessing Graphcool runtime logs

If your queries and mutation fails, it might be helpful to see what's happening in the Graphcool runtime. You can do so with the following command:

```sh
docker logs -f local_graphcool_1
```


## More info

The Graphcool CLI commands that start with `graphcool local` commands are effectively a _proxy_ to the [Docker CLi](https://docs.docker.com/engine/reference/commandline/cli/):

- `graphcool local up` starts the Docker containers that contain the Graphcool runtime (run `graphcool local eject` if you need access to the [Docker Compose](https://docs.docker.com/compose/) file)
- `graphcool local stop` stops the Docker containers
- `graphcool local restart` stops and restarts the Docker containers


