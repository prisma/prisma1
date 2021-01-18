---
alias: nu5oith4da
description: Variables
---

# Using Variables

Variables allow you to dynamically replace configuration values in your service definition file `prisma.yml`.

To use variables inside `prisma.yml`, you need to reference the values enclosed in `${}` brackets. Inside the brackes, you first need to specify the _variable source_ and the _variable name_, separated by a colon.

```yml
# prisma.yml file
yamlKeyXYZ: ${src:myVariable} # see list of current variable sources below
# this is an example of providing a default value as the second parameter
otherYamlKey: ${src:myVariable, "someDefaultValue"}
```

> **Note**: The quotes around the default value are required.

A _variable source_ can be either of the following three options:

- An _environment variable_
- A _self-reference_ to another value inside the same service
- An _option from the command line_

> Note that you can only use variables in property **values** - not in property keys.

### Environment variables

You can reference environment variables inside the service definition file.

When using an environment variable, the value that you put into the bracket is composed of:

- the _prefix_ `env:`
- the _name_ of the environment variable

In the following example, environment variables are used to declare the stage, the cluster and the secret the `example` service:

```yml
service: example
stage: ${env:PRISMA_STAGE}
cluster: ${env:PRISMA_CLUSTER}
secret: ${env:PRISMA_SECRET}
datamodel: database/datamodel.graphql
```

Note that the CLI will load environment variables from 3 different locations and in the following order:

1. The local environment
1. A file specified with the `--dotenv` parameter
1. if the `--dotenv`argument was omitted, a file called `.env` in the same directory

### Self-references

You can recursively reference other property values inside the same `prisma.yml` file.

When using a recursive self-reference as a variable, the value that you put into the bracket is composed of:

- the _prefix_ `self:`
- (optional) the _path_ to the referenced property

> If no path is specified, the value of the variable will be the full YAML file.

In the following example, the `createCRMEntry` function uses the same subscription query as the `sendWelcomeEmail` function:

```yml
subscriptions:
  sendWelcomeEmail:
    query: database/subscriptions/createUserSubscription.graphql
    webhook:
      url: ${self:custom.severlessEndpoint}/sendWelcomeEmail
      headers: ${self:custom.headers}
  createCRMEntry:
    query: ${self:functions.subscriptions.sendWelcomeEmail.query}
    webhook:
      url: ${self:custom.severlessEndpoint}/createCRMEntry
      headers: ${self:custom.headers}

custom:
  serverlessEndpoint: 'https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev'
  headers:
    Authorization: Bearer wohngaeveishuomeiphohph1ls
```

### CLI options

You can reference CLI options that are passed when invoking a `prisma` command.

When referencing a CLI option, the value that you put into the bracket is composed of:

- the _prefix_ `opt:`
- the _name_ of the CLI option

As an example, consider this `prisma.yml` file:

```yml
service: example
stage: ${opt:stage}
secret: secret123
datamodel: datamodel.graphql
```

Now, you can pass a `--stage` option when running `prisma deploy`. The CLI will pick the value provided value up and set it as the `stage` in `prisma.yml`.

```sh
prisma deploy --stage dev
```
