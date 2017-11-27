---
alias: de0eer2quu
description: Open a GraphQL Playground
---

# `graphcool console`

Open the console for the current service. The current service is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage

```sh
graphcool console [flags]
```

#### Flags

```
-t, --target TARGET      Target name
```

#### Examples

##### Open the console for the current service.

```sh
graphcool console
```

##### Open the console for the `prod` environment.

```sh
graphcool console [flags]
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
```