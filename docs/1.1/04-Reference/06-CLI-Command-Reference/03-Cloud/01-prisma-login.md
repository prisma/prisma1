---
alias: shaip5yaej
description: Authenticate with Prisma Cloud.
---

# `prisma login`

Authenticate with Prisma Cloud.

#### Usage

```sh
prisma login [flags]
```

#### Flags

```
 -k, --key KEY    Cloud session key
```

#### Examples

##### Authenticate with Prisma Cloud (opens browser).

```sh
prisma login
```

##### Authenticate with Prisma Cloud by manually passing the cloud session key.

```sh
prisma login --key __KEY__
```

> **Note**: In the above command, the `__KEY__` placeholder needs to be replaced with the value of your valid cloud session key. You can find the `cloudSessionKey` in `~/.prisma/config.yml`.