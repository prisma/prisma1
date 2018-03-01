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

- The `cluster` property in `prisma.yml` is **specified**. In this case, the CLI will directly deploy the specified cluster.
- The `cluster` property in `prisma.yml` is **not specified**. In this case, the CLI will prompt you with an interactive selection of your available clusters. After you selected a cluster, it will write your decision to `prisma.yml`, so the selected cluster will be used as the default cluster for future deploys. To bring up the interactive selection prompt again, you can invoke `prisma deploy --interactive` or simply remove the `cluster` property from `prisma.yml`.

In any case, the value of [`cluster`](!alias-ufeshusai8#clusters-optional) needs to be the identical to the key of an entry in the `clusters` map from the cluster registry (_or_ refer to one of the clusters configured through your Prisma Cloud account).

Consider the above example for a cluster registry. In that case, the following would be valid entries in `prisma.yml` for the `cluster` property:

- Deploy the service to the `local` cluster:

  ```yml(path="prisma.yml")
  cluster: local
  ```

- Deploy the service to the `digital-ocean` cluster:

  ```yml(path="prisma.yml")
  cluster: digital-ocean
  ```

### Adding and removing clusters

If you want to add a custom cluster to the cluster registry, you can either use the `prisma cluster add` command or manually add a cluster entry to the file, providing the required information. Similarly, to delete a cluster you can either run `prisma cluster remove` or  simply remove it from the cluster registry by hand.

You can list your clusters and associated information using `prisma cluster list`. If you're authenticated with the Prisma Cloud, the command will also output the clusters you've configured there.

## Authentication

Clusters are secured using public/private key pairs. The cluster knows the public key. The private key is known locally by the Prisma CLI and used to generate _cluster tokens_. These cluster tokens are used to authenticate requests against the cluster (e.g. an invocation of `prisma deploy`) which can then be validated by the cluster using the public key.

## Logout

To logout from the Prisma CLI remove the `cloudSessionKey` from `~/.prisma/config.yml` file.

<!-- 
![](https://imgur.com/SmHhGDD.png)
-->
