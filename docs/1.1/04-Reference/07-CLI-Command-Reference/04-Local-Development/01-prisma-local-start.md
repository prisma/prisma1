---
alias: woh9sheith
description: Start local development cluster (Docker required)
---

# `prisma local start`

Start local development cluster ([Docker](https://www.docker.com) required).

#### Usage

```sh
prisma local start [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

#### Examples

##### Start local default cluster called `local`.

```sh
prisma local start
```

##### Start local cluster called `mycluster`.

```sh
prisma local start --name mycluster
```

> **Note**: If it didn't exist before, the command adds a new entry with the key `mycluster` to your [cluster registry](!alias-eu2ood0she#cluster-registry) in `~/.prisma/config.yml`.