---
alias: ufeshusai8
description: YAML Structure
---

# YAML Structure

## Overview

The service definition file `prisma.yml` has the following root properties:

- `datamodel` (required): Type definitions for database models, relations, enums and other types.
- `endpoint`: HTTP endpoint for the Prisma API. Can be omitted to prompt CLI deployment wizard.
- `secret`: Secret for securing the API endpoint.
- `schema`: Path to the GraphQL schema for the Prisma API.
- `subscriptions`: Configuration of subscription webhooks.
- `seed`: Points to a file containing mutations for data seeding.
- `custom`: Used to provide variables which can be referenced elsewhere in `prisma.yml`.

> The exact structure of `prisma.yml` is defined with [JSON schema](http://json-schema.org/). You can find the corresponding schema definition [here](https://github.com/graphcool/graphcool-json-schema/blob/master/src/schema.json).

## `datamodel` (required)

The `datamodel` points to one or more `.graphql`-files containing type definitions written in [GraphQL SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51). If multiple files are provided, the CLI will simply concatenate their contents at deployment time.

#### Type

The `datamodel` property expects a **string** or a **list of strings**.

#### Examples

The data model is defined in a file called `types.graphql`.

```yml
datamodel: types.graphql
```

The data model is defined in two files called `types.graphql` and `enums.graphl`. When deployed, the contents of both files will be concatenated by the CLI.

```yml
datamodel:
  - types.graphql
  - enums.graphql
```

## `endpoint` (optional)

The HTTP endpoint for your Prisma API is composed of the following components:

- **Prisma server**: The server that will host your Prisma API.
- **Workspace** (only Prisma Cloud): The name of the Workspace you configured through Prisma Cloud.
- **Service name**: A descriptive name for your Prisma API.
- **Stage**: The development stage of your cluster (e.g. `dev`, `staging`, `prod`).

Note that the `endpoint` is actually required to deploy your Prisma API. However, if you don't specify it in `prisma.yml` before running `prisma1 deploy`, the CLI will use a wizard to prompt you with a few questions and add the `endpoint` to `prisma.yml` for you.

#### Type

The `endpoint` property expects a **string**.

#### Examples

The following example endpoint encodes this information:

- **Prisma server**: `localhost:4466` means you're using Docker to deploy the API on your local machine (on port `4466`).
- **Service name**: `default`
- **Stage**: `default`

> **Note**: When service name and stage are both set to `default`, they can be omitted and will be inferred by Prisma. This means this example endpoint is equivalent to writing: `http://localhost:4466/`

```yml
endpoint: http://localhost:4466/default/default
```

The following example endpoint encodes this information:

- **Prisma server**: `eu1.prisma.sh` means you're using a Prisma Sandbox to deploy your Prisma API.
- **Workspace**: `public-helixgoose-752` is a randomly generated string that identifies the Prisma Cloud workspace for your Sandbox.
- **Service name**: `myservice`
- **Stage**: `dev`

```yml
endpoint: https://eu1.prisma.sh/public-helixgoose-752/myservice/dev
```

The following example endpoint encodes this information:

- **Prisma server**: `http://my-pr-Publi-1GXX8QUZU3T89-413349553.us-east-1.elb.amazonaws.com` means you're using a custom server to deploy your Prisma API.
- **Service name**: `cat-pictures`
- **Stage**: `prod`

```yml
endpoint: http://my-pr-Publi-1GXX8QUZU3T89-413349553.us-east-1.elb.amazonaws.com/cat-pictures/prod
```

## `secret` (optional)

A secret is used to generate (or _sign_) authentication tokens ([JWT](https://jwt.io)). One of these authentication tokens needs to be attached to the HTTP request (in the `Authorization` header field). A secret must follow these requirements:

- must be [utf8](https://en.wikipedia.org/wiki/UTF-8) encoded
- must not contain spaces
- must be at most 256 characters long

Note that it's possible to encode multiple secrets in this string, which allows for smooth secret rotations.

Read more about Database [authentication here](!alias-utee3eiquo#authentication).

<InfoBox type=warning>

**WARNING**: If the Prisma API is deployed without a `secret`, it does not require authentication. This means everyone with access to the `endpoint` is able to send arbitrary queries and mutations and therefore read and write to the database!

</InfoBox>

#### Type

The `secret` property expects a **string** (not a list of strings). If you want to specify multiple secrets, you need to provide them as a comma-separated list (spaces are ignored), but still as a single string value.

#### Examples

Define one secret with value `moo4ahn3ahb4phein1eingaep`.

```yml
secret: moo4ahn3ahb4phein1eingaep
```

Define three secrets with values `myFirstSecret`, `SECRET_NUMBER_2` and `3rd-secret`. Note that the spaces before the second secret are ignored.

```yml
secret: myFirstSecret,    SECRET_NUMBER_2,3rd-secret
```

Use the value of the `MY_SECRET` environment variable as the secret(s).

```yml
secret: ${env:MY_SECRET}
```

## `subscriptions` (optional)

The `subscriptions` property is used to define all the subscription webhooks for your Prisma service. A subscription needs (at least) two pieces of information:

- a _subscription query_ that defines upon which event a function should be invoked and what the payload looks like
- the URL of a _webhook_ which is invoked via HTTP once the event happens
- (optionally) a number of HTTP headers to be attached to the request that's sent to the URL

#### Type

The `subscriptions` property expects an **object** with the following properties:

- `query` (required): The file path to the _subscription query_.
- `webhook` (required): Information (URL and optionally HTTP headers) about the webhook to be invoked. If there are no headers, you can provide the URL to this property directly (see first example below). Otherwise, `webhook` takes another object with properties `url` and `headers` (see second example below).

#### Examples

Specify one event subscription without HTTP headers.

```yml
subscriptions:
  sendWelcomeEmail:
      query: database/subscriptions/sendWelcomeEmail.graphql
      webhook: https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev/sendWelcomeEmail
```

Specify one event subscription with two HTTP headers.

```yml
subscriptions:
  sendWelcomeEmail:
      query: database/subscriptions/sendWelcomeEmail.graphql
      webhook:
        url: https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev/sendWelcomeEmail
        headers:
          Authorization: ${env:MY_ENDPOINT_SECRET}
          Content-Type: application/json
```

## `seed` (optional)

Database seeding is a standardised way to populate a service with test data.

#### Type

The `seed` property expects an **object**, with either one of two sub-properties:

* `import`: instructions to import data when seeding a service. You can refer to two kinds of files:
  * either a path to a `.graphql` file with GraphQL operations
  * or a path to a `.zip` file that contains a data set in [Normalized Data Format (NDF)](!alias-teroo5uxih)
* `run`: shell command that will be executed when seeding a service. This is meant for more complex seed setups that are not covered by `import`.

> Note: `run` is currently not supported. Follow [the proposal](https://github.com/graphcool/framework/issues/1181) to stay informed.

Seeds are implicitly executed when deploying a service for the first time (unless explicitly disabled using the `--no-seed` flag). Track [this feature request for additional seeding workflows](https://github.com/graphcool/prisma/issues/1536).

#### Examples

Refer to a `.graphql` file containing seeding mutations:

```yml
seed:
  import: database/seed.graphql
```

Refer to a `.zip` file with a data set in NDF:

```yml
seed:
  import: database/backup.zip
```

Run a Node script when seeding:

```yml
seed:
  run: node script.js
```

## `custom` (optional)

The `custom` property lets you specify any sorts of values you want to reuse elsewhere in your `prisma.yml`. It thus doesn't have a predefined structure. You can reference the values using variables with the [`self` variable source](!alias-nu5oith4da#self-references), e.g.: `${self:custom.myVariable}`.

#### Type

The `custom` property expects an **object**. There are no assumptions about the shape of the object.

#### Examples

Define two custom values and reuse them in the definition of the event subscription.

```yml
custom:
  serverlessEndpoint: https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev
  subscriptionQueries: database/subscriptions/

subscriptions:
  sendWelcomeEmail:
    query: ${self:custom.subscriptionQueries}/sendWelcomeEmail.graphql
    webhook: https://${self:custom.serverlessEndpoint}/sendWelcomeEmail
```

## `hooks` (optional)

The `hooks` property is used to define terminal commands which will be executed by the Prisma CLI before or after certain commands.

The following hooks are currently available:

- `post-deploy`: Will be invoked _after_ the `prisma1 deploy` command

#### Type

The `hooks` property expects an **object**. The properties match the names of the currently availale hooks.

#### Examples

Here is an example that performs three tasks after `prisma1 deploy` was executed:

1. Print "Deployment finished"
1. Download the GraphQL schema for the `db` project specified in `.graphqlconfig.yml`
1. Invoke code generation as specified in `.graphqlconfig.yml`

```yml
hooks:
  post-deploy:
    - echo "Deployment finished"
    - graphql get-schema --project db
    - graphql prepare
```

Note that this setup assumes the availability of a `.graphqlconfig.yml` looking similar to this:

```yml
projects:
  prisma:
    schemaPath: generated/prisma.graphql
    extensions:
      prisma: prisma.yml
      prepare-binding:
        output: generated/prisma.ts
        generator: prisma-ts
```
