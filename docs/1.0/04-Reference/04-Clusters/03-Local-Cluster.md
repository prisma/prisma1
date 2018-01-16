---
alias: si4aef8hee
description: Overview
---

# Local Cluster

This chapter describes advanced topics for the local Prisma cluster.

If you want to develop without internet access or simply prefer having everything locally, you can use `prisma local` to set up a local Prisma cluster with Docker.

### Installing docker

You need an up-to-date version of Docker to run Prisma locally. You can find installation instructions for the free Docker Community Edition (CE) on https://www.docker.com/community-edition

### Set up local cluster

Getting started is as simple as running:

```sh
prisma local start
```

This will download the open source Docker images for Prisma and start two Docker containers for Prisma and MySQL. It can take a while, depending on your internet connection.

Now you can deploy a servie to the Local cluster:

```sh
❯ prisma deploy

? Please choose the cluster you want to deploy "demo@dev" to (Use arrow keys)

  prisma-eu1      Free development cluster (hosted on Prisma Cloud)
  prisma-us1      Free development cluster (hosted on Prisma Cloud)
❯ local           Local cluster (requires Docker)
```

### Troubleshooting

You can view the current version of Prisma you are running by listing all available clusters:

```sh
> prisma cluster list

name            version        endpoint
──────────────  ─────────────  ──────────────────────────────────
local           1.0.0-beta4.2  http://localhost:4466/cluster
```

## Database Access (SQL)

You can connect directly to the MySQL database powering your local Prisma cluster.

If you used `prisma local start` to start your local Prisma cluster, you will have two containers running:

`prisma` is running the main Prisma service
`prisma-db` is running the MySQL server that stores your data

This guide explains how to connect to your local MySQL server in order to query and update data directly.

## Debugging

You can view logs from your local Prisma cluster to debug issues.

### Logs

you can view normal debug logs:

```sh
prisma local logs
```

### Docker logs

If you need more extensive logs you can view the raw logs from the containers running MySQL and Prisma:

```sh
docker logs prisma

docker logs prisma-db
```

### Verify Docker containers

If you get an error message saying `Error response from daemon: No such container` you can verify that the containers are running:

```sh
docker ps
```

You should see output similar to this:

```
❯ docker ps
CONTAINER ID  IMAGE                       COMMAND                 CREATED            STATUS            PORTS                   NAMES
7210106b6650  prismagraphql/prisma:1.0.0  "/app/bin/single-ser…"  About an hour ago  Up About an hour  0.0.0.0:4466->4466/tcp  prisma
1c15922e15ba  mysql:5.7                   "docker-entrypoint.s…"  About an hour ago  Up About an hour  0.0.0.0:3306->3306/tcp  prisma-db
```

### Nuke

If your local prisma cluster is in an unrecoverable state, the easiest option might be to completely reset it. Be careful as this command will reset all data in your local cluster.

```sh
❯ prisma local nuke
Nuking local cluster 10.9s
Booting fresh local development cluster 18.4s
```
