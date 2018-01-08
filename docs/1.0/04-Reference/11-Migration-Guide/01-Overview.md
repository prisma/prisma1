---
alias: koo7shaino
description: Overview
---

# Overview

## Graphcool evolution

Graphcool has evolved quite a bit since it first went live. Here is an overview of its three major phases:

* **Graphcool Backend-as-a-Service**: The initial product of Graphcool was a "BaaS for GraphQL". In that version, an entire backend is configured through the web-based GUI of the Graphcool Console, including the integration of serverless functions and features like authentication and 3rd party integrations.
* **Graphcool Framework**: The Graphcool Framework is the open source version of the BaaS. Everything that was previously done through the Graphcool Console, is now possible with a local developer workflow using the Graphcool CLI. This enables private hosting of Graphcool, better testing, debugging and CI workflows. A major change compared to the BaaS version is that authentication is now done with resolver functions rather than being configured through the UI. Permission queries are not configured through the UI any more but are also written in source files.
* **Graphcool 1.0 - "GraphQL Database"**: Graphcool 1.0 focusses on the core: Its GraphQL API. Note that the GraphQL API introduces a few breaking changes in 1.0. On an architectural level, the biggest difference is that Graphcool now requires a web server (such as Express.js) for which it then provides the persistence layer. This web server implements all functionality that was previously performed by serverless functions (except for subscriptions, they're still available in 1.0). In JavaScript, you can use `graphql-yoga` as your Express.js-based GraphQL server.

<InfoBox type=info>

To learn more about the migration from the BaaS to the Graphcool Framework, you can check out the previous [upgrade guide](!alias-aemieb1aev).

</InfoBox>

## Migrating to 1.0

We recommend that you are migrating your project in a development environment before putting it into production. This ensures a smooth migration process. You can use the [import](!alias-ol2eoh8xie) and [export](!alias-pa0aip3loh) functionality for Graphcool services to migrate data between the different projects.

### New architecture: Your GraphQL server uses Graphcool as a data store

The core idea of Graphcool 1.0 is to use an additional GraphQL server that uses Graphcool as a data store. The easiest way to build this GraphQL server is using `graphql-yoga`. When implementing the resolvers for this GraphQL server, you can use schema bindings and the `graphcool-binding` package to conventiently forward incoming queries to Graphcool.

### The GraphQL server takes over implementation of business logic and auth workflows

Another important change in Graphcool 1.0 is that it is not possible to integrate any serverless functions directly with your Graphcool service any more. Instead, functionality you previously would have implemented with [resolvers](!alias-su6wu3yoo2) or [hooks](!alias-pa6guruhaf) now goes into your GraphQL server directly. This gives a lot more flexibility and caters a higher number of and overall more advanced use cases.

This includes functionality for authentication and permissions which were previously handled by the Graphcool Framework directly. Another piece of logic many applications are dealing with, file handling, is moved into the GraphQL server as well.

### Deploying your GraphQL server

As Graphcool 1.0 only represents the "database" component in your server architecture, you need to host the GraphQL server yourself.

A quick and easy way to deploy your GraphQL server is using [Now](https://zeit.co/now) or using a serverless function and the [Serverless Framework](https://serverless.com). When doing so, you should ensure that your server is deployed to the same [AWS region](http://docs.aws.amazon.com/general/latest/gr/rande.html) as your Graphcool service to not introduce additional latency and ensure optimal performance.

