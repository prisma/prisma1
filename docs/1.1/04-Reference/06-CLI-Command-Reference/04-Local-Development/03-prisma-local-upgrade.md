---
alias: quungoogh9
description: Download latest Docker image of the local cluster.
---

# `prisma local pull`

Download [latest Docker image](https://hub.docker.com/r/prismagraphql/prisma/) for the local cluster using `docker pull`. The command further invokes `docker up` for the specified cluster instance.

#### Usage

```sh
prisma local upgrade [flags]
```

#### Flags

```
-n, --name NAME    Name of the cluster instance
```

#### Examples

##### Upgrade local default cluster called `local`.

```sh
prisma local upgrade
```

##### Upgrade local cluster called `mycluster`.

```sh
prisma local upgrade --name mycluster
```