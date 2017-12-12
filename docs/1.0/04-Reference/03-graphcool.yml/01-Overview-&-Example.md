---
alias: foatho8aip
description: Overview
---

# Overview & Example

## Overview

Every Graphcool service consists of several components that developers can provide, such as the service name, the data model for the service, information about deployment targets and authentication or the configuration of subscription functions.

All of these components are configured your service's root configuration file: `graphcool.yml`.

## Example

Here is a simple example of a service definition file:

```yml
# REQUIRED
# `my-demo-app` is the name of this Graphool service.
service: my-demo-app

# REQUIRED
# This service is based on the type definitions in the two files
# `database/types.graphql` and `database/enums.graphql`
datamodel:
  - database/types.graphql
  - database/enums.graphql

# REQUIRED
# This service has two stages: `dev` and `prod`. The
# default stage is `dev` (meaning it will be used by
# the CLI unless explicitly stated otherwise).
stages:
  default: dev
  dev: local
  prod: london-cluster

# OPTIONAL (default: false)
# Whether authentication is required for this service
# is based on the value of the `GRAPHCOOL_DISABLE_AUTH`
# environment variable.
disableAuth: ${env:GRAPHCOOL_DISABLE_AUTH}

# OPTIONAL
# Path to the full GraphQL schema definition of your service.
# Note that the schema definition is generated based on your
# data model.
schema: schemas/database.graphql

# OPTIONAL
# This service has one event subscription configured. The corresponding
# subscription query is located in `database/subscriptions/welcomeEmail.graphql`.
# When the subscription fires, the specified `webhook` is invoked via HTTP.
subscriptions:
  sendWelcomeEmail:
    query: database/subscriptions/sendWelcomeEmail.graphql
    webhook:
      url: https://${self.custom:serverlessEndpoint}/sendWelcomeEmail
      headers:
        Authorization: ${env:MY_ENDPOINT_SECRET}

# OPTIONAL
# This service only defines one custom variable that's referenced in
# the `webhook` of the `subscription`
custom:
  serverlessEndpoint: https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev
```

This service definition expects the following file structure:

```
.
├── graphcool.yml
├── database
│   ├── subscriptions
│   │   └── welcomeEmail.graphql
│   ├── types.graphql
│   └── enums.graphql
└── schemas
    └── database.graphql
```
