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
-t, --target TARGET      Target name
```

#### Examples

##### Export data from default service environment.

```sh
prisma export
```

##### Export data from a specific service environment.

```sh
prisma export --target prod
```

Assuming you're executing the command in a directory that contains a `.prismarc` looking similar to this:

```yml
targets:
  default: dev
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
```
