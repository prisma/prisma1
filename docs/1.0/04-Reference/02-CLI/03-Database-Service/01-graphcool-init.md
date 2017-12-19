---
alias: eeb1ohr4ec
description: Creates the local file structure for a new Graphcool database service
---

# `graphcool init`

Creates the local file structure for a new Graphcool database service:

- `graphcool.yml` contains the [service definition](!alias-opheidaix3)
- `datamodel.graphql` contains the definition of your [data model](!alias-eiroozae8u) 
- `.graphqlconfig` is a configuration file following the standardized [`graphql-config`](https://github.com/graphcool/graphql-config) format and is used by various tools, e.g. the [GraphQL Playground](https://github.com/graphcool/graphql-playground)

If you provide a directory name as an argument to the command, all these files will be placed inside a new directory with that name.

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
