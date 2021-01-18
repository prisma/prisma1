---
alias: bai9yeitoa
description: Hard-reset local development cluster.
---

# `prisma1 local nuke`

Hard-reset local development cluster. **This irrevocably wipes all the services including data in your local cluster**.

#### Usage

```sh
prisma1 local logs [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

#### Examples

##### Nuke local default cluster called `local`.

```sh
prisma1 local nuke
```

##### Nuke local cluster called `mycluster`.

```sh
prisma1 local stop --name mycluster
```