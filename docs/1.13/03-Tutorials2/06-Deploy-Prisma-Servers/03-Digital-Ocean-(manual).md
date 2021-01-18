---
alias: texoo6aemu
description: Learn how to deploy your Prisma database service to Digital Ocean manually.
---

# Digital Ocean (manual)

This section describes how to set up a fully functioning Prisma server on Digital Ocean in less than 20 minutes.

In the following procedure you will connect to your Droplet with SSH, and manually install all required dependencies. Most users should use the procedure described in [Digital Ocean (Docker Machine)](!alias-texoo9aemu) instead.

[Digital Ocean](https://www.digitalocean.com/) is an easy-to-use provider of virtual servers. They offer configurable compute units of various sizes, called [Droplets](https://www.digitalocean.com/products/droplets/).

> The setup described in this section does not include features you would normally expect from production-ready servers, such as _automated backups_ and _active failover_. We will add more guides in the future describing a more complete setup suitable for production use.

## 1. Register at Digital Ocean

If you haven't already, you need to start by creating an account at for Digital Ocean. You can do so [here](https://cloud.digitalocean.com/registrations/new).

## 2. Create your Digital Ocean Droplet

In this section we are going to walk through setting up a `Droplet`.

![droplet-create](https://i.imgur.com/5IXcUVw.png)

First pick the latest Ubuntu Distribution, as of this writing it is `18.04.1 x64`.

![distribution](https://i.imgur.com/vYTNX9q.png)

Next pick a region that is close to your end user. You should pick the region closest to you. Set a name for your Droplet, `prisma` for example.

![name](https://i.imgur.com/q1rXXe2.png)

Relax while your Droplet is being created. This should take less than a minute.

Once you have your Droplet available, you need to generate a key which is needed to connect to your Droplet

Follow the instructions [here](https://www.digitalocean.com/docs/droplets/how-to/add-ssh-keys/) to generate your SSH key

## 3. Install Prisma on your Droplet

Digital Ocean will show you the IP address. If you are using MacOS or Linux you can now connect like this:

```sh
ssh root@__IP_ADDRESS__ -i /Users/.ssh/id_rsa_do
```

Adding our Droplet's IP address this looks like the following:

```sh
ssh root@37.139.15.166 -i /Users/.ssh/id_rsa_do
```

You need to install both Docker and Docker Compose. Digital Ocean has detailed guides for installation that you can find [here](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04) and [here](https://www.digitalocean.com/community/tutorials/how-to-install-docker-compose-on-ubuntu-16-04).

For the quick path, execute these commands on your Droplet:

```sh
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -y docker-ce
sudo curl -L https://github.com/docker/compose/releases/download/1.22.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
```

Install Node.JS:

```sh
curl -sL https://deb.nodesource.com/setup_10.x | sudo -E bash -
sudo apt-get install -y nodejs
```

Install the Prisma CLI:

```sh
npm -g install prisma1
```

The Prisma CLI is now installed on your Digital Ocean Droplet. Next, you need to configure and start the Prisma server.

## 4. Start the Prisma server

In this section you're going to setup the infrastructure needed to deploy the Prisma service to the Droplet.

In your Droplet, create a `docker-compose.yml` file

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
  mysql: ~
```

The `managementApiSecret` is used to secure your Prisma server. It is specified in the server's Docker configuration and later used by the Prisma CLI to authenticate its requests against the server.

Now run the following command in your terminal:

```sh
docker-compose up -d
```

This will fetch the Docker images for both `prisma` and `mysql`. To verify that the Docker containers are running, run the following command:

```sh
docker ps
```

```sh
CONTAINER ID        IMAGE                       COMMAND                  CREATED             STATUS              PORTS                    NAMES
24f4dd6222b1        prismagraphql/prisma:1.12   "/bin/sh -c /app/sta…"   15 seconds ago      Up 1 second         0.0.0.0:4466->4466/tcp   root_prisma_1
d8cc3a393a9f        mysql:5.7                   "docker-entrypoint.s…"   15 seconds ago      Up 13 seconds       3306/tcp                 root_mysql_1
```

## 5. Deploy the Service

Now that your Prisma server and its database are running via Docker, you will deploy the Prisma service.

We are going to bootstrap a new Prisma service via `prisma1 init`.

On your local machine ,run the following command in your terminal:

```sh
prisma1 init hello-world
```

Following the interactive prompt, select `Use other server`. Enter the IP of the digital ocean Droplet, a `service name` for your Prisma service, a `service stage`, and follow the prompt for adding the management secret.

This command should output:

```sh
Created 3 new files:

prisma.yml           Prisma service definition
datamodel.graphql    GraphQL SDL-based datamodel (foundation for database)
.env                 Env file including PRISMA_API_MANAGEMENT_SECRET
```

Next navigate into the `hello-world` directory that was generated from `prisma1 init` and deploy your Prisma service with the following commands:

```sh
cd hello-world

prisma1 deploy
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

Connect to the `/management` endpoint in a browser. For example, if your Droplet has the IP address of 37.139.15.166 open the following webpage: http://37.139.15.166:4466/management. If a GraphQL Playground shows up, your Prisma server is set up correctly.

Finally, connect to the `/` endpoint in a browser. For example, if your Droplet has the IP address of 37.139.15.166 open the following webpage: http://37.139.15.166:4466. You can now explore the GraphQL API of your Prisma service.

You can send the following mutation in GraphQL Playground to create a new user:

```graphql
mutation {
  createUser(data: { name: "Alice" }) {
    id
    name
  }
}
```

With the newly created `User`, run a query by it's `id`:

```graphql
query {
  user(where: { id: "cjkar2d62000k0847xuh4g70o"}) {
    id
    name
  }
}
```
