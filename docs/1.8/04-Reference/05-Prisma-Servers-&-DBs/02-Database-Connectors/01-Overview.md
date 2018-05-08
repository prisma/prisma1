---
alias: eiw6ahgiet
description: Connectors are used to connect a Prisma server to one or more databases.
---

# Overview

Connectors are responsible for connecting your [Prisma server](!alias-eu2ood0she) to a database. There are two kinds of connectors:

- **Active**: Manage and migrate the database schema
- **Passive**: Read and write data only

## Active connectors

Active connectors are used with **new ("greenfield") databases** that don't store any data and are not bound to a specific database schema. In these cases, the Prisma CLI will be the main interface to govern the structure of the database depending on the [data model](!alias-eiroozae8u) of your Prisma APIs.

## Passive connectors

Passive connectors are mainly used for **existing databases** that already have a databse schema and/or store some data. In these cases, the Prisma CLI will use _introspection_ to learn about the database schema and translate that into a corresponding [data model](!alias-eiroozae8u) for your Prisma API.

## More info

You can learn more about connectors [here](https://github.com/graphcool/prisma/issues/1751) and [here](https://github.com/graphcool/prisma/#database-connectors).