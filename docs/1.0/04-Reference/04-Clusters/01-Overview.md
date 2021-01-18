---
alias: eu2ood0she
description: Overview
---

# Overview

Prisma services are deployed to so-called _clusters_. A cluster is a hosted environment for Prisma services.

In essence, there are two kinds of _clusters_ you can deploy to:

- **Self-hosted / local clusters**: Self-hosted clusters are running on Docker. They are created and managed using the Prisma CLI. For the vast majority of use cases, _self-hosted clusters are the preferred option to deploy Prisma services_. This chapter explains how to create and manage your own self-hosted clusters.
- **Development clusters** (based on Prisma Cloud): Allow to conventiently deploy your Prisma service to the web. Note that deploying to development clusters _does not require an account in the Prisma cloud_. Development clusters have certain limitations, such as rate limiting of incoming requests (10 requests per 10 seconds) and an upperbound in storage capacity (100 MB). Services deployed to development clusters are deleted after a certain period of inactivity.

> **Note**: For the vast majority of use cases, hosted clusters are the preferred option to deploy Prisma services. Development clusters should only be used for learning, demo and prototyping purposes.

## Cluster registry

When first used, the Prisma CLI creates a new directory (called `.prisma`) in your home directory. This directory contains the _cluster registry_: `~/.prisma/config.yml`.

The cluster registry lists information about the clusters you can deploy your services to. It is used by the Prisma CLI to provision deployment options to you.

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

If you want to add a custom cluster, you can either use the `prisma cluster add` command or manually add a cluster entry to the file, providing the required information.

You can list your clusters and more information using `prisma cluster list`.

## Cluster deployment

Check the tutorials for setting up the Docker container for the [Local Cluster](alias-meemaesh3k) or on [Digital Ocean](!alias-texoo9aemu).

## Logout

To logout from Prisma CLI remove the `cloudSessionKey` from `~/.prisma/config.yml` file.
