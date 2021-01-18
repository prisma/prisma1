---
alias: shaip5yaej
description: Authenticate with Prisma Cloud.
---

# `prisma1 login`

Authenticate with Prisma Cloud. Note that rather than providing your cloud session key via the `--key` flag, you can also set the `PRISMA_CLOUD_SESSION_KEY` environment variable - this is especially useful for CI environments.

#### Usage

```sh
prisma1 login [flags]
```

#### Flags

```
 -k, --key KEY    Cloud session key
```

#### Examples

##### Authenticate with Prisma Cloud (opens browser).

```sh
prisma1 login
```

##### Authenticate with Prisma Cloud by manually passing the cloud session key.

```sh
prisma1 login --key __KEY__
```

> **Note**: In the above command, the `__KEY__` placeholder needs to be replaced with the value of your valid cloud session key. You can find the `cloudSessionKey` in `~/.prisma/config.yml`.