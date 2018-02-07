---
alias: vu5ohngud5
description: Stop local development cluster
---

# `prisma local stop`

Stop local development cluster.

#### Usage

```sh
prisma local stop [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

#### Examples

##### Stop local default cluster called `local`.

```sh
prisma local stop
```

##### Stop local cluster called `mycluster`.

```sh
prisma local stop --name mycluster
```