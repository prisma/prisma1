---
alias: anaif5iez3
description: Open the GraphQL Playground
---

# `prisma1 playground`

Open a [GraphQL Playground](https://github.com/graphcool/graphql-playground) for the current service. By default, this open the Desktop version of the Playground (if installed). The browser-based Playground can be forced by passing the `--web` flag. The Playground is running on port `3000`.

#### Usage

```sh
prisma1 playground [flags]
```

#### Flags

```
--dotenv DOTENV          Path to .env file to inject env vars
-w, --web                Open browser-based Playground
```

#### Examples

##### Open Playground (Desktop version, if installed)

```sh
prisma1 playground
```

##### Open Playground (browser-based version)

```sh
prisma1 playground --web
```