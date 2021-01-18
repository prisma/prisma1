---
alias: meemaesh3k
description: Learn how to deploy your Prisma database service to a local cluster.
---

# Local Prisma server with Docker

If you want to develop without internet access or simply prefer having everything locally, you can use Docker to run Prisma locally.

### Installing Docker

You need an up-to-date version of Docker to run Prisma locally. You can find installation instructions for the free Docker Community Edition (CE) on https://www.docker.com/community-edition

### Set up local Prisma server

Use the interactive `init` command to bootstrap configuration for your Prisma server:

```sh
prisma1 init
```

Choose `Create new database` and pick MySQL or Postgres.

This will create a `docker-compose.yml` file with a configuration appropriate for running Prisma locally. 
Now run `docker-compose up -d` to start Prisma and and empty database of your selected type.

This will download the open source Docker images for Prisma as well as your selected database. It can take a while, depending on your internet connection.

Now you can deploy a service to the local cluster:

```
❯ prisma1 deploy

? Please choose the cluster you want to deploy "demo@dev" to (Use arrow keys)

  Deploying service `default` to stage `default` to server `local` 169ms
```

### Upgrading local cluster

New versions of Prisma are released every other week. To upgrade, you should first upgrade your CLI:

```sh
npm -g install prisma1
```

You can now update your Prisma server by manually changing the `docker-compose.yml` file to use the latest version of Prisma:

Before:

```yml
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.11
```

After:

```yml
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.12
```

For the change to take effect, run:

```sh
docker-compose up -d
```

> Note: It is recommended to export your data before upgrading, using the `prisma1 export` command.

If you run into issues during or after upgrading, you can use normal docker commands to remove your docker containers and start from scratch:

- `docker ps` to list your containers
- `docker stop [CONTAINER ID]` to stop a container
- `docker rm [CONTAINER ID]` to remove the container

### Access your database directly

To learn how you can access your MySQL database directly, follow [this](!alias-eechaeth3l) tutorial.
