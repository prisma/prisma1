---
alias: meemaesh3k
description: Learn how to deploy your Prisma database service to a local cluster.
---

# Local Cluster (Docker)

This guide describes advanced topics for the local Prisma cluster.

If you want to develop without internet access or simply prefer having everything locally, you can use `prisma local` to set up a local Prisma cluster with Docker.

### Installing Docker

You need an up-to-date version of Docker to run Prisma locally. You can find installation instructions for the free Docker Community Edition (CE) on https://www.docker.com/community-edition

### Set up local cluster

Getting started is as simple as running:

```sh
prisma local start
```

This will download the open source Docker images for Prisma and start two Docker containers for Prisma and MySQL. It can take a while, depending on your internet connection.

Now you can deploy a service to the local cluster:

```
❯ prisma deploy

? Please choose the cluster you want to deploy "demo@dev" to (Use arrow keys)

  prisma-eu1      Free development cluster (hosted on Prisma Cloud)
  prisma-us1      Free development cluster (hosted on Prisma Cloud)
❯ local           Local cluster (requires Docker)
```

This will add a `cluster` entry to the `prisma.yml` with the cluster you chose.

### Upgrading local cluster

To upgrade the local cluster, you can run:

```sh
prisma local upgrade
```

> Note: It is recommended to export your data before upgrading, using the `prisma export` command.

If you run into issues during or after upgrading, you can [nuke the local cluster](!alias-si4aef8hee), wiping all data in the process.

### Cluster Information

You can view the current version of Prisma you are running by listing all available clusters:

```
> prisma cluster list

name            version        endpoint
──────────────  ─────────────  ──────────────────────────────────
local           1.0.0-beta4.2  http://localhost:4466/cluster
```

## Database Access (SQL)

Follow this guide to connect directly to the MySQL database powering your local Prisma cluster.

If you used `prisma local start` to start your local Prisma cluster, you will have two containers running:

`prisma` is running the main Prisma service
`prisma-db` is running the MySQL server that stores your data

This guide explains how to connect to your local MySQL server in order to query and update data directly.

> In the following, we assume that you have a local Prisma service running.

#### 1. Get the name of the Docker container that runs the MySQL database

list your running Prisma Docker containers:

```sh
docker ps --filter name=prisma
```

Verify that there is a container with the name `prisma-db` image `mysql:5.7`.

#### 2. Open MySQL client

To open the MySQL client in your terminal, run the following command. Note that prisma-db is the container name from the list above:

```sh
 docker exec -it prisma-db mysql -u root --host 127.0.0.1 --port 3306 --password=prisma
```

#### 3. Send SQL queries to the database

Once the MySQL client is open, you can ask for the currently available databases:

```mysql
show databases;
```

This will print the following output:

```
+---------------------------+
| Database                  |
+---------------------------+
| information_schema        |
| prisma                    |
| logs                      |
| mysql                     |
| performance_schema        |
| sys                       |
| <service-id>              |
+---------------------------+
```

From the available databases, the following three are relevant for you:

* `<service-id>`: The name of this database is a combination of the name and stage of your service. It looks like this: `service@stage`. It contains meta information about your service used to generate the GraphQL schema.
* `prisma`: This database contains meta-information about the Prisma service definition.

You can now select one of these databases with the `use` command, for example if your `<service-id>` is `my-service@dev`, then run:

```mysql
use my-service@dev;
```

You can list tables like this:

```sh
show tables;
```

```
+----------------------+
| Tables_in_my-app@dev |
+----------------------+
| Post                 |
| _RelayId             |
+----------------------+
```

Now you can start writing queries:

```sh
SELECT * FROM Post;
```

```
+---------------------------+---------------------+---------------------+-------------+----------------------+-------------------------------------------------+
| id                        | createdAt           | updatedAt           | isPublished | title                | text                                            |
+---------------------------+---------------------+---------------------+-------------+----------------------+-------------------------------------------------+
| cjc82o6cg000b0135wpxgybf6 | 2018-01-09 20:12:02 | 2018-01-09 20:12:02 |           1 | Hello World          | This is my first blog post ever!                |
| cjc82o6mo000d013599yzlwls | 2018-01-09 20:12:02 | 2018-01-09 20:12:02 |           1 | My Second Post       | My first post was good, but this one is better! |
| cjc82o6n4000f01350jortmv2 | 2018-01-09 20:12:02 | 2018-01-09 20:12:02 |           0 | Solving World Hunger | This is a draft...                              |
+---------------------------+---------------------+---------------------+-------------+----------------------+-------------------------------------------------+
```

You can quit MySQL like this:

```sh
exit;
```
