---
alias: vu5ohngud5
description: Stop local development cluster
---

# `prisma1 local stop`

Stop local development cluster.

#### Usage

```sh
prisma1 local stop [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

#### Examples

##### Stop local default cluster called `local`.

```sh
prisma1 local stop
```

##### Stop local cluster called `mycluster`.

```sh
prisma1 local stop --name mycluster
```