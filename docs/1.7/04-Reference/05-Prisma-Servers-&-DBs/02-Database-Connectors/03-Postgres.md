---
alias: neix6nesie
description: Learn how to use the Postgres connector to connect your Prisma server to a Postgres database.
---

# Postgres

To connect your Prisma server to a Postgres database, you need to use the Postgres connector. The connector needs to be specified in the [`docker-compose.yml`](!alias-aira9zama5#docker-compo) file that's the foundation for your Prisma server:

```yml
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.7
    restart: always
    ports:
    - "4466:4466"
    environment:
      PRISMA_CONFIG: |
        managementApiSecret: my-server-secret-123
        port: 4466
        databases:
          default:
            connector: postgres
            active: true
            host: db
            port: 5432
            user: root
            password: prisma
  db:
    image: postgres
    restart: always
    environment:
      MYSQL_USER: root
      MYSQL_ROOT_PASSWORD: prisma
```

## Get started

### Creating a Prisma server

You can setup a new Prisma server that connects to a Postgres database using the Prisma CLI (requires [Docker](https://www.docker.com)):

1. Run `prisma1 init hello-world`
1. Select **Create new database**
1. Select PostgreSQL
1. Navigate into the new `hello-world` directory: `cd hello-world`
1. Start Prisma server: `docker-compose up -d`
1. Run `prisma1 deploy` to deploy your Prisma API

### Troubleshooting

If you have used previous versions of the Prisma CLI with Docker, you need to clear your Docker setup before running `docker-compose up -d`, otherwise you might run into the following error message:

```
ERROR: for postgres_prisma_1  Cannot start service prisma: driver failed programming external connectivity on endpoint postgres_prisma_Creating postgres_db_1     ... done

ERROR: for prisma  Cannot start service prisma: driver failed programming external connectivity on endpoint postgres_prisma_1 (b9aa3375c9374b77bab447b3777d1e5a7d78e0081106699b637065e6db4a5a88): Bind for 0.0.0.0:4466 failed: port is already allocated
ERROR: Encountered errors while bringing up the project.
```

You can clear your Docker setup using the following commands:

```sh
docker kill $(docker ps -aq)
docker rm $(docker ps -aq)
```

> **Note**: If you're using [`fish`](https://fishshell.com/) or some other shell, you might need to adjust the commands accordingly.
