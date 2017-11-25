---
alias:
description: Export data
---

### `graphcool export`

Exports your service data by generating a URL from which you can download a .zip-file that contains all the data from your service.

#### Usage

```sh
graphcool export [flags]
```

#### Flags

```
-t, --target TARGET      Target name
```

#### Examples

##### Export data from default service environment.

```sh
graphcool export
```

##### Export data from a specific service environment.

```sh
graphcool export --target prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
targets:
  default: dev
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
```