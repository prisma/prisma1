---
alias: texoo9aemu
description: Learn how to deploy your Prisma database service to Digital Ocean using Docker Machine.
---

# Digital Ocean (Docker Machine)

In this tutorial, you will learn how to create a Prisma [cluster](!alias-eu2ood0she) on Digital Ocean and deploy your Prisma services to it.

[Digital Ocean](https://www.digitalocean.com/) is an easy-to-use provider of virtual servers. They offer configurable compute units of various sizes, called [Droplets](https://www.digitalocean.com/products/droplets/).

<InfoBox>

The setup described in this section does not include features you would normally expect from production-ready servers, such as _automated backups_ and _active failover_. We will add more guides in the future describing a more complete setup suitable for production use.

</InfoBox>

## 1. Install Docker & Docker Machine

The first thing you need to is install Docker and Docker Machine. The easiest way to install theses tools is by following the step in the Docker Docs directly.

<Instruction>

Follow the instructions (steps 1 through 3) at [https://docs.docker.com/machine/install-machine/#install-machine-directly](https://docs.docker.com/machine/install-machine/#install-machine-directly) to install Docker as well as  Docker Machine.

</Instruction>

Once you're done, you should be able to run the following command which will output the version of `docker-machine`:

```sh
docker-machine -v
```

## 2. Register at Digital Ocean

<Instruction>

If you haven't already, you need to start by creating an account at for Digital Ocean. You can do so [here](https://cloud.digitalocean.com/registrations/new).

</Instruction>

Once you have your account available, you need to generate a personal access token which is needed to create your Droplet.

<Instruction>

Follow the instructions [here](https://docs.docker.com/machine/examples/ocean/#step-2-generate-a-personal-access-token) to obtain your personal access token.

Make sure to save the token, you'll need it in the next step.

</Instruction>

## 3. Create your Digital Ocean Droplet

You can now go ahead and create your Droplet. The personal access token from the previous step will be used to associate the Droplet with your Digital Ocean account.

<Instruction>

In your terminal, use [`docker-machine create`](https://docs.docker.com/machine/reference/create/) to create your Droplet using the [`digitalocean`](https://docs.docker.com/machine/drivers/digital-ocean/) driver. Note that you need to replace the `__TOKEN__` placeholder with your personal access token from the previous step:

```sh
docker-machine create --driver digitalocean --digitalocean-access-token __TOKEN__ --digitalocean-size 1gb prisma
```

</Instruction>

The `--digitalocean-size 1gb` argument indicates that the Droplet receives 1GB of memory. The last argument of the command, `prisma`, determines the _name_ of the Droplet.

You will now have a running Droplet that can be controlled by `docker-machine`. You can list your current Droplets by running the following command:

```sh
docker-machine ls
```

## 4. Install Prisma on your Droplet

Now that you have your Droplet up and running, you can install Prisma on it. As [the Prisma infrastructure runs on Docker](!alias-aira9zama5), you could theoretically do this by using the Docker CLI directly. In that case, you could use the [Docker Compose file](!alias-aira9zama5#docker-compose-file) as foundation and run the required commands (e.g. [`docker-compose up`](https://docs.docker.com/compose/reference/up/)) yourself.

However, the Prisma CLI actually offers some commands that you can use for convenience to not fiddle with Docker yourself. In this tutorial, you'll take advantage of these commands - under the hood they will simply configure your Docker environment and invoke the required CLI commands.

The `docker` CLI is a client that allows to create and manage your Docker containers on a dedicated _host_ machine. By default, that host machine is your local machine (`localhost`). However, using `docker-machine` you can _point_ the `docker` CLI to run against a different host. Meaning that all your `docker` commands (including `docker-compose`) can be executed to a remote machine over the internet.

So, the next step is to make sure your `docker` commands actually run against the remote Digital Ocean Droplet you just created. You can do so by setting a number of environment variables that'll be used by `docker`. But how do you know _which_ environment variables - and what values to set? `docker-machine` to the rescue!

The following command prints the correct environment variables for you to set - in fact it even prints the commands that you need to execute in your terminal in order to set them correctly. Thank you `docker-machine`! 🙏

```sh
docker-machine env prisma
```

The output of that command looks somewhat similar to the following (depending on your shell and OS the commands to set environment variables might differ):

```sh
$ docker-machine env prisma
export DOCKER_TLS_VERIFY="1"
export DOCKER_HOST="tcp://104.132.24.246:2376"
export DOCKER_CERT_PATH="/Users/johndoe/.docker/machine/machines/prisma"
export DOCKER_MACHINE_NAME="prisma"
# Run this command to configure your shell:
# eval $(docker-machine env prisma)
```

<Instruction>

Copy the last line of that output, excluding the `#` in the beginning (which indicates a comment) and paste it into your terminal:

```sh
eval $(docker-machine env prisma)
```

</Instruction>

That's it - all your `docker` (and therefore `prisma1 local`) commands will now run against the Digital Ocean Droplet instead of your local machine!

To actually install Prisma on your Droplet, you need to perform the following steps (we'll go over them in detail afterwards):

1. Create `docker-compose.yml` with proper configuration
1. Create `.env` to configure the environment variables used by Docker
1. Run `docker-compose up` to install Prisma on the Droplet

Let's get started!

<Instruction>

First, go ahead and create a new directory on your file system where you'll place all the files for your project, you can call it `digital-ocean-demo`.

</Instruction>

<Instruction>

Inside that directory, create a new file called `docker-compose.yml` and copy the following contents into it:

```yml(path="digital-ocean-demo/docker-compose.yml")
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
    image: prismagraphql/prisma:1.3
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
      #CLUSTER_PUBLIC_KEY: $CLUSTER_PUBLIC_KEY
      BUGSNAG_API_KEY: ""
      ENABLE_METRICS: "0"
      JAVA_OPTS: "-Xmx1G"

networks:
  prisma:
    driver: bridge

volumes:
  db-persistence: ~
```

</Instruction>

Note the comment in front of the `environment.CLUSTER_PUBLIC_KEY` property. For now, we don't provide a public key to the Prisma cluster which is going to enable _everyone_ with access to the IP address of the Droplet to deploy and delete services to and from it! We'll add the security layer afterwards!

<Instruction>

Next, create another file that you call `.env`. Then paste the following contents into it:

```(path="digital-ocean-demo/.env")
PORT=4466
CLUSTER_ADDRESS=http://prisma-database:${PORT}

SQL_CLIENT_HOST=prisma-db
SQL_CLIENT_PORT=3306
SQL_CLIENT_USER=root
SQL_CLIENT_PASSWORD=__SECRET_2__
SQL_CLIENT_CONNECTION_LIMIT=10

SQL_INTERNAL_HOST=prisma-db
SQL_INTERNAL_PORT=3306
SQL_INTERNAL_USER=root
SQL_INTERNAL_PASSWORD=__SECRET_2__
SQL_INTERNAL_DATABASE=graphcool
SQL_INTERNAL_CONNECTION_LIMIT=10

SCHEMA_MANAGER_SECRET=SECRET_1
SCHEMA_MANAGER_ENDPOINT=http://prisma-database:${PORT}/cluster/schema
```

</Instruction>

> **Note**: To learn more about the variables configured here, check the corresponding [reference docs](!alias-aira9zama5#structure).

With these files in place, you can go ahead and install Prisma!

<Instruction>

Inside the `digital-ocean-demo` directory, run the following command:

```sh(path="digital-ocean-demo")
docker-compose up -d
```

</Instruction>

Remember that thanks to Docker Machine, this command will now run against the remote host, i.e. your Digital Ocean Droplet. The `-d` option starts the container in _detached mode_, meaning it will run as a background process.

Awesome, you now have your Prisma cluster available on the Digital Ocean Droplet! This means you can add it as a new cluster to your [cluster registry](!alias-eu2ood0she#cluster-registry) and then deploy your Prisma services to it!

## 5. Add the cluster to your cluster registry

The cluster registry is stored in `~/.prisma/config.yml`. It lists all the clusters you can use to deploy your Prisma services (excluding the ones you configured through the Prisma Cloud).

There is two ways how you can a new cluster to the registry:

1. Use the interactive Prisma CLI using the  `prisma1 cluster add` command
1. Open `~/.prisma/config.yml` and add the cluster manually

In both cases, there are three pieces of information that need to be provided:

- The **name** of the cluster, you can choose anything you like here.
- The **host**, i.e. the IP (or domain) including the port where the cluster is running.
- The **cluster secret** (optional). This is the private key matching the public key which was deployed to the Prisma cluster as the `environment.CLUSTER_PUBLIC_KEY` variables. As noted above, we skipped that step for now but will come back to it in a bit to enable cluster security.

You'll use the second option and add the cluster configuration to the registry manually.

<Instruction>

Open `~/.prisma/config.yml` and add a new entry (here called `digital-ocean-cluster`) to the `clusters` map:

```yml(path="/.prisma/config.yml")
clusters:
  digital-ocean-cluster:
    host: 'http://__DROPLET_IP_ADDRESS__:4466'
```

</Instruction>

Notice that there currently is a placeholder of the IP address of the Digital Ocean Droplet, so the value for `host` is not yet valid!

There are several ways to figure out the IP of your Droplet, e.g. by checking the [Digital Ocean Console](https://cloud.digitalocean.com/droplets). For this tutorial, you'll use the `docker-machine` CLI again.

<Instruction>

In your terminal, run the following command:

```sh
docker-machine ip prisma
```

</Instruction>

Note that `prisma` is just the _name_ of the Droplet you initially created. This will print the IP address of that Droplet.

<Instruction>

Copy the IP address that was printed from the command and use it to replace the `__DROPLET_IP_ADDRESS__` placeholder in the URL that's the value for the `host` property of your `digitical-ocean-cluster` cluster in `~/.prisma/config.yml`, e.g.:

```yml(path="/.prisma/config.yml")
host: 'http://104.131.127.241:4466'
```

</Instruction>

## 6. Deploy a Prisma service to the cluster

You're now ready to deploy a Prisma service to the cluster.

<Instruction>

Inside the `digital-ocean-demo` directory, run the following command:

```sh(path="digital-ocean-demo")
prisma1 init hello-world
```

</Instruction>

<Instruction>

From the interactive prompt, choose the `Minimal setup: database-only` option.

</Instruction>

This creates a new directory that has the initial setup for a minimal Prisma service:

```
hello-world
  ├── .graphqlconfig.yml
  ├── datamodel.graphql
  └── prisma.yml
```

Next, you can go ahead and deploy the Prisma service.

<Instruction>

Navigate into the newly-created `hello-world` directory and deploy the service:

```sh(path="digital-ocean-demo")
cd hello-world
prisma1 deploy
```

</Instruction>

Because no `cluster` is specified in `prisma.yml`, the CLI now prompts you to select a cluster. The list includes the `digital-ocean-cluster` you previously added:

```
$ prisma1 deploy

? Please choose the cluster you want to deploy "hello-world@dev" to (Use arrow keys)

  local                   Local cluster (requires Docker)
❯ digital-ocean-cluster   Self-hosted
```

<Instruction>

Select the `digital-ocean-cluster` from the list.

</Instruction>

The CLI is now going to deploy the service to that cluster and write the `cluster` entry to `prismal.yml`.

<InfoBox>

The Prisma CLI in that case acts as a _client_ for the Prisma Cluster API which is used to manage services on a specific cluster. If you want to explore that Cluster API yourself, you can navigate your browser to `http://__DROPLET_IP_ADDRESS__:4466/cluster`. Similar to before, you'll have to replace the `__DROPLET_IP_ADDRESS__` placeholder with the actual IP address of your Digital Ocean Droplet.

</InfoBox>

This is it! Your Prisma service is now running on your local cluster and can be access through the endpoint that was printed by the `prisma1 deploy` command! It will look similar to this: `http://__DROPLET_IP_ADDRESS__:4466/hello-world/dev`.

You can go ahead and send the following mutation and query to it:

```graphql
mutation {
  createUser(data: {
    name: "Sarah"
  }) {
    id
  }
}
```

```graphql
{
  users {
    id
    name
  }
}
```

## 7. Enable cluster authentication

As noted before, _everyone_ with access to the endpoint of your cluster (`http://__DROPLET_IP_ADDRESS__:4466/`) will be able to access your cluster in any way they like. This means they can add new services or delete existing ones and also get full read/write-access to the application data from these services. This is a major security leak and should never be the case in any sort of production environment! Let's go and fix it.

Here's a quick overview of the steps you need to perform (like before, we'll go through each step in detail afterwards):

1. Generate an RSA public/private keypair
1. Submit the public key as the `CLUSTER_PUBLIC_KEY` environment variable to the Prisma cluster (this step enables the cluster authentication)
1. Add the private key as the `clusterSecret` to the cluster registry
1. Redeploy the Prisma service

To generate a public/private-keypair, you can use any mechanism you wish, e.g. the [`openssl`](https://www.openssl.org/docs/manmaster/man1/openssl.html) CLI. For the purpose of this tutorial, you'll use the Prisma Cloud API.

<Instruction>

Open [`https://api.cloud.prisma.sh/`](https://api.cloud.prisma.sh/) in your browser and send the following query:

```graphql
{
  generateKeypair {
    public
    private
  }
}
```

</Instruction>

This query takes a few seconds and then returns a public and a private key.

<Instruction>

From the response, copy the public key and add it as the `CLUSTER_PUBLIC_KEY` to your `.env` file:

```(path="digital-ocean-demo/.env")
CLUSTER_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\r\n [ long key omitted ] -----END PUBLIC KEY-----\r\n",
```

</Instruction>

<Instruction>

Next, you need to remove the comment in front of the `environment.CLUSTER_PUBLIC_KEY` property in `docker-compose.yml`:

```yml(path="digital-ocean-demo/docker-compose.yml")
# ... other properties
environment:
  PORT: $PORT
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
  CLUSTER_PUBLIC_KEY: $CLUSTER_PUBLIC_KEY # remove comment here
  BUGSNAG_API_KEY: ""
  ENABLE_METRICS: "0"
  JAVA_OPTS: "-Xmx1G"
# ... other properties
```

</Instruction>

<Instruction>

Inside the `digital-ocean-demo` directory, run the following command to submit the new property along with its environment variable:

```sh
docker-compose up -d
```

</Instruction>

Awesome - your cluster is now secured and only people who have access to your private key will be able to access it from now on!

Next, you need to make the private key available to the CLI so your `prisma1 deploy` and similar commands can be properly authenticated. The way to do this is by setting it as the `clusterSecret` in `~/.prisma/config.yml`:

<Instruction>

Open `~/.prisma/config.yml` and paste the `private` key you received as the response from the previous `generateKeypair` query as the `clusterSecret` into it. The `digital-ocean-cluster` entry will then look similar to this:

```yml(path="/.prisma/config.yml")
clusters:
  digital-ocean-cluster:
    host: 'http://__DROPLET_IP_ADDRESS__:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY-----\r\n[ long key omitted ]]\r\n-----END RSA PRIVATE KEY-----\r\n"
```

</Instruction>

That's it - from now on all deploys to the cluster will be authenticated using the `clusterSecret`.

<!--
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

Change the host to use the ip of your Digital Ocean Droplet.

Copy the output of the following command and replace the content of `clusterSecret` for the new cluster:

```sh
cat private.pem | sed -n -e 'H;${x;s/\n/\\\\r\\\\n/g;p;}'
```

to restart the Prisma cluster:

```
docker-compose kill
docker-compose up -d
``` -->
