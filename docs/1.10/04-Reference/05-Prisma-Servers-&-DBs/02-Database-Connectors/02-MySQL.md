---
alias: ajai7auhoo
description: Learn how to use the MySQL connector to connect your Prisma server to a MySQL database.
---

# MySQL

To connect your Prisma server to a MySQL database, you need to use the MySQL connector. The connector needs to be specified in the [`docker-compose.yml`](!alias-aira9zama5#docker-compo) file that's the foundation for your Prisma server:

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
            connector: mysql
            host: mysql
            port: 3306
            user: root
            password: prisma
            migrations: true
            managementSchema: management
  mysql:
    image: mysql
    restart: always
    environment:
      MYSQL_USER: root
      MYSQL_ROOT_PASSWORD: prisma
    volumes:
      - mysql:/var/lib/mysql
volumes:
  mysql: ~
```

## Get started

### Creating a Prisma server

You can setup a new Prisma server that connects to a MySQL database using the Prisma CLI (requires [Docker](https://www.docker.com)):

1. Run `prisma1 init hello-world`
1. Select **Create new database**
1. Select MySQL
1. Navigate into the new `hello-world` directory: `cd hello-world`
1. Start Prisma server: `docker-compose up -d`
1. Run `prisma1 deploy` to deploy your Prisma API

### Troubleshooting

#### Connecting to a local database not running in Docker

As Prisma is running in Docker it cannot connect directly to databases running on your local machine by connecting to `localhost`. To work around this Docker v 18.03 introduced a hostname that is routed to your local machine: `host.docker.internal`. Read more about this in this [Stack Overflow question](https://stackoverflow.com/questions/31324981/how-to-access-host-port-from-docker-container).

#### general

If you have used previous versions of the Prisma CLI with Docker, you need to clear your Docker setup before running `docker-compose up -d`, otherwise you might run into the following error message:

```
ERROR: for mysql_prisma_1  Cannot start service prisma: driver failed programming external connectivity on endpoint mysql_prisma_Creating mysql_db_1     ... done

ERROR: for prisma  Cannot start service prisma: driver failed programming external connectivity on endpoint mysql_prisma_1 (b9aa3375c9374b77bab447b3777d1e5a7d78e0081106699b637065e6db4a5a88): Bind for 0.0.0.0:4466 failed: port is already allocated
ERROR: Encountered errors while bringing up the project.
```

You can clear your Docker setup using the following commands:

```sh
docker kill $(docker ps -aq)
docker rm $(docker ps -aq)
```

> **Note**: If you're using [`fish`](https://fishshell.com/) or some other shell, you might need to adjust the commands accordingly.
