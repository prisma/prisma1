---
alias: viuf8uus7o
description: An overview of how to configure and manage the database model with Graphcool.
---

# Overview

Graphcool offers an abstraction over your database. This means you don't need to use SQL any more. Instead, you're using the GraphQL [Schema Definition Language](https://www.graph.cool/docs/faq/graphql-sdl-schema-definition-language-kr84dktnp0/) (SDL) for your [type definitions](!alias-eiroozae8u). Graphcool then generates and manages the underlying database schema for you.

[Migrations](!alis-paesahku9t) can be performed by updating your type definitions and running the [`graphcool deploy`](!alias-aiteerae6l#graphcool-deploy) command in the CLI. In cases where additional information for a migration is required (such as default values for a non-nullable type that was added to the database schema), you need to provide an additional file with the required information.


<InfoBox type=info>

Every Graphcool service is backed by a SQL database. When deploying to a _shared_ [cluster](!alias-zoug8seen4#managing-clusters-in-the-global-graphcoolrc), the service comes with an instance of [AWS Aurora](https://aws.amazon.com/rds/aurora/). For deployments with Docker, [MySQL](https://hub.docker.com/r/mysql/mysql-server/) is used.

</InfoBox>
