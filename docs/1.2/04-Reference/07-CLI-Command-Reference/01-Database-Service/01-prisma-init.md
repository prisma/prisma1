---
alias: eeb1ohr4ec
description: Creates the local file structure for a new Prisma database service
---

# `prisma1 init`

Creates the local file structure for a new Prisma database service. If no `--boilerplate` option is provided, the command will trigger an interactive prompt allowing you to choose a preconfigured template for your service. There are two major options for such a template:

- `Minimal setup: database-only`: Provides with you with a plain Prisma database service
- `GraphQL server/fullstack boilerplate`: Bootstraps the foundation for a fully-fledged GraphQL app (backend-only or fullstack) based on [GraphQL boilerplates](https://github.com/graphql-boilerplates)

In any case, the bootstrapped Prisma service will at minimum contain these files:

- `prisma.yml` contains the [service definition](!alias-opheidaix3)
- `datamodel.graphql` contains the definition of your [data model](!alias-eiroozae8u)
- `.graphqlconfig` is a configuration file following the standardized [`graphql-config`](https://github.com/graphcool/graphql-config) format and is used by various tools, e.g. the [GraphQL Playground](https://github.com/graphcool/graphql-playground)

If you provide a directory name as an argument to the command, the generated files will be placed inside a new directory with that name.

#### Usage

```sh
prisma1 init DIRNAME [flags]
```

#### Flags

```
 -b, --boilerplate BOILERPLATE    Full URL or repo shorthand (e.g. `owner/repo`) to boilerplate GitHub repository
```

#### Examples

##### Create file structure for Prisma database service in current directory.

```sh
prisma1 init
```

##### Create file structure for Prisma database service in directory called `database`.

```sh
prisma1 init database
```

##### Bootstrap file for GraphQL server based on `node-basic` boilerplate in directory called `node-app`.

```sh
prisma1 init node-app --boilerplate node-basic
```