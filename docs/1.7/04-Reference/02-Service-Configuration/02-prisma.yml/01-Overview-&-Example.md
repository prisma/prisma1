---
alias: foatho8aip
description: Overview
---

# Overview

Every Prisma service consists of several components that developers can provide, such as the API endpoint, the data model for the service, information about deployment and authentication or the configuration of subscription webhooks.

All of these components are set up in the configuration file for a service: `prisma.yml`.

## Example

Here is a simple example of a service definition file:

```yml
# REQUIRED
# This service is based on the type definitions in the two files
# `database/types.graphql` and `database/enums.graphql`
datamodel:
  - database/types.graphql
  - database/enums.graphql

# OPTIONAL
# The endpoint represents the HTTP endpoint for your Prisma API. It encodes
# several pieces of information:
# * Prisma server (`localhost:4466` in this example)
# * Service name (`myservice` in this example)
# * Stage (`dev` in this example)
# NOTE: When service name and stage are set to `default`, they can be omitted.
# Meaning http://myserver.com/default/default can be written as http://myserver.com.
endpoint: http://localhost:4466/myservice/dev

# OPTIONAL
# The secret is used to create JSON web tokens (JWTs). These tokens need to be
# attached in the `Authorization` header of HTTP requests against the Prisma endpoint.
# WARNING: If the secret is not provided, the Prisma API can be accessed
# without authentication!
secret: mysecret123

# OPTIONAL
# A "post-deployment" hook that first downloads the GraphQL schema from an endpoint configured
# in `.graphqlconfig` and then invokes a code generation process.
hooks:
  post-deploy:
    - graphql get-schema --project db
    - graphql prepare

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