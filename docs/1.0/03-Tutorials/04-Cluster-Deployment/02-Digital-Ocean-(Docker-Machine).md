---
alias: texoo9aemu
description: Learn how to deploy your Prisma database service to Digital Ocean using Docker Machine.
---

# Digital Ocean - Docker Machine

This section describes how to set up a fully functioning Prisma server on Digital Ocean in less than 20 minutes. We will use Docker Machine to automate the entire installation process.

Digital Ocean is an easy to use provider of virtual servers. They offer different compute sizes called droplets. The 10$ droplet is a good starting point for a Prisma server.

> The setup described in this section does not include features you would normally expect from production ready servers, such as backup and active failover. We will add more guides in the future describing a more complete setup suitable for production use.

## Install Docker

You need Docker and Docker Machine.

Follow the instructions at https://docs.docker.com/machine/install-machine/#install-machine-directly

When done, you should be able to run the following command:

```
❯ docker-machine -v
docker-machine version 0.13.0, build 9ba6da9
```

## Connect to Digital Ocean

If you are new to Digital Ocean you should start by creating an account at https://www.digitalocean.com/

Then follow step 1 through 2 on https://docs.docker.com/machine/examples/ocean/

You can now start a droplet with the name prisma like this:

> Note that this starts a droplet with 1GB memory in the Amsterdam region. You can pick a different size and region by tweaking the parameters as described in https://docs.docker.com/machine/drivers/digital-ocean/

```sh
docker-machine create --driver digitalocean --digitalocean-access-token xxxxxxxxx --digitalocean-region ams2 --digitalocean-size 1gb prisma
```

You will now have a running droplet that is controleld by `docker-machine`. List your droplets by running:

```
❯ docker-machine ls
NAME             ACTIVE   DRIVER         STATE     URL                        SWARM   DOCKER        ERRORS
prisma           -        digitalocean   Running   tcp://45.55.177.154:2376           v18.01.0-ce
```

## Install Prisma on your Droplet

First step is to configure Docker to conenct to your droplet. Do this by running `docker-machine env prisma` and then run the command that is printed in the last line.

The command varies based on your os and shell. On MacOS it might look like this:

```sh
❯ docker-machine env prisma
set -gx DOCKER_TLS_VERIFY "1";
set -gx DOCKER_HOST "tcp://188.226.158.30:2376";
set -gx DOCKER_CERT_PATH "/Users/demo/.docker/machine/machines/prisma";
set -gx DOCKER_MACHINE_NAME "prisma";
# Run this command to configure your shell:
# eval (docker-machine env prisma-1gb)

❯ eval (docker-machine env prisma-1gb)
```

You can read more about this in the Docker docs https://docs.docker.com/machine/reference/env/

Now you need to copy the Configuration files for Prisma Cluster. In a new folder create the two files:

> .env

```
PORT=4466
SCHEMA_MANAGER_SECRET=SECRET_1
SCHEMA_MANAGER_ENDPOINT=http://prisma-database:${PORT}/cluster/schema
CLUSTER_ADDRESS=http://prisma-database:${PORT}
CLUSTER_PUBLIC_KEY=PUBLIC_KEY

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
```

> docker-compose.yml

```yml
version: "3"
services:
  prisma-db:
    container_name: prisma-db
    image: mysql:5.7
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

  prisma:
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
      CLUSTER_VERSION: 1.0.0-beta4.2

networks:
  prisma:
    driver: bridge

volumes:
  db-persistence: ~
```

In `.env`, replace `SECRET_1` and `SECRET_2` (occurs twice) with a long and secure random string.

Now you are ready to use `docker-compose` to start your Prisma cluster:

```sh
docker-compose up -d
```

## Enable Cluster Security

By default, anyone can connect to the new cluster using the Prisma CLI and deploy services. To lock down access, you need to configure a public/private keypair.

```
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -outform PEM -pubout -out public.pem
```

This will generate two files `private.pem` and `public.pem`

Copy the output of the following command to the `.env` file and replace `PUBLIC_KEY`:

```sh
cat public.pem | sed -n -e 'H;${x;s/\n/\\\\r\\\\n/g;p;}'
```

open ~/.prisma/config.yml in your favourite editor. It is likely that you already have one or more clusters defined in this file. Add another cluster and give it a descriptive name. For example `do-cluster`:

```yml
clusters:
  local:
    host: 'http://localhost:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
  do-cluster:
    host: 'http://45.55.177.154:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
```

Change the host to use the ip of your Digital Ocean droplet.

Copy the output of the following command and replace the content of `clusterSecret` for the new cluster:

```sh
cat private.pem | sed -n -e 'H;${x;s/\n/\\\\r\\\\n/g;p;}'
```

to restart the Prisma cluster:

```
docker-compose kill
docker-compose up -d
```

## Deploy a service

On your local machine, verify that the cluster configuration is being picked up correctly:

```sh
prisma1 cluster list
```

The output should include your newly added cluster.

Now you can create a new service and deploy it to the cluster:

```
prisma1 init
prisma1 deploy
```

Pick the new cluster in the deployment option. You should see output similar to this:

```
Added cluster: do-cluster to prisma.yml
Creating stage dev for service demo ✔
Deploying service `demo` to stage `dev` on cluster `do-cluster` 1.3s

Changes:

  User (Type)
  + Created type `User`
  + Created field `id` of type `GraphQLID!`
  + Created field `name` of type `String!`
  + Created field `updatedAt` of type `DateTime!`
  + Created field `createdAt` of type `DateTime!`

Applying changes 1.8s

Hooks:

Running $ graphql prepare...

Your GraphQL database endpoint is live:

  HTTP:  http://45.55.177.154:4466/demo/dev
  WS:    ws://45.55.177.154:4466/demo/dev
```
