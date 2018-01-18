---
alias: eep0ugh1wa
description: Frequently asked questions about any technical matters regarding Prisma.
---

# Technical

### Can I host Prisma myself?

There are basically two ways for hosting Prisma:

- Do it yourself (using any available cloud provider, such as Digital Ocean, AWS, Google Cloud, ...)
- In the Prisma Cloud (coming soon)

### How do I connect my database to Prisma?

At the moment, Prisma only supports MySQL as a database technology (with [a lot more](https://github.com/graphcool/graphcool/issues/1006) planned in the future). Connecting an existing MySQL database will require the following steps:

1. Translate your SQL schema into a GraphQL data model written in [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51)
1. Deploy a Prisma service with that data model
1. If your database previously contained some data, [import the data](!alias-caith9teiy) into your Prisma service

If you want to migrate from an existing data source, you can check out our plans to support automatic data imports [here](https://github.com/graphcool/graphcool/issues/1410).

### What databases does Prisma support?

**MySQL** is the very first database supported by Prisma. Here's a list of more databases to be supported soon:

- **Postgres** (see [feature request)(https://github.com/graphcool/prisma/issues/1641))
- **MongoDB** (see [feature request)(https://github.com/graphcool/prisma/issues/1643))
- Oracle
- MS SQL
- ArangoDB
- Neo4j
- Druid
- Dgraph

If you have any preferences on which of these you'd like to see implemented first, you can create a new feature request asking for support or give a +1 for an existing one.

### What are the benefits of GraphQL?

GraphQL comes with a plethora of benefits for application development:

- GraphQL allows client applications to **request exactly the data they need from an API** (which reduces network traffic and increases performance).
- When accessing a GraphQL API from the frontend, you can use a GraphQL client library (such as Apollo Client or Relay) which **reduces frontend boilerplate** and provides **out-of-the-box support for features like caching, optimistic UI updates, realtime functionality, offline support and more**.
- GraphQL APIs are based on a strongly typed [schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) which effectively provides a way for you to have a **strongly typed API layer**. This means developers can be confident about the operations an API allows and the shape of the responsed to be returned by the server.
- GraphQL has an **amazing ecosystem of tooling** which greatly improve workflows and overall developer experience. For example, [GraphQL Playground](!alias-chaha125ho) which provides an interactive IDE for sending queries and mutations to an API (which can even be used by non-technical audiences). Another example are [GraphQL bindings](!alias-quaidah9ph) which enable to compose existing GraphQL APIs like LEGO bricks.
- The GraphQL ecosystem is made the **fantastic GraphQL community** who is putting in a lot of thought as well as hard work for how GraphQL can be evolved in the future and what other tools developers will benefit from.

### How do backups work

Since Prisma is only a layer _on top of your database_ but you still have full control over the database itself, you have the full power and flexibility regarding your own backup strategy.

Prisma also offers a [data export](!alias-pa0aip3loh) feature which you can use to create continuous backups for your data from the API layer.
