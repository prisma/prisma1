---
alias: eu2ood0she
description: Overview
---

# Overview

Learn about local and remote Prisma clusters.

## Cluster Registry

The file `~/.prisma/config.yml` contains information about clusters and your `clusterSessionKey`. It might look like this:

```yml
clusterSessionKey: eyJ************
clusters:
  local:
    host: 'http://localhost:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY-----  [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
  do-cluster:
    host: 'http://45.55.177.154:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
```

The file is automatically created by the `prisma` cli.

If you just want to deploy to the shared cluster, you don't have to care about this file.

If you want to add a custom cluster, you can either use the `prisma cluster` command or add a cluster by hand to this file.

The `clusterSessionKey` is used to authenticate your account with the Prisma Cloud API.

You can also provide it as the environment variable `PRISMA_CLUSTER_SESSION_KEY`.
If both `PRISMA_CLUSTER_SESSION_KEY` env var  and the `clusterSessionKey` key in the `~/.prisma/config.yml` are present, the `PRISMA_CLUSTER_SESSION_KEY` has higher precedence.

You can list your clusters and more information using `prisma cluster list`.

To add a cluster, choose it from the interactive selection when deploying a new service, or use `prisma cluster add` to add a new cluster.

## Cluster Deployment

Check the tutorials for setting up the Docker container for the [Local Cluster](meemaesh3k) or on [Digital Ocean](!alias-texoo9aemu).
