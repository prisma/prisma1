---
alias: meemaesh3k
description: Learn how to deploy your Prisma database service to a local cluster.
---

# Local Cluster (Docker)

This guide describes advanced topics for the local Prisma cluster.

If you want to develop without internet access or simply prefer having everything locally, you can use `prisma local` to set up a local Prisma cluster with Docker.

### Installing Docker

You need an up-to-date version of Docker to run Prisma locally. You can find installation instructions for the free Docker Community Edition (CE) on https://www.docker.com/community-edition

### Set up local cluster

Getting started is as simple as running:

```sh
prisma local start
```

This will download the open source Docker images for Prisma and start two Docker containers for Prisma and MySQL. It can take a while, depending on your internet connection.

Now you can deploy a service to the local cluster:

```
❯ prisma deploy

? Please choose the cluster you want to deploy "demo@dev" to (Use arrow keys)

  prisma-eu1      Free development cluster (hosted on Prisma Cloud)
  prisma-us1      Free development cluster (hosted on Prisma Cloud)
❯ local           Local cluster (requires Docker)
```

This will add a `cluster` entry to the `prisma.yml` with the cluster you chose.

### Upgrading local cluster

To upgrade the local cluster, you can run:

```sh
prisma local upgrade
```

> Note: It is recommended to export your data before upgrading, using the `prisma export` command.

If you run into issues during or after upgrading, you can [nuke the local cluster](!alias-si4aef8hee), wiping all data in the process.

### Cluster Information

You can view the current version of Prisma you are running by listing all available clusters:

```
> prisma cluster list

name            version        endpoint
──────────────  ─────────────  ──────────────────────────────────
local           1.0.0-beta4.2  http://localhost:4466/cluster
```

> To learn how you can directly your MySQL database, follow [this](!alias-eechaeth3l) tutorial.