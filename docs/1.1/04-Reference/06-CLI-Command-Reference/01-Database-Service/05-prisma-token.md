---
alias: shoo8cioto
description: Create a new service token
---

# `prisma token`

Generate a new service token. The token is a JWT that is signed with the [service secret](!alias-ufeshusai8#secret-optional).

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

##### Copy token to clipboard.

```
prisma token --copy
```