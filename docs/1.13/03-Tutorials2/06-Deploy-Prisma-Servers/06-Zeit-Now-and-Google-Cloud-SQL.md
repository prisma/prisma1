---
alias: mohj5eiwot
description: Learn how to deploy Prisma servers to Zeit Now.
---

# Zeit Now and GCP Cloud SQL

In this tutorial, you’re going to learn how to deploy a Prisma server to [Zeit Now](https://zeit.co/now). The server will be backed by a MySQL database hosted in [Google Cloud Platform](https://cloud.google.com/gcp) (GCP).

## Overview

[Zeit Now](https://zeit.co/now) provides real-time Node and Docker cloud deployments.

Google Cloud SQL provides fully-managed PostgreSQL & MySQL instances. You may follow the [tutorials and quick guides](https://cloud.google.com/sql/docs/) to configure and setup your SQL instance using either the console or command line.

## Prerequisites

#### GCP Cloud SQL

For the purpose of this tutorial you will need to have signed up for GCP, created a project and enabled the Cloud SQL Admin API (see [quickstart](https://cloud.google.com/sql/docs/mysql/) for more information).

You also should install the [Cloud SDK](https://cloud.google.com/sdk/).

### Zeit Now

Sign up to [zeit.co](https://zeit.co).

Install the [now-cli)](https://github.com/zeit/now-cli)

## 1. MySQL

### 1.1. Creating A Cloud SQL Instance

We'll be creating a MySQL instance called `prisma` on GCP via the command line. You may also do this via the console.

```bash
gcloud sql instances create prisma --tier=db-f1-micro --region=europe-west1
```

You'll see the following output:

```
NAME    DATABASE_VERSION  LOCATION        TIER         ADDRESS      STATUS
prisma  MYSQL_5_6         europe-west1-d  db-f1-micro  xx.xx.xx.xx  RUNNABLE
```

You can see more information about the instance with the following command:

```bash
gcloud sql instances describe prisma
```

Write down the `connectionName` field, it should look something like `project-id:europe-west1-d:prisma`

* We are deploying to the `europe-west1` region as this is the same location as Zeit in Europe. The default region is `us-central`.
* Tier is the instance's machine type. `db-f1-micro` is the smallest possible. The default tier is `db-n1-standard-1`.

---


* You can learn the additional [command line flags](https://cloud.google.com/sdk/gcloud/reference/beta/sql/instances/create)
* The steps for creating a Postgres database instance are virtually identical.

### 1.2. Create SQL User

Create a user called `prisma` with a password of `my-secret`, that only has access over the `cloud_sql_proxy` that will be running alongside the Prisma process inside the Docker container.

```bash
gcloud beta sql users create prisma --instance=prisma --password=my-secret --host=cloudsqlproxy~%
```

Your MySQL Instance is now up and running.

Note down your username and password. You'll need this information later!

### 1.3. Create SQL Client Service Account

In order to authorize the secure `cloud_sql_proxy` process to connect to our instance, we'll need to create a service account. The Cloud SQL Proxy provides secure access to your Cloud SQL Second Generation instances without having to whitelist IP addresses or configure SSL.

Go to your [GCP Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts) section within `IAM & admin`

Click `Create Service Account`, give it a name, and make sure it has the `Cloud SQL Client` role attached.

Select `Furnish a new private key`, with a `JSON` key type. 

Click `Save` and put the private key JSON file somewhere memorable.

> Learn more about the cloud_sql_proxy: https://cloud.google.com/sql/docs/mysql/sql-proxy

## 2. Deploying a Prisma server to Zeit Now

Deployment to Now consists of 3 files:

- Dockerfile
- Prisma configuration
- Now deployment configuration

### 2.1 Now Secrets

First we will add secrets to Now to store the SQL password, management secret, instance connection name, and an encoded service account private key from earlier.

```bash
now secret add sql-password my-secret
now secret add prisma-management-api-secret so-secret
now secret add instance-connection-name project-id:europe-west1-d:prisma
now secret add gcloud-service-key "$(cat ./gcloud-service-key.json | base64)"
```

> Learn more about Now secrets: https://zeit.co/docs/getting-started/secrets

### 2.2 Prisma Configuration

Create a file `config.yml`. This contains the Prisma Server configuration.

```yml
managementApiSecret: PRISMA_MANAGEMENT_API_SECRET
port: 4466
databases:
    default:
        connector: mysql
        host: 127.0.0.1 # required by cloud_sql_proxy
        port: 3306
        user: prisma
        password: SQL_PASSWORD
        migrations: true
        active: true
```

* `SQL_PASSWORD` and `PRISMA_MANAGEMENT_API_SECRET` will be replaced with Now build-time environment variables.

### 2.2 Now Deployment Configuration

Create `now.json`. This file contains our build environment variables and references to the secrets.

```json
{
  "name": "prisma-now",
  "type": "docker",
  "env": {
    "INSTANCE_CONNECTION_NAME": "@instance-connection-name"
  },
  "features": {
    "cloud": "v1"
  },
  "build": {
    "env": {
      "SQL_PASSWORD": "@sql-password",
      "PRISMA_MANAGEMENT_API_SECRET": "@prisma-management-api-secret",
      "GCLOUD_SERVICE_KEY": "@gcloud-service-key"
    }
  },
}
```

> Learn more about environment variables: https://zeit.co/docs/features/build-env-and-secrets and more about `now.json`: https://zeit.co/docs/features/configuration.
> To take advantage of Zeit's automatic scaling: https://zeit.co/docs/getting-started/scaling

### 2.3 Dockerfile

Create a file called `Dockerfile`. This tells Now how to build and configure the Prisma server.

```file
FROM alpine as base
WORKDIR /usr/src

ARG PRISMA_MANAGEMENT_API_SECRET
ARG MYSQL_PASSWORD
ARG GCLOUD_SERVICE_KEY

RUN wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud_sql_proxy 

RUN echo "$GCLOUD_SERVICE_KEY" | base64 -d > gcloud-service-key.json

COPY config.yml .
RUN sed -i "s/PRISMA_MANAGEMENT_API_SECRET/$PRISMA_MANAGEMENT_API_SECRET/g" config.yml
RUN sed -i "s/MYSQL_PASSWORD/$MYSQL_PASSWORD/g" config.yml

FROM prismagraphql/prisma:1.14

COPY --from=base /usr/src/cloud_sql_proxy /usr/src/gcloud-service-key.json /usr/src/config.yml ./

RUN chmod +x cloud_sql_proxy

ENV PRISMA_CONFIG_PATH ./config.yml

EXPOSE 4466

ENTRYPOINT ./cloud_sql_proxy -instances=$INSTANCE_CONNECTION_NAME=tcp:3306 -credential_file=gcloud-service-key.json & /bin/sh -c /app/start.sh
```

You can edit `Dockerfile` with the version of Prisma you wish to deploy from [dockerhub](https://hub.docker.com/r/prismagraphql/prisma/tags/)

### 2.4 Deploy

Then you are ready to deploy.

```bash
now
```

Now will go through it's deployment process, which will take a few minutes.

> From here you may wish to alias the deployment in Now: https://zeit.co/docs/features/aliases before updating your endpoint in `prisma.yml`.

An example project can be found here: https://github.com/develomark/prisma-now.

## Author

[Mark Petty](https://github.com/develomark) – [Intrusted](https://intrusted.co.uk)

[Sam McCord](https://github.com/sammccord) – [Brutal Software](https://brutal.software)