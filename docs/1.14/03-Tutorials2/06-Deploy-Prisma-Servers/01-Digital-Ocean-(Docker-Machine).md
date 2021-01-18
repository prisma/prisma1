---
alias: texoo9aemu
description: Learn how to deploy your Prisma database service to Digital Ocean using Docker Machine.
---

# Digital Ocean (Docker Machine)

In this tutorial, you will learn how to create a Prisma [server](!alias-eu2ood0she) on Digital Ocean and deploy your Prisma services to it.

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

For example

```sh
docker-machine version 0.15.0, build b48dc28d
```

## 2. Register at Digital Ocean

<Instruction>

If you haven't already, you need to start by creating an account at for Digital Ocean. You can do so [here](https://cloud.digitalocean.com/registrations/new).

</Instruction>

Once you have your account available, you need to generate a personal access token which is needed to create your Droplet.

<Instruction>

Follow the instructions [here](https://docs.docker.com/machine/examples/ocean/#step-2-generate-a-personal-access-token) to obtain your personal access token.

Make sure to save the token, it will soon disappear after creating it and you'll need it in the next step.

</Instruction>

## 3. Create your Digital Ocean Droplet

You can now go ahead and create your Droplet. The personal access token from the previous step will be used to associate the Droplet with your Digital Ocean account.

<Instruction>

In your terminal, use [`docker-machine create`](https://docs.docker.com/machine/reference/create/) to create your Droplet using the [`digitalocean`](https://docs.docker.com/machine/drivers/digital-ocean/) driver. Note that you need to replace the `__TOKEN__` placeholder with your personal access token from the previous step:

```sh
docker-machine create --driver digitalocean --digitalocean-access-token __TOKEN__ --digitalocean-size 1gb prisma
```

This will take a few minutes.

</Instruction>

The `--digitalocean-size 1gb` argument indicates that the Droplet receives 1GB of memory. The last argument of the command, `prisma`, determines the _name_ of the Droplet.

You will now have a running Droplet that can be controlled by `docker-machine`. You can list your current Droplets by running the following command:

```sh
docker-machine ls
```

## 4. Install Prisma on your Droplet

Now that you have your Droplet up and running, you can install Prisma on it. As [the Prisma infrastructure runs on Docker](!alias-aira9zama5), you could theoretically do this by using the Docker CLI directly. In that case, you could use the [Docker Compose file](!alias-aira9zama5#docker-compose-file) as foundation and run the required commands (e.g. [`docker-compose up -d`](https://docs.docker.com/compose/reference/up/)) yourself.

The Prisma CLI offers commands that you can use for convenience so you do not have to fiddle with Docker yourself. In this tutorial, you'll take advantage of these commands - under the hood they will simply configure your Docker environment and invoke the required CLI commands.

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
2. Create `.env` to configure the environment variables used by Docker
3. Run `docker-compose up -d` to install Prisma on the Droplet

Let's get started!

<Instruction>

First, go ahead and create a new directory on your file system (on your local machine) where you'll place all the files for your project, you can call it `prisma-digital-ocean-demo`.

</Instruction>

<Instruction>

Inside that directory, create a new file called `docker-compose.yml` and copy the following contents into it:

```yml(path="prisma-digital-ocean-demo/docker-compose.yml")
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.12
    restart: always
    ports:
    - "4466:4466"
    environment:
      PRISMA_CONFIG: |
        port: 4466
        databases:
          default:
            connector: mysql
            active: true
            host: prisma-db
            port: 3306
            user: root
            password: prisma
  db:
    container_name: prisma-db
    image: mysql:5.7
    restart: always
    environment:
      MYSQL_USER: root
      MYSQL_ROOT_PASSWORD: prisma
```

</Instruction>

Note the comment in front of the `environment.PRISMA_CONFIG.managementApiSecret` property. For now, we don't provide a secret to the Prisma server which is going to enable _everyone_ with access to the IP address of the Droplet to deploy and delete services to and from it! We'll add the security layer afterwards!

> **Note**: To learn more about the variables configured here, check the corresponding [reference docs](!alias-aira9zama5#structure).

With these files in place, you can go ahead and install Prisma!

<Instruction>

Inside the `prisma-digital-ocean-demo` directory, run the following command:

```sh(path="prisma-digital-ocean-demo")
docker-compose up -d
```

</Instruction>

Remember that thanks to Docker Machine, this command will now run against the remote host, i.e. your Digital Ocean Droplet. The `-d` option starts the container in _detached mode_, meaning it will run as a background process.

Awesome, you now have your Prisma server available on the Digital Ocean Droplet!

## 5. Deploy a Prisma service

You're now ready to deploy a Prisma service to your droplet

<Instruction>

Inside the `prisma-digital-ocean-demo` directory, run the following command:

```sh(path="prisma-digital-ocean-demo")
prisma1 init hello-world
```

</Instruction>

<Instruction>

From the interactive prompt, choose the `Use other server` option. Next, supply the `host` address to the prompt asking for the server endpoint. Last set a name for this service, in this case we will use `hello-world` and a stage from the interactive prompt in this case `dev`.

</Instruction>

This creates a new directory that has the initial setup for a minimal Prisma service:

```
hello-world
  ├── datamodel.graphql - GraphQL SDL-based datamodel (foundation for database)
  └── prisma.yml - Prisma service definition
```

Next, you can go ahead and deploy the Prisma service.

<Instruction>

Navigate into the newly-created `hello-world` directory and deploy the service:

```sh(path="prisma-digital-ocean-demo")
cd hello-world
prisma1 deploy
```

</Instruction>


The CLI is now going to deploy the service to that droplet.

<InfoBox>

The Prisma CLI in this case acts as a _client_ for the Prisma Management API which is used to manage services on a specific seerrv. If you want to explore the Management API yourself, you can navigate your browser to `http://__DROPLET_IP_ADDRESS__:4466/management`. Similar to before, you'll have to replace the `__DROPLET_IP_ADDRESS__` placeholder with the actual IP address of your Digital Ocean Droplet.

</InfoBox>

This is it! Your Prisma service is now running on your local server and can be access through the endpoint that was printed by the `prisma1 deploy` command! It will look similar to this: `http://__DROPLET_IP_ADDRESS__:4466/hello-world/dev`.

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

## 6. Enable server authentication

As noted before, _everyone_ with access to the endpoint of your server (`http://__DROPLET_IP_ADDRESS__:4466/`) will be able to access your server in any way they like. This means they can add new services or delete existing ones and also get full read/write-access to the application data from these services. This is a major security issue and should never be the case in any sort of production environment! Let's fix it.

Here's a quick overview of the steps you need to perform (like before, we'll go through each step in detail afterwards):

1. Add a managementApiSecret to your docker-compose.yml and reapply your compose file with `docker-compose up -d`
2. Redeploy the Prisma service

To generate a `managementApiSecret`, you can use any mechanism you wish.

```yml(path="prisma-digital-ocean-demo/docker-compose.yml")
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.12
    restart: always
    ports:
    - "4466:4466"
    environment:
      PRISMA_CONFIG: |
        managementApiSecret: my-secret-secret
        port: 4466
        databases:
          default:
            connector: mysql
            active: true
            host: prisma-db
            port: 3306
            user: root
            password: prisma
  db:
    container_name: prisma-db
    image: mysql:5.7
    restart: always
    environment:
      MYSQL_USER: root
      MYSQL_ROOT_PASSWORD: prisma
```

<Instruction>

Inside the `prisma-digital-ocean-demo` directory, run the following command to submit the new property along with its environment variable:

```sh
docker-compose up -d
```

</Instruction>

Awesome - your server is now secured and only people who have access to your private key will be able to access it from now on!

Let's try and deploy the server without changing anything:

```sh
prisma1 deploy
Creating stage dev for service hello-world !
 ▸    Server at http://104.131.127.241:4466:4466 requires a cluster secret. Please provide it with the env var PRISMA_MANAGEMENT_API_SECRET
```

Perfect this is what we expected! Now you need to make the private key available to the CLI so your `prisma1 deploy` and similar commands can be properly authenticated.

<Instruction>

In the `hello-world` directory, create a `.env` file. `prisma.yml` understands variables from this file, add your `PRISMA_MANAGEMENT_API_SECRET` like so:


```(path="hello-world/.env")
PRISMA_MANAGEMENT_API_SECRET=my-secret-secret
```

Now run `prisma-deploy` and you should be have a successful deploy!

</Instruction>

That's it - from now on all deploys to the server will be authenticated using the `PRISMA_MANAGEMENT_API_SECRET`
