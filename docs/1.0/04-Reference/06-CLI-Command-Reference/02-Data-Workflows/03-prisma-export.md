---
alias: oonu0quai2
description: Export service data to a local file
---

# `prisma1 export`

Exports your service data.

#### Usage

```sh
prisma1 export [flags]
```

#### Flags

```
--dotenv DOTENV           Path to .env file to inject env vars
```

#### Examples

##### Export data

```sh
prisma1 export
```

##### Export data with specific environment variables

```sh
prisma1 export --dotenv .env.prods
```
