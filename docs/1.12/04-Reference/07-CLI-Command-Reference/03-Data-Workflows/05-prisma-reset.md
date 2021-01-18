---
alias: clchg7lwe1
description: Reset service data
---

# `prisma1 reset`

Reset the service data on the current stage/cluster.

#### Usage

```sh
prisma1 reset [flags]
```

#### Flags

```
 -e, --env-file ENV-FILE    Path to .env file to inject env vars
 -f, --force                Force reset data without confirmation
```

#### Examples

##### Reset service data for current stage/cluster (with confirmation prompt).

```sh
prisma1 reset
```

##### Reset service data for current stage/cluster (without confirmation prompt).

```sh
prisma1 reset --force
```