---
alias: oonu0quai2
description: Export service data to a local file
---

# `prisma export`

Exports your service data.

#### Usage

```sh
prisma export [flags]
```

#### Flags

```
--dotenv DOTENV           Path to .env file to inject env vars
```

#### Examples

##### Export data

```sh
prisma export
```

##### Export data with specific environment variables

```sh
prisma export --dotenv .env.prods
```
