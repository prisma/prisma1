---
alias: quaidah9pi
description: GraphQL Config is the easiest way to configure a development environment with a GraphQL schema (supported by most tools, editors &amp; IDEs).
---

# graphql-config

[`graphql-config`](https://github.com/graphcool/graphql-config) is the easiest way to configure your development environment with your GraphQL schema (supported by most tools, editors &amp; IDEs)

## How it works

This project aims to be provide a unifying configuration file format to configure your GraphQL schema in your development environment.

Additional to the format specification, it provides the [`graphql-config`](#graphql-config-api) library, which is used by [all supported tools and editor plugins](#supported-by). The library reads your provided configuration and passes the actual GraphQL schema along to the tool which called it.

## Usage

Install [`graphql-cli`](https://github.com/graphcool/graphql-cli) and run `graphql init`. Answer a few simple questions and you are set up!

You can either configure your GraphQL endpoint via a configuration file `.graphqlconfig`
(or `.graphqlconfig.yaml`) which should be put into the root of your project

### Simplest use case

The simplest config specifies only `schemaPath` which is path to the file with introspection
results or corresponding SDL document

```json
{
  "schemaPath": "schema.graphql"
}
```

or

```json
{
  "schemaPath": "schema.json"
}
```

### Specifying includes/excludes files

You can specify which files are included/excluded using the corresponding options:

```json
{
  "schemaPath": "schema.graphql",
  "includes": ["*.graphql"],
  "excludes": ["temp/**"]
}
```

> Note: `excludes` and `includes` fields are globs that should match filename.
> So, just `temp` or `temp/` won't match all files inside the directory.
> That's why the example uses `temp/**`

#### Specifying endpoint info

You may specify your endpoints info in `.graphqlconfig` which may be used by some tools.
The simplest case:

```json
{
  "schemaPath": "schema.graphql",
  "extensions": {
    "endpoints": {
      "dev": "https://example.com/graphql"
    }
  }
}
```

In case you need provide additional information, for example headers to authenticate your GraphQL endpoint or
an endpoint for subscription, you can use expanded version:

```json
{
  "schemaPath": "schema.graphql",
  "extensions": {
    "endpoints": {
      "dev": {
        "url": "https://example.com/graphql",
        "headers": {
          "Authorization": "Bearer ${env:AUTH_TOKEN_ENV}"
        },
        "subscription": {
          "url": "ws://example.com/graphql",
          "connectionParams": {
            "Token": "${env:YOUR_APP_TOKEN}"
          }
        }
      }
    }
  }
}
```

> Note: do not save secure information in .graphqlconfig file. Use [Environment variables](https://github.com/graphcool/graphql-config/blob/master/specification.md#referencing-environment-variables) for that like in the example above.

In case if you have multiple endpoints use the following syntax:

```json
{
  "schemaPath": "schema.graphql",
  "extensions": {
    "endpoints": {
      "prod": {
        "url": "https://your-app.com/graphql",
        "subscription": {
          "url": "wss://subscriptions.graph.cool/v1/instagram"
        }
      },
      "dev": {
        "url": "http://localhost:3000/graphql",
        "subscription": {
          "url": "ws://localhost:3001"
        }
      }
    }
  }
}
```
