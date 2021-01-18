---
alias: shoo8cioto
description: Create a new service token
---

# `prisma1 token`

Generate a new [service token](!alias-utee3eiquo#service-token). The token is a [JWT](https://jwt.io) that is signed with the [service secret](!alias-utee3eiquo#service-secret).

#### Usage

```sh
prisma1 token [flags]
```

#### Flags

```
-c, --copy                 Copy token to clipboard
-e, --env-file ENV-FILE    Path to .env file to inject env vars
```

#### Examples

##### Print service token.

```sh
prisma1 token
```

##### Copy service token to clipboard.

```sh
prisma1 token --copy
```