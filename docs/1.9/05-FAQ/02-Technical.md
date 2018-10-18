---
alias: eep0ugh1wa
description: Frequently asked questions about any technical matters regarding Prisma.
---

# Technical

### Can I host Prisma myself?

There are basically two ways for hosting Prisma:

- With [Prisma Cloud](https://www.prismagraphql.com/cloud/). Follow [this](!alias-ua9gai4kie) tutorial to started with Digital Ocean.
- Do it yourself (using any available cloud provider, such as Digital Ocean, AWS, Google Cloud, ...). Follow [this](!alias-texoo9aemu) tutorial to started.

### How do I connect my database to Prisma?

At the moment, Prisma supports MySQL as a database technology (with [a lot more](https://github.com/graphcool/prisma/issues/1751) coming in the future). Connecting an existing MySQL database will require the following steps:

1. Translate your SQL schema into a GraphQL data model written in [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51)
1. Deploy a Prisma service with that data model
1. If your database previously contained some data, [import the data](!alias-caith9teiy) into your Prisma service

If you want to migrate from an existing data source, you can check out our plans to support automatic data imports [here](https://github.com/graphcool/graphcool/issues/1410).

### What databases does Prisma support?

**MySQL** is the very first database supported by Prisma. You can find more information about which databases will be supported [here](https://github.com/graphcool/prisma/tree/master/cli/#supported-databases).

If you have any preferences on which database you'd like to see implemented, you can create a new feature request or give a +1 for an existing one.

### Does Prisma Cloud come with built-in Database?

By its very nature, Prisma Cloud does **not** come with a built-in Database.

We offer a **fully managed solution** where we optionally offer you a database. https://www.prisma.io/cloud/ (search for “serverless hosting”)
There are **demo servers** available in Prisma Cloud where you can create demo services, which will use the DB assigned to those servers (owned by Prisma for development purposes).

### What are the benefits of GraphQL?

GraphQL comes with a plethora of benefits for application development:

- GraphQL allows client applications to **request exactly the data they need from an API** (which reduces network traffic and increases performance).
- When accessing a GraphQL API from the frontend, you can use a GraphQL client library (such as Apollo Client or Relay) which **reduces frontend boilerplate** and provides **out-of-the-box support for features like caching, optimistic UI updates, realtime functionality, offline support and more**.
- GraphQL APIs are based on a strongly typed [schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e) which effectively provides a way for you to have a **strongly typed API layer**. This means developers can be confident about the operations an API allows and the shape of the responsed to be returned by the server.
- GraphQL has an **amazing ecosystem of tooling** which greatly improve workflows and overall developer experience. For example, [GraphQL Playground](!alias-chaha125ho) which provides an interactive IDE for sending queries and mutations to an API (which can even be used by non-technical audiences). Another example are [GraphQL bindings](!alias-quaidah9ph) which enable to compose existing GraphQL APIs like LEGO bricks.
- The GraphQL ecosystem is made the **fantastic GraphQL community** who is putting in a lot of thought as well as hard work for how GraphQL can be evolved in the future and what other tools developers will benefit from.

### How do backups work?

Because Prisma is only a layer _on top of your database_, you still have full control over the database itself. This means you have the full power and flexibility regarding your own backup strategy.

Prisma also offers a [data export](!alias-pa0aip3loh) feature which you can use to create continuous backups for your data from the API layer.
