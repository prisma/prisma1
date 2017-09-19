---
alias: viuf8uus7o
description: An overview of how to configure and manage the database model with Graphcool.
---

# Overview

With Graphcool, you don't manage your database layer _directly_. Instead, you're defining your data model using the GraphQL [Schema Definition Language](https://www.graph.cool/docs/faq/graphql-sdl-schema-definition-language-kr84dktnp0/) (SDL) to define all your model types. Graphcool then generates and manages the underlying database schema for you.

Migrations can be performed by updating your type definitions and running the `graphcool deploy` command in the CLI. In cases where additional information for a migration is required (such as default values for a non-nullable type that was added to the database schema), you need to provide an additional file with the required information.


<InfoBox type=info>

Graphcool uses a SQL database under the hood. In the hosted version of Graphcool, every project comes with an instance of [AWS Aurora](https://aws.amazon.com/rds/aurora/). In the self-hosted version, it's possible to plug in other databases like MySQL.

</InfoBox>