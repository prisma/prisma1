---
alias: texoo6aemu
description: Learn how to deploy your Prisma database service to Digital Ocean manually.
---

# Digital Ocean (manual)

This section describes how to set up a fully functioning Prisma server on Digital Ocean in less than 20 minutes.

In the following procedure you will connect to your droplet with ssh, and manually install all required dependencies. Most users should use the procedure described in [Digital Ocean (Docker Machine)](!alias-texoo9aemu) instead.

[Digital Ocean](https://www.digitalocean.com/) is an easy-to-use provider of virtual servers. They offer configurable compute units of various sizes, called [Droplets](https://www.digitalocean.com/products/droplets/).

<InfoBox>

The setup described in this section does not include features you would normally expect from production-ready servers, such as _automated backups_ and _active failover_. We will add more guides in the future describing a more complete setup suitable for production use.

</InfoBox>

## 1. Register at Digital Ocean

<Instruction>

If you haven't already, you need to start by creating an account at for Digital Ocean. You can do so [here](https://cloud.digitalocean.com/registrations/new).

</Instruction>


## 2. Create your Digital Ocean Droplet

You can now go ahead and create your Droplet. First pick the latest Ubuntu Distribution, as of this writing it is `18.04.1 x64`. Next pick a region that is close to your end user. If this Prisma will be used for development, you should pick the region closest to you. Set a name for your droplet, `prisma` for example.

Relax while your droplet is being created. This should take less than a minute.

Once you have your droplet available, you need to generate a ssh key which is needed to connect to your Droplet

<Instruction>

Follow the instructions [here](https://www.digitalocean.com/docs/droplets/how-to/add-ssh-keys/) to generate your ssh key

</Instruction>

## 3. Install Prisma on your Droplet

Digital Ocean will show you the ip address. If you are using MacOS or Linux you can now connect like this:

```sh
ssh root@37.139.15.166

# You can optionally specify the path to your ssh key:
ssh root@37.139.15.166 -i /Users/demo/.ssh/id_rsa_do
```

You need to install both Docker and Docker Compose. Digital Ocean has a detailed guide for installation that you can find [here](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04) and [here](https://www.digitalocean.com/community/tutorials/how-to-install-docker-compose-on-ubuntu-16-04).

For the quick path, execute these commands on your droplet:

```sh
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -y docker-ce
sudo curl -L https://github.com/docker/compose/releases/download/1.22.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
```

Install node:

```sh
curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
sudo apt-get install -y nodejs
```

Install Prisma:

```sh
npm -g install prisma
```

Prisma is now installed. Let's connect a service next.

## 4. Connect the Service

We're going to setup the infrastructure needed to run the Prisma database API on the droplet.

In your droplet, create a `docker-compose.yml` file

```sh
touch docker-compose.yml
```
In this file, add the configuration below:

```yml
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
        managementApiSecret: my-secret
        databases:
          default:
            connector: mysql
            host: mysql
            port: 3306
            user: root
            password: prisma
            migrations: true
  mysql:
    image: mysql:5.7
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: prisma
    volumes:
      - mysql:/var/lib/mysql
volumes:
  mysql:
```

In the config where you see `managementApiSecret`, provide a secret you would like to use to authenticate requests to the service. In our example we are using `my-secret`.

Now run

```sh
docker-compose up -d
```

This will fetch the docker images for both `prisma` and `mysql`. To verify that docker is running these images, run:

```sh
docker ps
```

```sh
CONTAINER ID        IMAGE                       COMMAND                  CREATED             STATUS              PORTS                    NAMES
24f4dd6222b1        prismagraphql/prisma:1.12   "/bin/sh -c /app/sta…"   15 seconds ago      Up 1 second         0.0.0.0:4466->4466/tcp   root_prisma_1
d8cc3a393a9f        mysql:5.7                   "docker-entrypoint.s…"   15 seconds ago      Up 13 seconds       3306/tcp                 root_mysql_1
```

## 5. Deploy the Service

Now that your infrastructure is running via docker, we will deploy the prisma service.

For us to enable deploys from our local machines, open up `~/.prisma/config.yml` in your favorite editor on YOUR machine.

It is likely that you already have one or more services defined in this file. Add another service and give it a descriptive name. For example do-cluster:

```yml
clusters:
  local:
    host: 'http://localhost:4466'
  do-cluster:
    host: 'http://37.139.15.166:4466'
```

Next we are going to bootstrap a new project via `prisma init`. On your local machine input:

```sh
prisma init hello-world
```

Following the interactive prompt, select `Use other server`. Enter the ip of the digital ocean droplet, and follow the prompt for adding the management secret.

This command should output:

```sh
Created 3 new files:

prisma.yml           Prisma service definition
datamodel.graphql    GraphQL SDL-based datamodel (foundation for database)
.env                 Env file including PRISMA_API_MANAGEMENT_SECRET
```

Next `cd` into the `hello-world` directory generated from the `prisma init` command and run:

```sh
prisma deploy
```

```sh
Creating stage default for service default ✔
Deploying service `default` to stage `default` to server `default` 653ms

Changes:

  User (Type)
  + Created type `User`
  + Created field `id` of type `GraphQLID!`
  + Created field `name` of type `String!`
  + Created field `updatedAt` of type `DateTime!`
  + Created field `createdAt` of type `DateTime!`

Applying changes 1.2s

Your Prisma GraphQL database endpoint is live:

  HTTP:  http://37.139.15.166:4466
  WS:    ws://37.139.15.166:4466
```

Connect to the `/management` endpoint in a browser. If your droplet has the ip `37.139.15.166` open the following webpage: `http://37.139.15.166:4466/management`. If a GraphQL Playground shows up, your Prisma server is set up correctly.

Finally, connect to the `/` endpoint in a browser. If your droplet has the ip `37.139.15.166` open the following webpage: `http://37.139.15.166:4466`. You should be able to explore the schema reflected in your `datamodel.graphql`.
