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
    env_file: './.mysql.env'
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
    env_file: './.prisma.env'

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

#### Environment variables

You might have noticed the `env_file` definitions in the `docker-compose.yml` above. The two files will define how both containers (`mysql` and `prisma server`) will be configured. The following section describes how to configure them for development purposes **without any security mechanisms enabled**.

MySQL environment:

```env(path=./.mysql.env)
MYSQL_ROOT_PASSWORD=prisma
```

Prisma environment:

```env(path=./.prisma.env)
PORT=4466
SQL_CLIENT_HOST_CLIENT1=prisma-db
SQL_CLIENT_HOST_READONLY_CLIENT1=prisma-db
SQL_CLIENT_HOST=prisma-db
SQL_CLIENT_PORT=3306
SQL_CLIENT_USER=root
SQL_CLIENT_PASSWORD=prisma
SQL_CLIENT_CONNECTION_LIMIT=10
SQL_INTERNAL_HOST=prisma-db
SQL_INTERNAL_PORT=3306
SQL_INTERNAL_USER=root
SQL_INTERNAL_PASSWORD=prisma
SQL_INTERNAL_DATABASE=graphcool
CLUSTER_ADDRESS=http://prisma-database:4466
SQL_INTERNAL_CONNECTION_LIMIT=10
SCHEMA_MANAGER_SECRET=graphcool
SCHEMA_MANAGER_ENDPOINT=http://prisma-database:4466/cluster/schema
CLUSTER_PUBLIC_KEY=<GENERATE VIA https://api.cloud.prisma.sh/>
BUGSNAG_API_KEY=""
ENABLE_METRICS=0
JAVA_OPTS=-Xmx1G
```

##### Digital Ocean

The environment definition above works as well when deploying to Digital Ocean with one **big** caveat: No security is enabled. You have to configure the environment variables differently in order to secure your cluster deployment for a production kind of scenario:

  * `./.mysql.env`: Use a more secure password and define it in `MYSQL_ROOT_PASSWORD`
  * `./.prisma.env`: Use the new MySQL root password in the variables: `SQL_CLIENT_PASSWORD` and `SQL_INTERNAL_PASSWORD`


**Important:** Follow the section about [Enable cluster authentication](https://www.prismagraphql.com/docs/tutorials/cluster-deployment/digital-ocean-(docker-machine)-texoo9aemu#7.-enable-cluster-authentication) in the [Digital Ocean Tutorial](https://www.prismagraphql.com/docs/tutorials/cluster-deployment/digital-ocean-(docker-machine)-texoo9aemu). The steps described in there could be summarized as:

  1. Generate a RSA public/private keypair
  2. Submit the public key as the `CLUSTER_PUBLIC_KEY` environment variable to the Prisma cluster (this step enables the cluster authentication)
  3. Add your cluster via `prisma cluster add` and provide the `private key` when asked about the cluster secret (see next section)

## Adding the cluster to your local Prisma configuration

You have to tell Prisma CLI your new configuration in order to be capable of deploying a Prisma service to the new cluster:

```
❯ prisma cluster add
? Please provide the cluster endpoint "Define your cluster endpoint. Local: http://localhost:4466; Digital Ocean: http://droplet-ip-address:4466
? Please provide the cluster secret "Local: Leave empty (CLUSTER_PUBLIC_KEY is commented); Digital Ocean: The private key from the key pair generated via `https://api.cloud.prisma.sh"
? Please provide a name for your cluster "your-cluster-name"
```

**Note:** Please provide all inputs **without the quotation marks**.

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
