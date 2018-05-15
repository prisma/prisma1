# YML Structure

This example demonstrates various properties available to us in `prisma.yml` that define our service description.

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/yml-structure
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd yml-structure
yarn install
```

### 2. Deploy the Prisma database service

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1.  Remove the `cluster` property from `prisma.yml`
1.  Run `yarn prisma deploy`
1.  When prompted by the CLI, select a demo cluster (e.g. `prisma-eu1` or `prisma-us1`)
1.  Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command

</details>

### 3. Description of `prisma.yml` properties

#### datamodel (required)

The datamodel points to one or more .graphql-files containing type definitions written in [GraphQL SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51). If multiple files are provided, the CLI will simply concatenate their contents at deployment time.

In this example we are defining the data model via two files `datamodel.graphql` and `enums.graphql`

#### custom (optional)

The custom property lets you specify any sorts of values you want to reuse elsewhere in your prisma.yml. It thus doesn't have a predefined structure. You can reference the values using variables with the self variable source, e.g.: `${self:custom.myVariable}`.

#### endpoint (optional)

In this example the `endpoint` property has the value

`http://${self:custom.serverIP}:${self:custom.serverPort}/yml-structure/default`

and based on the description of `custom` in context of this example, it will resolve to

`http://localhost:4466/yml-structure/default`

Let us deconstruct it and extract valid components

* Prisma server: The server that will host your Prisma API i.e. `http://localhost:4466`
* Workspace (only Prisma Cloud): The name of the Workspace you configured through Prisma Cloud - workspaces are not present on local server. These are exclusive to Prisma cloud.
* Service name: A descriptive name for your Prisma API i.e. `yml-structure`
* Stage: The development stage of your cluster (e.g. dev, staging, prod) i.e. `default`

Note that `default` name for `service` and `stage` can be omitted, which means that the following endpoints are equivalent:

`http://localhost:4466/default/default`

`http://localhost:4466/default`

`http://localhost:4466`

#### secret

This property is used to generate a JWT for authentication with the service. A secret must follow these requirements:

* must be utf8 encoded
* must not contain spaces
* must be at most 256 characters long

After deploying a service with `secret` set in `prisma.yml`. We need to add `Authorization` HTTP header to all the requests we send to that service.

To get a hold of the JWT token, type `prisma token` in the console and send subsequent requests to the deployed service at
`http://localhost:4466/yml-structure` with the following HTTP headers

```json
{
  "Authorization": "Bearer <token from prisma token command>"
}
```

More features of the `seceret` property in `prisma.yml` are:

* It can have multiple secrets as comma separated values

`secret: first-secret, second-secret, third-secret`

* It can have a value supplied from environment variables

`secret: ${env:MY_SECRET}`

#### seed

`seed` property can be used to initialize the service database when it is being deployed for the first time.

In this example, we are using the following

```yml
seed:
  import: seed.graphql
```

This basically tells seed to use `seed.graphql` when the service is being deployed for the first time.

However, more options are available with `seed` property.

`import` sub-property can have `.graphql` files as in this example or path to zip file in [NDF](https://www.prisma.io/docs/reference/data-import-and-export/normalized-data-format-teroo5uxih/) format (probably generated via `prisma export` command).

`run` sup-property can executre arbitrary scripts to cover comples seed cases that are not covered by `seed` property.

#### hooks

The `hooks` property is used to define terminal commands which will be executed by the Prisma CLI before or after certain commands.

Currently, only `post-deploy` hook is available. In this example, we are using the `post-deploy` hook to perform the following tasks

```yml
hooks:
  post-deploy:
    - echo "Deployment finished"
    - graphql get-schema --project db
    - graphql prepare
```

* Printing "Deployment finished"

* Getting the latest schema

* Running `graphql prepare` to generate TS bindings

Note that these commands work closely in conjunction with `.graphqlconfig.yml` file that looks like this and directs the output of generate schema and typescript bindings.

```yml
projects:
  db:
    schemaPath: generated-schema.graphql
    extensions:
      endpoints:
        default: 'http://localhost:4466/yml-structure'
      prisma: prisma.yml
      prepare-binding:
        output: generated-prisma.ts
        generator: prisma-ts
```
