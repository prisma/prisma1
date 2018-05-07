---
alias: fae2ooth2u
description: Frequently asked questions about Prisma Cloud.
---

# Prisma Cloud

### What is a workspace?

You can roughly think of a workspace similar to a GitHub organization. It is a collection of personal Prisma Cloud accounts, clusters and services. Here are a few hints how workspaces relate to these:

- One workspace can be associated with many personal accounts. One personal account be in many workspaces.
- One workspace can be associated with many clusters. One cluster is always connected to exactly one workspace.
- Because one cluster can host many services, one workspace can be associated with many services.

> **Note**: The clusters above all refer to private clusters that were setup in the Prisma Clouds (as opposed to self-hosted and development clusters for which above statements might not be true).

### How do I manage access rights and permissions among my team members?

Right now, every team member that's invited to join a project on Prisma Cloud has full access rights. More fine-grained management of access and permissions for team members is coming soon.

### What are clusters? How do they relate to services?

A cluster is the _runtime environment_ for Prisma services. This means in order to deploy a Prisma service (using the `prisma deploy` command), you need to have _some_ cluster available.

In general, there are three types of clusters:

- **Local / self-hosted** (using [Docker](https://www.docker.com/)): You can create your own Prisma clusters locally or host them using a cloud provider of your choice. For example, follow [this](!alias-texoo9aemu) tutorial to learn how to host your own Prisma Cluste on Digital Ocean.
- **Development clusters** ([Prisma Cloud](https://www.prismagraphql.com/cloud)): Prisma Cloud offers free development clusters which you can use for learning, prototyping and development. Note that when deployed to a development cluster, your services will be rate limited (at 10 requests per 10 seconds) and have an upper bound in storage (100 MB). Note that you don't need a Prisma Cloud account to deploy to a development cluster.
- **Private clusters** ([Prisma Cloud](https://www.prismagraphql.com/cloud)): A private cluster is connected to your own database which you're provisioning when initially setting up the cluster.

### How do I create a new private cluster?

Private clusters are created through the Prisma Cloud Console. Follow this [tutorial](!alias-ua9gai4kie) to learn how it works or watch this 3-min demo [video](https://www.youtube.com/watch?v=jELE4KXJPn4&lc=).

### How do multi-staging development workflows work?

Because one cluster can host many services, you can deploy your services that represent the stages / environments (e.g. `dev`, `staging` and `prod`) to the same cluster. It is not _required_ to use multiple clusters for your multiple stages.

However, to be certain that nothing that's happening in `dev` or `staging` can have negative effects on your `prod` stage, it is recommended to deploy your production environment to its own cluster. Ideally, you do the same for other stages / environments as well just to minimize the risk they're affecting each other.

It is of course also possible to use local clusters or the Prisma Cloud development cluster as development environments when appropriate.

### Is there a free version of Prisma Cloud?

It is free to deploy to development clusters. However, when deploying services to development clusters they are rate limited and have an upper bound in storage capacity. For production use cases, you should always use a self-hosted cluster or a private cluster in the Prisma Cloud.

### How do I connect a database?

Every cluster is backed by one database. In the future, it will be possible to connect multiple databases to the same cluster.

The database is connected to the cluster when the cluster is initially created. Follow this [tutorial](!alias-ua9gai4kie) to learn how this works or watch this 3-min demo [video](https://www.youtube.com/watch?v=jELE4KXJPn4&lc=).

### Are there automatic backups?

Because Prisma is only a layer _on top of your database_, you still have full control over the database itself. This means you have the full power and flexibility regarding your own backup strategy.

In the future Prisma Cloud is going to simplify backup workflows and, for example, enable automatic point-in-time restores.