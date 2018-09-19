---
alias: zie0ahhaox
description: Learn how to deploy Prisma servers to Dokku.
---

# Dokku

In this tutorial, you’re going to learn how to deploy a Prisma server to [Dokku](http://dokku.viewdocs.io/dokku/).

## Overview

[Dokku](http://dokku.viewdocs.io/dokku/) is an extendible, open source Platform as a Service that runs on a single server of your choice.

## Prerequisites

### VPS or local server to host Dokku

You need a system that meets following requirements:

1.  A fresh installation of Ubuntu 16.04 x64, Ubuntu 14.04 x64, Debian 8.2 x64 or CentOS 7 x64 (experimental) with the FQDN set [1]

2.  At least 1GB of system memory

### Install Dokku

You need to install an up-to-date version of Dokku on your system of choice. You can find installation instructions on http://dokku.viewdocs.io/dokku/getting-started/installation/#installing-the-latest-stable-version.

## Deploy

### Create the app

Create the application on the Dokku host. You will need to `ssh` onto the host to run this command.

```bash
# on the Dokku host
dokku apps:create prisma-server
```

### Create the backing service

Dokku by default does not provide any datastores such as MySQL or PostgreSQL. You will need to install plugins to handle that, but fortunately [Dokku has official plugins for common datastores.](http://dokku.viewdocs.io/dokku/community/plugins/#official-plugins-beta)

### Install the Postgres plugin and create a service

```bash
# on the Dokku host
# install the Postgres plugin
# Plugin installation requires root, hence the user change
sudo dokku plugin:install https://github.com/dokku/dokku-postgres.git

# create a Postgres service with the name prisma-server-db
dokku postgres:create prisma-server-db
```

You`ll see an output like that, once your server was created:

<pre><code>
       Waiting for container to be ready
       Creating container database
       Securing connection to database
=====> Postgres container created: tst
=====> Container Information
       Config dir:          /var/lib/dokku/services/postgres/tst/config
       Data dir:            /var/lib/dokku/services/postgres/tst/data
       Dsn:                 postgres://postgres:bd793d8a330715891698434697a0345b@dokku-postgres-prisma-server-db:5432/prisma-server-db
       Exposed ports:       -
       Id:                  df03bfa7ca9ae3d42857dc5db2d98542542c5f4482fd2d269cf86ad663bcb2fe
       Internal ip:         172.17.0.5
       Links:               -
       Service root:        /var/lib/dokku/services/postgres/tst
       Status:              running
       Version:             postgres:10.2
</pre></code>

The important part of that output is the Dsn. We'll need it later for setting up the prisma server!

### Linking backing service to application

Once the service creation is complete, link the service to your `prisma-server` application.

```bash
# on the Dokku host
# each official datastore offers a `link` method to link a service to any application
dokku postgres:link prisma-server-db prisma-server
```

### Create a new project folder

Create a new folder on your local machine and add these two files with the following content to it.

`Dockerfile`

```
FROM prismagraphql/prisma:1.13-beta
ARG PRISMA_CONFIG_PATH
ENV PRISMA_CONFIG_PATH prisma.yml
COPY config.yml prisma.yml
EXPOSE 4466
```

`config.yml`

```yml
port: 4466
databases:
  default:
    connector: postgres
    host: dokku-postgres-prisma-server-db
    port: 5432
    user: postgres
    password: bd793d8a330715891698434697a0345b
    migrations: true
```

### Deploy the Prisma server

Now you can deploy the Prisma server to your Dokku server. All you have to do is initialize a git project and add a remote to the app.

```bash
# from your local machine, inside your project folder
git init
git add .
git commit -m "Initial commit"
# the remote username *must* be dokku or pushes will fail
git remote add dokku dokku@YOUR_DOKKU_HOST:prisma-server
git push --set-upstream dokku master
```

### Access your new deployed Prisma server

The GraphQL Playground should be accessible at http://YOUR_DOKKU_HOST:4466 after dokku deployed your app!

## Author

[Waldemar Penner](https://github.com/w0wka91)
