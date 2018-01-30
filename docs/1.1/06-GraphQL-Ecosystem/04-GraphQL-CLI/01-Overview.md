---
alias: quaidah9pj
description: GraphQL CLI is a command line tool for common GraphQL development workflows.
---

# graphql-cli

📟 [`graphql-cli`](https://github.com/graphql-cli/graphql-cli) is a command line tool for common GraphQL development workflows.

## Install

You can simply install the CLI using `npm` or `yarn` by running the following command. This will add the `graphql` (and shorter `gql`) binary to your path.

```sh
npm install -g graphql-cli
```


## Usage
```
Usage: graphql [command]

Commands:
  graphql create [directory]             Bootstrap a new GraphQL project
  graphql add-endpoint                   Add new endpoint to .graphqlconfig
  graphql add-project                    Add new project to .graphqlconfig
  graphql get-schema                     Download schema from endpoint
  graphql schema-status                  Show source & timestamp of local schema
  graphql ping                           Ping GraphQL endpoint
  graphql query <file>                   Run query/mutation
  graphql diff                           Show a diff between two schemas
  graphql playground                     Open interactive GraphQL Playground
  graphql lint                           Check schema for linting errors
  graphql prepare                        Bundle schemas and generate bindings
  graphql codegen [--target] [--output]  Generates apollo-codegen
                                         code/annotations from your
                                         .graphqlconfig

Options:
  --dotenv       Path to .env file                                      [string]
  -p, --project  Project name                                           [string]
  -h, --help     Show help                                             [boolean]
  -v, --version  Show version number                                   [boolean]

Examples:
  graphql init                 Interactively setup .graphqlconfig file
  graphql get-schema -e dev    Update local schema to match "dev" endpoint
  graphql diff -e dev -t prod  Show schema diff between "dev" and "prod"
                               endpoints

```
