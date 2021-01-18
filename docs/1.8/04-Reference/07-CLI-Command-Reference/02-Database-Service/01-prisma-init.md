---
alias: eeb1ohr4ec
description: Creates the local file structure for a new Prisma database API
---

# `prisma init`

Creates the local file structure for a new Prisma database API. 

If you provide a directory name as an argument to the command, the generated files will be placed inside a new directory with that name.

<InfoBox type=warning>

Note that in [Prisma 1.7](https://github.com/graphcool/prisma/releases/tag/1.7.0), the `--boilerplate` flag has been removed from `prisma init`. This means you can not bootstrap an entire GraphQL server based on a [GraphQL boilerplate](https://github.com/graphql-boilerplates) project any more.

To bootstrap a GraphQL server based on a GraphQL boilerplate project, use the `graphql create` command from the [GraphQL CLI](https://github.com/graphql-cli/graphql-cli):

```bash
# Install the GraphQL CLI
npm install -g graphql-cli

# Choose a boilerplate from the interactive prompt ...
graphql create myapp 

# ... or directly select a boilerplate project via the `--boilerplate` option (e.g. `typescript-advanced`)
graphql create myapp --boilerplate typescript-advanced
```

</InfoBox>

#### Usage

```sh
prisma init DIRNAME
```

#### Examples

##### Create file structure for Prisma database service in directory called `myapp`.

```sh
prisma init myapp
```
