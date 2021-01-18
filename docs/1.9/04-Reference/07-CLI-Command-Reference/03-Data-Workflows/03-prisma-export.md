---
alias: oonu0quai2
description: Export service data to a local file
---

# `prisma1 export`

Exports your service data to a local zip directory.

#### Usage

```sh
prisma1 export [flags]
```

#### Flags

```
-e, --env-file ENV-FILE    Path to .env file to inject env vars
-p, --path PATH            Path to export .zip file
```

#### Examples

##### Export data to file with default name (`export-<timestamp>.zip`).

```sh
prisma1 export
```

##### Export data to file called `mydata.zip`.

```sh
prisma1 export --path mydata.zip
```