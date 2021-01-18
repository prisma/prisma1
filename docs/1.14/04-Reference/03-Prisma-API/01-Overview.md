---
alias: ohm2ouceuj
description: How to use the API
---

# Overview

## What is the Prisma API?

A Prisma service exposes a GraphQL API that is automatically generated based on the deployed [data model](!alias-eiroozae8u). It is also referred to as the **Prisma API**. The Prisma API defines CRUD operations for the types in the data model and allows to get realtime updates when events are happening in the database (e.g. new nodes are _created_ or existing ones _updated_ or _deleted_).

<InfoBox>

The Prisma API is defined by a corresponding [GraphQL schema](https://blog.graph.cool/graphql-server-basics-the-schema-ac5e2950214e), called [**Prisma database schema**](!alias-eiroozae8u#prisma-database-schema-vs-data-model).

</InfoBox>

## Exploring the Prisma API

The [GraphQL Playground](https://github.com/graphcool/graphql-playground) is the best tool to explore the Prisma API and get to know it better. You can use it to run GraphQL mutations, queries and subscriptions.

To open up a Playground for your database service, simply run the `prisma1 playground` command in the working directory of your service or paste your service's HTTP endpoint into the address bar of your browser.
