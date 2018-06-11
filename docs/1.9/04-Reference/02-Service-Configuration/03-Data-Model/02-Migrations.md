---
alias: ao8viesh2r
description: Migrations
---

# Migrations

## multi-tenancy

When using Prisma you can connect databases in two different modes:

*multi-tenant*
When a database is configured to operate in multi-tenancy mode, you can use it to deploy as many Prisma services as you want. For each Prisma service  you deploy, a new database schema is created. This way your services are fully isolated. The database schema name is based on the name and stage of your service. This was the only way to use Prisma before the 1.8 release.

*single-tenant*
To use prisma with an existing database, you must configure it in single-tenancy mode by specifying the name of the schema that contains your data. When a database is operating in single-tenancy mode, all Prisma services configured for the database will use the same database schema.

Currently it is only possible to configure a single database and it has to be called default. In the future it will be possible to configure multiple named databases and use them from the same service.

## Disable migrations

> note: currently Prisma can only migrate databases operating in multi-tenancy mode. This will be improved in the future

If you are using Prisma together with a database that is controlled by an existing application, you probably do not want Prisma to migrate the database structure. You can disable Prisma migrations by setting the `migrations` configuration option to false:

```yml
port: 4466
  databases:
    default:
      connector: postgres
      host: localhost
      port: 5432
      user: root
      password: prisma
      database: prisma
      schema: my_schema
      migrations: false
```

Read more about server configuration options in [server configuration](!alias-eiw6ahgiet)

### Deploy your service

When migrations are disabled, deploying your service will not migrate the underlying database. Instead it will change the shape of the generated GraphQL API. Changing your data model allows you to hide tables, rename columns and much more. See [introspection](!alias-aeb6diethe) for more details.

## Enable migrations

Migrations are enabled by default. To make it more explicit you can set the `migrations` key to `true`.

When migrations is enabled, deploying a service will actively migrate the underlying database. Read more about how this works in the [data modelling](!alias-eiroozae8u) chapter.
