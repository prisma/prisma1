---
alias: eeb1ohr4ec
description: Creates the local file structure for a new Graphcool database service
---

# `graphcool init`

Creates the local file structure for a new Graphcool database service:

- `graphcool.yml` which contains the [service definition](!alias-opheidaix3)
- `types.graphql` for your [data model](!alias-eiroozae8u) and other type definitions

If you provide a directory name as an argument to the command, all these files will be placed inside that directory.

#### Usage

```sh
graphcool init DIRNAME
```

#### Examples

##### Create file structure for Graphcool database service in current directory.

```sh
graphcool init
```

##### Create file structure for Graphcool database service in directory called `database`.

```sh
graphcool init database
```
