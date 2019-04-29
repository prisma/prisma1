---
alias: aira9zama5
description: Docker
---

# Docker

The infrastructure for self-hosted (and local) Prisma servers is entirely based on [Docker](https://www.docker.com). The [Docker CLI](https://docs.docker.com/engine/reference/commandline/cli) and [Docker Compose CLI](https://docs.docker.com/compose/reference/) are used to manage the corresponding Docker images and containers.

Here's a quick rundown of the most important commands:

- [`docker-compose up -d`](https://docs.docker.com/compose/reference/up/): Start a new Prisma server to which you can deploy your Prisma APIs
- [`docker-compose stop`](https://docs.docker.com/compose/reference/stop/): Stops the Prisma server
- [`docker-compose pull`](https://docs.docker.com/compose/reference/pull/): Downloads the [latest Prisma images](https://hub.docker.com/r/prismagraphql/prisma/tags/) from Docker Hub
- [`docker logs`](https://docs.docker.com/compose/reference/logs/): Shows the logs of the Prisma server (helpful for debugging)

Note that these commands assume that you're in a directory where the [`docker-compose.yml`](#docker-compose-file) for your Prisma server is available. Otherwise you might just use the [Docker CLI](https://docs.docker.com/engine/reference/commandline/cli) directly to manage the images and containers.

## Docker images

Prisma servers are configured with several Docker images:

- **Prisma**
  - [`prismagraphlq/prisma`](https://hub.docker.com/r/prismagraphql/prisma/)
- **Database**
  - [`mysql`](https://hub.docker.com/_/mysql/)
  - [`postgres`](https://hub.docker.com/_/postgres/)

The **Prisma** Docker image is required, along with exactly one **Database** image.

> **Note**: In future Prisma versions, it will be possible to connect your Prisma server to multiple databases.

## Configuration with Docker Compose

### Docker Compose file

Here is what `docker-compose.yml` for **Prisma 1.13** looks like:

```yml
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.13
    restart: always
    ports:
    - "4466:4466"
    environment:
      PRISMA_CONFIG: |
        managementApiSecret: my-server-secret-123
        port: 4466
        databases:
          default:
            connector: mysql  # or `postgres`
            migrations: true
            host: db
            port: 3306        # or `5432` for `postgres`
            user: root
            password: prisma
  db:
    image: mysql:5.7
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: prisma
    volumes:
      - mysql:/var/lib/mysql
volumes:
  mysql: ~
```

##### `prisma`

The `prisma-db` service is based on the [`prismagraphlq/prisma`](https://hub.docker.com/r/prismagraphql/prisma/) Docker `image`. Here is an overview of its most important properties:

- `ports`: The value `"4466:4466"` means that the Prisma server will be available on port `4466` of the host machine. For example: `http://localhost:4466`.
- `environment`: This property specifies one environment variable called `PRISMA_CONFIG`. `PRISMA_CONFIG` contains several configuration options as well as the information needed for the Prisma server to connect to a database.
  - `managementApiSecret`: The secret the CLI needs to send to authenticate its requests against the Prisma server (see [here](!alias-eu2ood0she#symmetric-approach-using-a-single-secret) for more info). Alternatively, you could use the property `legacySecret` for asymmetric authentication.
  - `port`: The port on which the Prisma server is running.
  - `databases`: Specify which databases the Prisma server should connect to (currently Prisma servers can only connect to one database).
    - `default.connector`: Specify which Prisma database connector is to be used.
  
    - `default.active`: Setting this to `true` means you're using an _active_ (as opposed to a _passive_) database connector
    - `default.host`: The host machine of the database.
    - `default.port`: The port on whcih the database is running.
    - `default.user` and `default.password`: Credentials to authentivate against the database.

#### `db`

The `db` service in the above example is based on the [`mysql`](https://hub.docker.com/_/mysql/) Docker image, alternatively you could use [`postgres`](https://hub.docker.com/_/postgres/).

Note that both environment variables that are specified, `MYSQL_USER` and `MYSQL_ROOT_PASSWORD`, need to correspond to the `default.user` and `default.password` values from the `prisma` configuration so that the Prisma server is able to authenticate against the database.

## Debugging

To get insights into the internals of your Prisma cluster, you can check the logs of the corresponding Docker container.

### Accessing Docker logs

If you need more extensive logs you can view the raw logs from the running Docker containers:

```sh
docker-compose logs
```

### Verify Docker containers

If you get an error message saying `Error response from daemon: No such container` you can verify that the containers are running:

```sh
docker ps
```

You should see output similar to this:

```
$ docker ps
CONTAINER ID        IMAGE                               COMMAND                  CREATED             STATUS              PORTS                    NAMES
2b799c529e73        prismagraphql/prisma:1.7            "/bin/sh -c /app/sta…"   17 hours ago        Up 7 hours          0.0.0.0:4466->4466/tcp   myapp_prisma_1
757dfba212f7        mysql:5.7                           "docker-entrypoint.s…"   17 hours ago        Up 7 hours          3306/tcp                 prisma-db
```

### Hard resetting the Docker environment

If your local prisma cluster is in an unrecoverable state, the easiest option might be to completely reset it. Be careful as **these commands will reset all data** in your local Prisma server (including deployed Prisma APIs).

```sh
docker-compose kill
docker-compose down
docker-compose up -d
```
