# Postgres

This example demonstrates how to implement a Postgres connector with Prisma.

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/postgres
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd postgres
yarn install
```

### 2. Deploy the Prisma database service

You can now [deploy](https://www.prismagraphql.com/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1.  Remove the `cluster` property from `prisma.yml`
1.  Run `yarn prisma deploy`
1.  When prompted by the CLI, select a public cluster (e.g. `prisma-eu1` or `prisma-us1`)
1.  Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command

</details>

### 3. Explore the Postgres

Running the command `docker ps` will show you an image running for Postgres running at port 5432 and an image running Prisma which connects to this external Postgres running at port 5432.
