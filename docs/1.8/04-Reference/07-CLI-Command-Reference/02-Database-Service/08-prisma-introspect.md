---
alias: jusreo9tn4
description: Introspect an existing database to generate the data model
---

# `prisma introspect`

Creates a data model by introspecting an existing database

#### Usage

```sh
prisma introspect
```

#### Limitations

Currently only works for Postgres databases.

#### Examples

##### Introspect an existing Postgres database

```sh
~/my-app $ prisma introspect
? What kind of database do you want to introspect? Postgres
? Enter database host localhost
? Enter database port 3306
? Enter database user prisma
? Enter database password ******
? Enter name of existing database prisma-db
? Enter name of existing schema public

Introspecting database 402ms
Created datamodel mapping based on 7 database tables.

Created 1 new file:               

  datamodel-[TIMESTAMP].graphql    GraphQL SDL-based datamodel (derived from existing database)
```

The generated datamodel file will contain a timestamp in its name to avoid overriding your existing datamodel.graphql file.
