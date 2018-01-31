---
alias: eu2ood0she
description: Overview
---

# Overview

Prisma services are deployed to so-called _clusters_. A cluster is a hosted environment for Prisma services.

In essence, there are two kinds of _clusters_ you can deploy your Prisma service to:

- **Self-hosted / local clusters**: Self-hosted (or locally deployed) clusters are running on [Docker](https://www.docker.com). They are created and managed using the Prisma CLI which is governs the underlying Docker _images_ and _containers_ for you.
- **Public clusters** (based on Prisma Cloud): Public clusters allow to conventiently deploy your Prisma service to the web without the overhead of configuring your own cluster. Note that Public clusters have certain limitations, such as rate limiting of incoming requests and an upperbound in storage capacity.

> For the vast majority of use cases, **self-hosted clusters are the preferred option to deploy Prisma services**. This chapter explains how to create and manage your own self-hosted clusters.

## Cluster registry

When first used, the Prisma CLI creates a new directory (called `.prisma`) in your home directory. This directory contains the _cluster registry_: `~/.prisma/config.yml`.

The cluster registry lists information about the clusters you can deploy your services to. It is used by the Prisma CLI to provision deployment options to you when you're running [`prisma deploy`](!alias-kee1iedaov).

### Example

Here is an example of what the cluster registry might look like:

```yml
clusters:
  local:
    host: 'http://localhost:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY-----  [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
  digital-ocean:
    host: 'http://45.55.177.154:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
```

When you're running `prisma deploy` for a Prisma service, there are two scenarios with respect to the target cluster:

- The `cluster` property in `prisma.yml` is specified. In this case, the CLI will directly deploy the specified cluster.
- The `cluster` property in `prisma.yml` is not specified. In this case, the CLI will prompt you with an interactive selection of your available clusters. After you selected a cluster, it will write it to `prisma.yml`. To bring 

In any case, the value of [`cluster`](!alias-ufeshusai8#clusters-optional) needs to be the identical to the key of an entry in the `clusters` map from the cluster registry (_or_ refer to one of the clusters configured through your Prisma Cloud account).

Consider the above example for a cluster registry. In that case, the following would be valid entries in `prisma.yml` for the `cluster` property:

- Deploy the service to the `local` cluster:

  ```yml
  cluster: local
  ```

- Deploy the service to the `digital-ocean` cluster:

  ```yml
  cluster: digital-ocean
  ```

### Adding and removing clusters

If you want to add a custom cluster to the cluster registry, you can either use the `prisma cluster add` command or manually add a cluster entry to the file, providing the required information. Similarly, to delete a cluster you can either run `prisma cluster remove` or  simply remove it from the cluster registry by hand.

You can list your clusters and associated information using `prisma cluster list`. If you're authenticated with the Prisma Cloud, the command will also output the clusters you've configured there.

## Cluster deployment

Prisma services are deployed with the Prisma CLI. Because the Prisma deployment infrastructure is based on [Docker](https://docs.docker.com), a number of commands in the Prisma CLI actually are simple _proxies_ for the [Docker CLI](https://docs.docker.com/engine/reference/commandline/cli). In fact, all of the `prisma local <subcommand>` commands as well as the `prisma cluster logs` command fall into that category.

- `prisma local start`: Starts the Prisma Docker containers by invoking [`docker-compose up`](https://docs.docker.com/compose/reference/up/)
- `prisma local stop`: Stops the Prisma Docker containers by invoking [`docker stop`](https://docs.docker.com/engine/reference/commandline/stop/)
- `prisma local upgrade`: Downloads the [latest Prisma images](https://hub.docker.com/r/prismagraphql/prisma/tags/) from Docker Hub using [`docker pull`](https://docs.docker.com/engine/reference/commandline/pull/)
- `prisma local nuke`: Hard-resets the local development cluster by invoking [`docker-compose kill`](https://docs.docker.com/compose/reference/kill/), [`docker-compose down`](https://docs.docker.com/compose/reference/down/) and [`docker-compose up`](https://docs.docker.com/compose/reference/up/) (in that order)
- `prisma cluster logs`: Shows the logs of the Docker containers using [`docker logs`](https://docs.docker.com/engine/reference/commandline/logs/)

Check the tutorials for setting up the Docker container for the [Local Cluster](alias-meemaesh3k) or on [Digital Ocean](!alias-texoo9aemu).
