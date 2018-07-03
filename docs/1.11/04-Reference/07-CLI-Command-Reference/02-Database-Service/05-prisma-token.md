---
alias: shoo8cioto
description: Create a new service token
---

# `prisma token`

Generate a new [API token](!alias-utee3eiquo#api-token). The token is a [JWT](https://jwt.io) that is signed with the [API secret](!alias-utee3eiquo#api-secret).

#### Usage

```sh
prisma token [flags]
```

#### Flags

```
-c, --copy                 Copy token to clipboard
-e, --env-file ENV-FILE    Path to .env file to inject env vars
```

#### Examples

##### Print service token.

```sh
prisma token
```

##### Copy service token to clipboard.

```sh
prisma token --copy
```