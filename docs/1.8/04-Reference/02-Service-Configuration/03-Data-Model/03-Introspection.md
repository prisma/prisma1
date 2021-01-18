---
alias: aeb6diethe
description: Introspection
---

# Introspection

## Overview

When connecting Prisma to an existing database it can be tedious to write the data model from scratch. To automate this process you can use the `prisma1 introspect` command to generate a data model based on your actual database schema.

The generated SDL serves as a basis for your Prisma service, but you can easily make modifications afterwards as you see fit. Some common modifications include hiding a table from the GraphQL API or making a column to a different name.

## Limitations

Currently database introspection only works with Postgres. Additionally there is a set of documented [known limitations](https://github.com/graphcool/prisma/issues/2377).

## Introspecting a database

There are two ways you can use the CLI to introspect a database: Using the interactive `prisma1 init` command and using the dedicated `prisma1 introspect` command.

During the prisma1 init flow you can choose to connect to an existing database with data. The CLI will ask for database connection information and verify that it can establish a successful connection. If the information is correct, the CLI will introspect the database and show you a summary

![](https://i.imgur.com/cNIeeJf.png)

The CLI has now generated all the files you need to run Prisma on your existing database, including your data model:

- datamodel.graphql
- docker-compose.yml
- prisma.yml

If you just want to generate the datamodel you can use the `prisma1 introspect` command. You will need to provide your database connection details as above. The CLI will generate a single file for you:

- datamodel-[TIMESTAMP].graphql

The timestamp component allows you to use the introspect command for an existing Prisma service without overriding your existing datamodel.

## Deploying

After you have made your changes to the generated data model it is time to deploy your Prisma service. If you used `prisma1 init` to setup your service, your Prisma server is already configured to not apply migrations to your database. If your are setting up Prisma manually you should ensure that your PRISMA_CONFIG has set `migrations: false` like this:

```yml
PRISMA_CONFIG: |
  port: 4466
  databases:
    default:
      connector: postgres
      migrations: false
      host: docker.for.mac.localhost
      port: '5432'
      user: postgres
      password: postgres
      database: postgres
      schema: public
```

This ensures that Prisma will change the GraphQL API based on the data model, but it will not perform migrations of your database.

## Generated data model

### Relations

#### inline relation column

This is the most common way to represent relationships in a SQL database.

```sql
CREATE TABLE product (
  id           serial PRIMARY KEY UNIQUE
, description  text NOT NULL
);

CREATE TABLE bill (
  id         serial PRIMARY KEY UNIQUE
, bill       text NOT NULL
, product_id int REFERENCES product (id) ON UPDATE CASCADE
);
```

The `product_id` column on the `bill` table has a foreign key constraint, which Prisma interprets as a one to many relationship.

```graphql
type Bill @pgTable(name: "bill") {
  bill: String!
  id: Int! @unique
  product: Product @pgRelation(column: "product_id")
}

type Product @pgTable(name: "product") {
  description: String!
  id: Int! @unique
  bills: [Bill!]!
}
```

The generated `bill` type has a `product` field. Prisma generates this name by taking the column name and remove any of these suffixes: `_id`, `ID`, `Id`.

The `Product` type has a `bills` field which is generated from the name of the `bill` table.

You can rename any relation field to change what they are called in the final GraphQL API.

#### Relation table

Relational schemas often rely on a relation table to connect two tables. This example represents a relation between Bills and Products.

```sql
CREATE TABLE product (
  id         serial PRIMARY KEY UNIQUE
, product    text NOT NULL
);

CREATE TABLE bill (
  id       serial PRIMARY KEY UNIQUE
, bill     text NOT NULL
);

CREATE TABLE bill_product (
  bill_id    int REFERENCES bill (id) ON UPDATE CASCADE ON DELETE CASCADE
, product_id int REFERENCES product (id) ON UPDATE CASCADE
);
```

As you can see from the generated data model, Prisma assumes that there is a many to many relationship between Bills and Products. This is because this is the maximum flexibility allowed by the example SQL schema. 

```graphql
type Bill @pgTable(name: "bill") {
  bill: String!
  id: Int! @unique
  products: [Product!]! @pgRelationTable(table: "bill_product" name: "bill_product")
}

type Product @pgTable(name: "product") {
  id: Int! @unique
  product: String!
  bills: [Bill!]! @pgRelationTable(table: "bill_product" name: "bill_product")
}
```

It is common to have additional application logic that ensures that for example only one Bill can be related to a Product. If this is the case for you, simply go ahead and modify the generated data model to match your business constraints.

#### Relation table with extra columns

If a relation table contains extra information Prisma chooses to treat the relation table as a dedicated type. This way you have the full flexibility to set and read the extra column using normal queries and nested mutations.

```sql
CREATE TABLE product (
  id         serial PRIMARY KEY UNIQUE
, product    text NOT NULL
);

CREATE TABLE bill (
  id       serial PRIMARY KEY UNIQUE
, bill     text NOT NULL
);

CREATE TABLE bill_product (
  bill_id    int REFERENCES bill (id) ON UPDATE CASCADE ON DELETE CASCADE
, product_id int REFERENCES product (id) ON UPDATE CASCADE
, some_other_column text NOT NULL
);
```

```graphql
type Bill @pgTable(name: "bill") {
  bill: String!
  id: Int! @unique
  bill_products: [Bill_product]
}

type Bill_product @pgTable(name: "bill_product") {
  bill: Bill @pgRelation(column: "bill_id")
  product: Product @pgRelation(column: "product_id")
  some_other_column: String!
}

type Product @pgTable(name: "product") {
  id: Int! @unique
  product: String!
  bill_products: [Bill_product]
}
```
