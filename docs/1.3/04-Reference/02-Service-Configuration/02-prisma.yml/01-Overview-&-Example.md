---
alias: foatho8aip
description: Overview
---

# Overview

Every Prisma service consists of several components that developers can provide, such as the service name, the data model for the service, information about deployment and authentication or the configuration of event subscriptions functions.

All of these components are set up in the configuration file for a service: `prisma.yml`.

## Example

Here is a simple example of a service definition file:

```yml
# REQUIRED
# `my-demo-app` is the name of this Prisma service.
service: my-demo-app

# REQUIRED
# This service is based on the type definitions in the two files
# `database/types.graphql` and `database/enums.graphql`
datamodel:
  - database/types.graphql
  - database/enums.graphql

# OPTIONAL
# The service will be deployed to the `local` cluster.
# Note that if you leave out this option, you will be
# asked which cluster to deploy to, and your decision
# will be persisted here.
cluster: local

# REQUIRED
# This service will be deployed to the `dev` stage.
stage: dev

# OPTIONAL (default: false)
# Whether authentication is required for this service
# is based on the value of the `PRISMA_DISABLE_AUTH`
# environment variable.
disableAuth: ${env:PRISMA_DISABLE_AUTH}

# OPTIONAL
# If your Prisma service requires authentication, this is the secret for creating JWT tokens.
secret: 

# OPTIONAL
# Path where the full GraphQL schema will be written to
# after deploying. Note that it is generated based on your
# data model.
schema: schemas/prisma.graphql

# OPTIONAL
# This service has one event subscription configured. The corresponding
# subscription query is located in `database/subscriptions/welcomeEmail.graphql`.
# When the subscription fires, the specified `webhook` is invoked via HTTP.
subscriptions:
  sendWelcomeEmail:
    query: database/subscriptions/sendWelcomeEmail.graphql
    webhook:
      url: https://${self:custom.serverlessEndpoint}/sendWelcomeEmail
      headers:
        Authorization: ${env:MY_ENDPOINT_SECRET}

# OPTIONAL
# Points to a `.graphql` file containing GraphQL operations that will be
# executed when initially deploying a service.
seed:
  import: database/seed.graphql

# OPTIONAL
# This service only defines one custom variable that's referenced in
# the `webhook` of the `subscription`
custom:
  serverlessEndpoint: https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev
```

This service definition expects the following file structure:

```
.
├── prisma.yml
├── database
│   ├── subscriptions
│   │   └── welcomeEmail.graphql
│   ├── types.graphql
│   └── enums.graphql
└── schemas
    └── prisma.graphql
```

## Tooling integrations

### Get autocompletion and validation while configuring `prisma.yml`

If you wish to have autocompletion while configuring your `prisma.yml` configuration file, as well as static errors checking before deploying your service, a [JSON Schema](https://github.com/graphcool/prisma-json-schema) is available to provide this kind of experience.
**For now though, it is only available for [VSCode](https://code.visualstudio.com/).**


**Step 1.**

Download and install [vs-code-yaml by redhat](https://github.com/redhat-developer/vscode-yaml) plugin.

**Step 2.**

Add the following to the [user and workspace settings](https://code.visualstudio.com/docs/getstarted/settings#_creating-user-and-workspace-settings):

```
"yaml.schemas": {
  "http://json.schemastore.org/prisma": "prisma.yml"
}
```
**Step 3.**

Trigger the intellisense using your usual hotkey (*Ctrl + Space* by default) on your `prisma.yml` file. It should now display all the available fields, along with their descriptions. If any errors are made, VSCode will instantly catch them.