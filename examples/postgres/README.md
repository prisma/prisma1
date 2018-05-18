# Postgres

This example demonstrates how to use Prisma with Postgres.

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

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a demo server (rather than locally with Docker), please follow [this link](https://www.prisma.io/docs/quickstart/).

</details>

### 3. Explore the Postgres

Running the command `docker ps` will show you an image running for Postgres running at port 5432 and an image running the Prisma which connects to this external Postgres running at port 5432.

Let us take a look at the Postgres specific parts of the `docker-compose.yml` file in this example.

This part of the configuration declares the service for postgres database which runs on its default port 5432.

```yml
postgres:
    image: postgres
    restart: always
    environment:
      POSTGRES_USER: prisma
      POSTGRES_PASSWORD: prisma
    volumes:
      - postgres:/var/lib/postgresql/data
```

This part of configurations declares the environment for Prisma server to connect to the external Postgres database running at the port 5432.

```yml
environment:
      PRISMA_CONFIG: |
        port: 4466
        # uncomment the next line and provide the env var PRISMA_MANAGEMENT_API_SECRET=my-secret to activate cluster security
        # managementApiSecret: my-secret
        databases:
          default:
            connector: postgres
            host: postgres
            port: 5432
            user: prisma
            password: prisma
            migrations: true
```
