---
alias: bai9yeitoa
description: Hard-reset local development cluster.
---

# `prisma local nuke`

Hard-reset local development cluster. **This irrevocably wipes all the services including data in your local cluster**.

#### Usage

```sh
prisma local logs [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

#### Examples

##### Nuke local default cluster called `local`.

```sh
prisma local nuke
```

##### Nuke local cluster called `mycluster`.

```sh
prisma local stop --name mycluster
```