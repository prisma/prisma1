---
alias:
description: Creates the local file structure for a new Graphcool service
---

### `graphcool init`

Creates the local file structure for a new Graphcool service:

- `graphcool.yml` which contains the [service definition](!alias-opheidaix3)
- `types.graphql` for your [data model](!alias-eiroozae8u) and other type definitions
- `src` (directory) with a default "Hello World" function

If you provide a directory to the command, all these files will be placed inside that directory.

#### Usage

```sh
graphcool init DIRNAME
```

#### Examples

##### Create file structure for Graphcool service in current directory.

```sh
graphcool init
```

##### Create file structure for Graphcool service in directory called `server`.

```sh
graphcool init server
```
