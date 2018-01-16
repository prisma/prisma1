---
alias: eu2ood0she
description: Overview
---

# Overview

Learn about local and remote Prisma clusters.

## Cluster Registry

The file `~/.prisma/config.yml` contains information about clusters. It might look like this:

```yml
clusters:
  local:
    host: 'http://localhost:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY-----  [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
  do-cluster:
    host: 'http://45.55.177.154:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
```

You can list your clusters and more information using `prisma cluster list`.

To add a cluster, choose it from the interactive selection when deploying a new service, or use `prisma cluster add` to add a new cluster.

## Cluster Deployment

Check the tutorials for setting up the Docker container for the [Local Cluster](meemaesh3k) or on [Digital Ocean](!alias-texoo9aemu).
