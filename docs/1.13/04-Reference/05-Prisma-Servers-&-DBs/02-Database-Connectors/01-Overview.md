---
alias: eiw6ahgiet
description: Connectors are used to connect a Prisma server to one or more databases.
---

# Overview

Connectors are responsible for connecting your [Prisma server](!alias-eu2ood0she) to a database. There are two primary decisions you have to make when connecting a database:

- **Multitenancy**: Should the database support multiple isolated Prisma services?
- **Migrations**: Should Prisma migrate the database structure?

## Multitenancy

If a database is configured to operate in the multitenancy mode, a new database schema is created for each service deployed to the database. This way each service is completely isolated. The name of the database schema is constructed by combining the service and stage name.

If the database is operating in singletenancy mode, the name of the database schema must be provided when configuring your Prisma server and all services deployed will use the same database schema. This is mostly useful if your have an existing database that is controled by an existing application, and want to use Prisma to create a GraphQL API for that database.

## Migrations

If migrations are enabled, deploying a Prisma service will migrate the structure of the connected database. In these cases, the Prisma CLI will be the main interface to govern the structure of the database based on the [data model](!alias-eiroozae8u) of your Prisma APIs.

For existing databases that might be part of an existing application, it is useful to configure Prisma to not migrate the connected database. In these cases, the Prisma CLI will use [introspection](!alias-aeb6diethe) to learn about the database schema and translate that into a corresponding [data model](!alias-eiroozae8u) for your Prisma API.

## Configuration

> Currently MySQL only support multitennancy and enabled migrations. Postgres support multitennancy and enabled migrations as well as singletenancy with disabled migrations. In the future more combinations will be enabled.

* Read how to configure a [MySQL database](!alias-ajai7auhoo)

* Read how to configure a [Postgres database](!alias-neix6nesie)

## More info

You can learn more about connectors [here](https://github.com/graphcool/prisma/issues/1751) and [here](https://github.com/graphcool/prisma/#database-connectors).
