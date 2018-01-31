---
alias: aira9zama5
description: Docker
---

# Docker

The infrastructure for self-hosted (and local) clusters is entirely based on [Docker](https://www.docker.com). In that sense, it's theoretically possible to deploy your Prisma service simply with the [Docker CLI](https://docs.docker.com/engine/reference/commandline/cli). In fact, a number of commands in the Prisma CLI actually are simple _proxies_ for the Docker CLI: All of the `prisma local <subcommand>` commands as well as the `prisma cluster logs` command fall into that category.

- `prisma local start`: Starts the Prisma Docker containers by invoking [`docker-compose up`](https://docs.docker.com/compose/reference/up/)
- `prisma local stop`: Stops the Prisma Docker containers by invoking [`docker stop`](https://docs.docker.com/engine/reference/commandline/stop/)
- `prisma local upgrade`: Downloads the [latest Prisma images](https://hub.docker.com/r/prismagraphql/prisma/tags/) from Docker Hub using [`docker pull`](https://docs.docker.com/engine/reference/commandline/pull/)
- `prisma local nuke`: Hard-resets the local development cluster by invoking [`docker-compose kill`](https://docs.docker.com/compose/reference/kill/), [`docker-compose down`](https://docs.docker.com/compose/reference/down/) and [`docker-compose up`](https://docs.docker.com/compose/reference/up/) (in that order)
- `prisma cluster logs`: Shows the logs of the Docker containers using [`docker logs`](https://docs.docker.com/engine/reference/commandline/logs/)

## Docker images

Prisma comes with two Docker images that are configured using [Docker Compose](https://docs.docker.com/compose/).

- [`prismagraphlq/prisma`](https://hub.docker.com/r/prismagraphql/prisma/)
- [`mysql`](https://hub.docker.com/_/mysql/)

> Currently Prisma supports only MySQL as a database technology. In the future, [more databases will be supported](!alias-eep0ugh1wa#what-databases-does-prisma-support).

## Configuration with Docker Compose

### Docker Compose file

#### Structure

Here is what `docker-compose.yml` for **Prisma 1.1** looks like:

```yml
version: "3"
services:
  prisma-db:
    image: mysql:5.7
    container_name: prisma-db
    networks:
      - prisma
    restart: always
    command: mysqld --max-connections=1000 --sql-mode="ALLOW_INVALID_DATES,ANSI_QUOTES,ERROR_FOR_DIVISION_BY_ZERO,HIGH_NOT_PRECEDENCE,IGNORE_SPACE,NO_AUTO_CREATE_USER,NO_AUTO_VALUE_ON_ZERO,NO_BACKSLASH_ESCAPES,NO_DIR_IN_CREATE,NO_ENGINE_SUBSTITUTION,NO_FIELD_OPTIONS,NO_KEY_OPTIONS,NO_TABLE_OPTIONS,NO_UNSIGNED_SUBTRACTION,NO_ZERO_DATE,NO_ZERO_IN_DATE,ONLY_FULL_GROUP_BY,PIPES_AS_CONCAT,REAL_AS_FLOAT,STRICT_ALL_TABLES,STRICT_TRANS_TABLES,ANSI,DB2,MAXDB,MSSQL,MYSQL323,MYSQL40,ORACLE,POSTGRESQL,TRADITIONAL"
    environment:
      MYSQL_ROOT_PASSWORD: $SQL_INTERNAL_PASSWORD
      MYSQL_DATABASE: $SQL_INTERNAL_DATABASE
    ports:
      - "3366:3306" # Temporary/debug mapping to the host
    volumes:
      - db-persistence:/var/lib/mysql

  prisma-database:
    image: prismagraphql/prisma:1.0
    restart: always
    ports:
      - "0.0.0.0:${PORT}:${PORT}"
    networks:
      - prisma
    environment:
      PORT: $PORT
      SCHEMA_MANAGER_SECRET: $SCHEMA_MANAGER_SECRET
      SCHEMA_MANAGER_ENDPOINT: $SCHEMA_MANAGER_ENDPOINT
      SQL_CLIENT_HOST_CLIENT1: $SQL_CLIENT_HOST
      SQL_CLIENT_HOST_READONLY_CLIENT1: $SQL_CLIENT_HOST
      SQL_CLIENT_HOST: $SQL_CLIENT_HOST
      SQL_CLIENT_PORT: $SQL_CLIENT_PORT
      SQL_CLIENT_USER: $SQL_CLIENT_USER
      SQL_CLIENT_PASSWORD: $SQL_CLIENT_PASSWORD
      SQL_CLIENT_CONNECTION_LIMIT: 10
      SQL_INTERNAL_HOST: $SQL_INTERNAL_HOST
      SQL_INTERNAL_PORT: $SQL_INTERNAL_PORT
      SQL_INTERNAL_USER: $SQL_INTERNAL_USER
      SQL_INTERNAL_PASSWORD: $SQL_INTERNAL_PASSWORD
      SQL_INTERNAL_DATABASE: $SQL_INTERNAL_DATABASE
      CLUSTER_ADDRESS: $CLUSTER_ADDRESS
      SQL_INTERNAL_CONNECTION_LIMIT: 10
      CLUSTER_PUBLIC_KEY: $CLUSTER_PUBLIC_KEY
      BUGSNAG_API_KEY: ""
      ENABLE_METRICS: "0"
      JAVA_OPTS: "-Xmx1G"

networks:
  prisma:
    driver: bridge

volumes:
  db-persistence:
```

#### Environment variables

- Prisma
  - `PORT`: The port your Prisma service(s) will running on
  - `SCHEMA_MANAGER_SECRET`: 
  - `SCHEMA_MANAGER_ENDPOINT`: 
- SQL client
  - `SQL_CLIENT_HOST`:
  - `SQL_CLIENT_PORT`:
  - `SQL_CLIENT_USER`:
  - `SQL_CLIENT_PASSWORD`:
  - `SQL_CLIENT_CONNECTION_LIMIT`:
- SQL internal:
  - `SQL_INTERNAL_HOST`: 
  - `SQL_INTERNAL_PORT`: 
  - `SQL_INTERNAL_USER`: 
  - `SQL_INTERNAL_PASSWORD`: 
  - `SQL_INTERNAL_DATABASE`: 
  - `SQL_INTERNAL_CONNECTION_LIMIT`: 
- Cluster: 
  - `CLUSTER_ADDRESS`: 
  - `CLUSTER_PUBLIC_KEY`: The public key for that cluster, it will be used to validate the _deployment token_

##### Local

```
PORT=4466
SCHEMA_MANAGER_SECRET=MUCHSECRET
SCHEMA_MANAGER_ENDPOINT="http://prisma-database:${PORT}/cluster/schema"

SQL_CLIENT_HOST="prisma-db"
SQL_CLIENT_PORT="3306"
SQL_CLIENT_USER="root"
SQL_CLIENT_PASSWORD="graphcool"
SQL_CLIENT_CONNECTION_LIMIT=10

SQL_LOGS_HOST="prisma-db"
SQL_LOGS_PORT="3306"
SQL_LOGS_USER="root"
SQL_LOGS_PASSWORD="graphcool"
SQL_LOGS_DATABASE="logs"
SQL_LOGS_CONNECTION_LIMIT=10

SQL_INTERNAL_HOST="prisma-db"
SQL_INTERNAL_PORT="3306"
SQL_INTERNAL_USER="root"
SQL_INTERNAL_PASSWORD="graphcool"
SQL_INTERNAL_DATABASE="graphcool"
SQL_INTERNAL_CONNECTION_LIMIT=10
```

##### Digital Ocean

```
PORT=4466
SCHEMA_MANAGER_SECRET=SECRET_1
SCHEMA_MANAGER_ENDPOINT=http://prisma-database:${PORT}/cluster/schema

SQL_CLIENT_HOST=prisma-db
SQL_CLIENT_PORT=3306
SQL_CLIENT_USER=root
SQL_CLIENT_PASSWORD=SECRET_2
SQL_CLIENT_CONNECTION_LIMIT=10

SQL_INTERNAL_HOST=prisma-db
SQL_INTERNAL_PORT=3306
SQL_INTERNAL_USER=root
SQL_INTERNAL_PASSWORD=SECRET_2
SQL_INTERNAL_DATABASE=graphcool
SQL_INTERNAL_CONNECTION_LIMIT=10

CLUSTER_ADDRESS=http://prisma-database:${PORT}
CLUSTER_PUBLIC_KEY=PUBLIC_KEY
```