---
alias: texoo6aemu
description: Learn how to deploy your Prisma database service to Digital Ocean manually.
---


# Digital Ocean (manual)

This section describes how to set up a fully functioning Prisma server on Digital Ocean in less than 20 minutes.

In the following procedure you will connect to your droplet with ssh, and manually install all required dependencies. Most users should use the procedure described in [Digital Ocean (Docker Machine)](!alias-texoo9aemu) instead.

Digital Ocean is an easy to use provider of virtual servers. They offer different compute sizes called droplets. The 10$ droplet is a good starting point for a Prisma server.

> The setup described in this section does not include features you would normally expect from production ready servers, such as backup and active failover. We will add more guides in the future describing a more complete setup suitable for production use.

## Creating a droplet

If you are new to Digital Ocean you should start by creating an account at https://www.digitalocean.com/

After you log in, create a new Droplet:

1. Pick the latest Ubuntu Distribution. As of this writing it is 17.10.
2. Pick a standard droplet. The 10$/mo droplet with 1 GB memory is a good starting point.
3. Pick a region that is close to your end user. If this Prisma will be used for development, you should pick the region closest to you.
4. Create an ssh key that you will use to connect to your droplet. Follow the guide provided by Digital Ocean.
5. Set a name for your droplet. For example `prisma`
6. Relax while your droplet is being created. This should take less than a minute.

## Installing Prisma

### Connect

Digital Ocean will show you the ip address. If you are using MacOS or Linux you can now connect like this:

```sh
ssh root@37.139.15.166

# You can optionally specify the path to your ssh key:
ssh root@37.139.15.166 -i /Users/demo/.ssh/id_rsa_do
```

### Install Docker & node & Prisma

You need to install both Docker and Docker Compose. Digital Ocean has a detailed guide for installation that you can find [here](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-16-04) and [here](https://www.digitalocean.com/community/tutorials/how-to-install-docker-compose-on-ubuntu-16-04).

For the quick path, execute these commands on your droplet:

```sh
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install -y docker-ce
sudo curl -L https://github.com/docker/compose/releases/download/1.18.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
```

Install node:

```sh
curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
sudo apt-get install -y nodejs
```

Install Prisma:

```sh
npm -g install prisma1
prisma1 local start
```

Prisma is now installed and running. At this point you should verify that everything is running correctly.

### Verify Prisma Installation

Verify that the correct docker containers are running:

```sh
> docker ps
CONTAINER ID        IMAGE                                COMMAND                  CREATED             STATUS              PORTS                    NAMES
e5d2e028eba9        prismagraphql/prisma:1.0.0   "/app/bin/single-ser…"   51 seconds ago      Up 49 seconds       0.0.0.0:4466->4466/tcp   prisma
42d9d5acd0e4        mysql:5.7                            "docker-entrypoint.s…"   51 seconds ago      Up 49 seconds       0.0.0.0:3306->3306/tcp   prisma-db
```

Run `prisma1 cluster list` and verify that the local cluster is in the output.

Finally, connect to the `/cluster` endpoint in a browser. If your droplet has the ip `37.139.15.166` open the following webpage: `http://37.139.15.166:4466/cluster`

If a GraphQL Playground shows up, your Prisma cluster is set up correctly.

### Connect and Deploy a Service

Your Prisma cluster is secured by a public/private keypair. In order to connect with the `prisma` CLI on your local machine, you need to configure security settings.

#### Copy cluster configuration from server

On your digital ocean droplet, copy the clusterSecret for the `local` cluster:

```sh
> cat ~/.prisma/config.yml
clusters:
  local:
    host: 'http://localhost:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
```

#### Add new cluster in local configuration

On your local development machine open `~/.prisma/config.yml` in your favourite editor. It is likely that you already have one or more clusters defined in this file. Add another cluster and give it a descriptive name. For example `do-cluster`:

```yml
clusters:
  local:
    host: 'http://localhost:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
  do-cluster:
    host: 'http://37.139.15.166:4466'
    clusterSecret: "-----BEGIN RSA PRIVATE KEY----- [ long key omitted ] -----END RSA PRIVATE KEY-----\r\n"
```

Change the host to use the ip of your Digital Ocean droplet.

#### Deploy a service

On your local machine, verify that the cluster configuration is being picked up correctly:

```sh
prisma1 cluster list
```

The output should include your newly added cluster.

Now you can create a new service and deploy it to the cluster:

```sh
prisma1 init
prisma1 deploy
```

Pick the new cluster in the deployment option. You should see output similar to this:

```sh
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

  HTTP:  http://37.139.15.166:4466/demo/dev
  WS:    ws://37.139.15.166:4466/demo/dev
```
