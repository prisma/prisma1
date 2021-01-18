---
alias: fae2ooth2u
description: Frequently asked questions about Prisma Cloud.
---

# Prisma Cloud

### What is a workspace?

You can roughly think of a workspace similar to a GitHub organization. It is a collection of personal Prisma Cloud accounts, Prisma servers and services. Here are a few hints how workspaces relate to these:

- One workspace can be associated with many personal accounts. One personal account be in many workspaces.
- One workspace can be associated with many Prisma servers. One Prisma server is always connected to exactly one workspace.
- Because one Prisma server can host many services, one workspace can be associated with many services.

> **Note**: The Prisma servers above all refer to private Prisma servers that were setup in the Prisma Cloud (as opposed to self-hosted and Demo servers for which above statements might not be true).

### How do I manage access rights and permissions among my team members?

Right now, every team member that's invited to join a project on Prisma Cloud has full access rights. More fine-grained management of access and permissions for team members is coming soon.

### What are Prisma servers? How do they relate to Prisma services?

A Prisma server is the _runtime environment_ for zero or more Prisma services. This means in order to deploy a Prisma service (using the `prisma1 deploy` command), you need to have a Prisma server available.

In general, there are three types of Prisma servers:

- **Local / self-hosted** (using [Docker](https://www.docker.com/)): You can create your own Prisma server locally or host them using a cloud provider of your choice. For example, follow [this](!alias-texoo9aemu) tutorial to learn how to host your own Prisma Cluste on Digital Ocean.
- **Demo** ([Prisma Cloud](https://www.prismagraphql.com/cloud)): Prisma Cloud offers free _Demo servers_ which you can use for learning, prototyping and development. Services deployed to Demo servers are rate limited (at 10 requests per 10 seconds) and have an upper bound in storage (100 MB). Note that you need a Prisma Cloud account to deploy to a Demo server.
- **Private** ([Prisma Cloud](https://www.prismagraphql.com/cloud)): A private Prisma server is connected to your own database which you provision when setting up the server.

### How do multi-staging development workflows work?

Because one Prisma server can host many services, you can deploy your services that represent the stages / environments (e.g. `dev`, `staging` and `prod`) to the same server. It is not _required_ to use multiple Prisma servers for your multiple stages.

However, to be certain that nothing that's happening in `dev` or `staging` can have negative effects on your `prod` stage, it is recommended to deploy your production environment to its own Prisma server. Ideally, you do the same for other stages / environments as well just to minimize the risk they're affecting each other.

It is of course also possible to use local Prisma server or the Prisma Cloud Demo server as development environments when appropriate.

### Is there a free version of Prisma Cloud?

It is free to deploy to Demo servers. However, when deploying services to Demo server they are rate limited and have an upper bound in storage capacity. For production use cases, you should always use a self-hosted or a private Prisma server.

### How do I connect a database?

Every Prisma server is backed by one database. In the future, it will be possible to connect multiple databases to the same Prisma server. The database is being connected to the Prisma server when the server is initially created.

### Are there automatic backups?

Because Prisma is only a layer _on top of your database_, you still have full control over the database itself. This means you have the full power and flexibility regarding your own backup strategy.

In the future Prisma Cloud is going to simplify backup workflows and, for example, enable automatic point-in-time restores.
