---
alias: eizai4ahno
description: Delete an existing service from the cluster its deployed to
---

# `prisma delete`

Delete an existing service from the cluster its deployed to. Note that this command needs to be executed inside the root directory of the Prisma service you want to delete (so the CLI has access to `prisma.yml`).

#### Usage

```sh
prisma delete [flags]
```

#### Flags

```
 -e, --env-file ENV-FILE    Path to .env file to inject env vars
 -f, --force                Force delete, without confirmation
```

#### Examples

##### Delete an existing service (with confirmation prompt).

```sh
prisma delete
```

##### Delete an existing service (without confirmation prompt).

```sh
prisma delete --force
```