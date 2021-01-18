---
alias: quungoogh9
description: Download latest Docker image of the local cluster.
---

# `prisma1 local pull`

Download [latest Docker image](https://hub.docker.com/r/prismagraphql/prisma/) for the local cluster using `docker pull`. The command further invokes `docker up` for the specified cluster instance.

#### Usage

```sh
prisma1 local upgrade [flags]
```

#### Flags

```
-n, --name NAME    Name of the cluster instance
```

#### Examples

##### Upgrade local default cluster called `local`.

```sh
prisma1 local upgrade
```

##### Upgrade local cluster called `mycluster`.

```sh
prisma1 local upgrade --name mycluster
```