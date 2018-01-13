---
alias: uz0phae1ph
description: Sign up or login
---

# `prisma login`

Sign up or login (opens your browser for authentication). The platform token that's received after successful login will be stored in `~/.prismarc`.

#### Usage

```sh
prisma login [flags]
```

#### Flags

```
-t, --token TOKEN    System token
```

#### Examples

##### Authenticate using the browser.

```sh
prisma login
```

##### Authenticate using an existing authentication token.

```sh
prisma auth -t <token>
```
