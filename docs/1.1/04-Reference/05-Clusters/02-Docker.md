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
      - "3306:3306" # Temporary/debug mapping to the host
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
  db-persistence: ~
```

> **Note**: In an upcoming release, [`prisma-database` will be renamed to just `prisma`](https://github.com/graphcool/prisma/issues/1791).

##### `prisma-db`

The `prisma-db` service is based on the [`mysql`](https://hub.docker.com/_/mysql/) Docker image. Here is an overview of its most important properties:

- `networks`: _Networks to join, referencing entries under the top-level networks key._ (from [Docker](https://docs.docker.com/compose/compose-file/#networks); value: `"Prisma"`)
- `restart`: _`no` is the default restart policy, and it does not restart a container under any circumstance. When `always` is specified, the container always restarts. The `on-failure` policy restarts a container if the exit code indicates an on-failure error._ (from [Docker](https://docs.docker.com/compose/compose-file/#restart); value: `always`)
- `command`: _Override the default command._ (from [Docker](https://docs.docker.com/compose/compose-file/#command))
- `environment.MYSQL_ROOT_PASSWORD`: _This variable is mandatory and specifies the password that will be set for the MySQL root superuser account._ (from [DockerHub](https://hub.docker.com/_/mysql/))
- `environment.MYSQL_DATABASE`: _This variable is optional and allows you to specify the name of a database to be created on image startup. If a user/password was supplied (see below) then that user will be granted superuser access (corresponding to GRANT ALL) to this database._ (from [DockerHub](https://hub.docker.com/_/mysql/))
- `ports`: _Expose ports._ (from [DockerHub](https://docs.docker.com/compose/compose-file/#ports); value: `_3366:3306_`) (left is host, right is container which is the port to be exposed)
- `volumes`: _Mount host paths or named volumes, specified as sub-options to a service._ (from [DockerHub](https://docs.docker.com/compose/compose-file/#volumes); value: `db-persistence:/var/lib/mysql`)

##### `prisma-database`

The `prisma-db` service is based on the [`prismagraphlq/prisma`](https://hub.docker.com/r/prismagraphql/prisma/) Docker image. Here is an overview of its most important properties:

- `networks`: _Networks to join, referencing entries under the top-level networks key._ (from [Docker](https://docs.docker.com/compose/compose-file/#networks); value: `"Prisma"`)
- `restart`: _`no` is the default restart policy, and it does not restart a container under any circumstance. When `always` is specified, the container always restarts. The `on-failure` policy restarts a container if the exit code indicates an on-failure error._ (from [Docker](https://docs.docker.com/compose/compose-file/#restart); value: `always`)
- `environment.SQL_CLIENT_HOST_CLIENT1`:
- `environment.SQL_CLIENT_HOST_READONLY_CLIENT1`:
- `environment.SQL_CLIENT_HOST`:
- `environment.SQL_CLIENT_PORT`:
- `environment.SQL_CLIENT_USER`:
- `environment.SQL_CLIENT_PASSWORD`:
- `environment.SQL_CLIENT_CONNECTION_LIMIT`: (value: `10`)
- `environment.SQL_INTERNAL_HOST`:
- `environment.SQL_INTERNAL_PORT`:
- `environment.SQL_INTERNAL_USER`:
- `environment.SQL_INTERNAL_PASSWORD`:
- `environment.SQL_INTERNAL_DATABASE`:
- `environment.CLUSTER_ADDRESS`:
- `environment.SQL_INTERNAL_CONNECTION_LIMIT`: (value: `10`)
- `environment.CLUSTER_PUBLIC_KEY`:
- `environment.BUGSNAG_API_KEY`:
- `environment.ENABLE_METRICS`:(value: `"0"`, i.e. _false_)
- `environment.JAVA_OPTS`: Maximum heap size available to Prisma (value: `"-Xmx1G"`, i.e. 1GB)

#### Environment variables

##### Local

```
PORT=4466

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

## Debugging

To get insights into the internals of your Prisma cluster, you can check the logs of the corresponding Docker container.

### Accessing Docker logs

If you need more extensive logs you can view the raw logs from the running Docker containers:

```sh
docker logs prisma-db
docker logs local_prisma-database_1
```

### Verify Docker containers

If you get an error message saying `Error response from daemon: No such container` you can verify that the containers are running:

```sh
docker ps
```

You should see output similar to this:

```
$ docker ps
CONTAINER ID  IMAGE                       COMMAND                 CREATED            STATUS            PORTS                   NAMES
7210106b6650  prismagraphql/prisma:1.0.0  "/app/bin/single-ser…"  About an hour ago  Up About an hour  0.0.0.0:4466->4466/tcp  prisma
1c15922e15ba  mysql:5.7                   "docker-entrypoint.s…"  About an hour ago  Up About an hour  0.0.0.0:3306->3306/tcp  prisma-db
```

### Nuke

If your local prisma cluster is in an unrecoverable state, the easiest option might be to completely reset it. Be careful as **this command will reset all data** in your local cluster.

```sh
$ prisma local nuke
Nuking local cluster 10.9s
Booting fresh local development cluster 18.4s
```
