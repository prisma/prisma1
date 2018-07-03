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

Note that above service definition expects the following file structure:

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

## Using variables

Variables allow you to dynamically replace configuration values in your service definition file.

They are especially useful when providing _secrets_ for your service and when you have a multi-staging developer workflow.

To use variables inside `prisma.yml`, you need to reference the values enclosed in `${}` brackets:

```yml
# graphcool.yml file
yamlKeyXYZ: ${variableSource} # see list of current variable sources below
# this is an example of providing a default value as the second parameter
otherYamlKey: ${variableSource, defaultValue}
```

A _variable source_ can be either of the following two options:

- A _recursive self-reference_ to another value inside the same service
- An _environment variable_
- An _option from the command line_

> Note that you can only use variables in property **values** - not in property keys. So you can't use variables to generate dynamic logical IDs in the custom resources section for example.

### Recursive self-reference

You can recursively reference other property values that live inside the same `prisma.yml` file.

When using a recursive self-reference as a variable, the value that you put into the bracket is composed of:

- the _prefix_ `self:`
- (optional) the _path_ to the referenced property

> If no path is specified, the value of the variable will be the full YAML file.

```yml
subscriptions:
  sendWelcomeEmail:
    query: database/subscriptions/sendWelcomeEmail.graphql
    webhook:
      url: https://${self:custom.serverlessEndpoint}/sendWelcomeEmail

custom:
  serverlessEndpoint: example.org
```

> **Note**: This works for any property inside `prisma.yml`, not just `custom`.

### Environment variable

You can reference [environment variables](https://en.wikipedia.org/wiki/Environment_variable) inside the service definition file.

When using an environment variable, the value that you put into the bracket is composed of:

- the _prefix_ `env:`
- the _name_ of the environment variable

In the following example, an environment variable is referenced to specify the URL and the authentication token for a webhook:

```yml
subscriptions:
  sendWelcomeEmail:
    query: database/subscriptions/sendWelcomeEmail.graphql
    webhook:
      url: https://example.org/sendWelcomeEmail
      headers:
        Authorization: ${env:MY_ENDPOINT_SECRET}
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