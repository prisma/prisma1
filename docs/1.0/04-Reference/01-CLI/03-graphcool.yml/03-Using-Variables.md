---
alias: nu5oith4da
description: Variables
---

# Using Variables

Variables allow you to dynamically replace configuration values in your service definition file `graphcool.yml`.

To use variables inside `graphcool.yml`, you need to reference the values enclosed in `${}` brackets. Inside the brackes, you first need to specify the _variable source_ and the _variable name_, separated by a colon.

```yml
# graphcool.yml file
yamlKeyXYZ: ${src:myVariable} # see list of current variable sources below
# this is an example of providing a default value as the second parameter
otherYamlKey: ${src:myVariable, defaultValue}
```

A _variable source_ can be either of the following three options:

- An _environment variable_
- A _self-reference_ to another value inside the same service
- An _option from the command line_

> Note that you can only use variables in property **values** - not in property keys.

### Environment variables

You can reference [environment variables](https://en.wikipedia.org/wiki/Environment_variable) inside the service definition file.

When using an environment variable, the value that you put into the bracket is composed of:

- the _prefix_ `env:`
- the _name_ of the environment variable

In the following example, environment variables are used to declare the stage, the cluster and the secret the `example` service:

```yml
service: example
stage: ${env:GRAPHCOOL_STAGE}
cluster: ${env:GRAPHCOOL_CLUSTER}
secret: ${env:GRAPHCOOL_SECRET}
datamodel: database/datamodel.graphql
```

Note that the CLI will load environment variables from 3 different locations and in the following order:

1. The local environment
1. A file specified with the `--dotenv` parameter
1. if the `--dotenv`argument was omitted, a file called `.env` in the same directory

### Self-references

You can recursively reference other property values inside the same `graphcool.yml` file.

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
      url: ${self.custom.severlessEndpoint}/sendWelcomeEmail
      headers: ${self.custom.headers}
  createCRMEntry:
    query: ${self:functions.subscriptions.sendWelcomeEmail.query}
    webhook:
      url: ${self.custom.severlessEndpoint}/createCRMEntry
      headers: ${self.custom.headers}

custom:
  serverlessEndpoint: 'https://bcdeaxokbj.execute-api.eu-west-1.amazonaws.com/dev'
  headers:
    Authorization: Bearer wohngaeveishuomeiphohph1ls
```

### CLI options

You can reference CLI options that are passed when invoking a `graphcool` command.

When referencing a CLI option, the value that you put into the bracket is composed of:

- the _prefix_ `opt:`
- the _name_ of the CLI option

<!--

TODO example is outdated (no --stage option anymore)

For the following example, assume the following `graphcool` command was just ran in the terminal:

```sh
graphcool deploy --stage prod
```

To reference the value of the `stage` option inside `graphcool.yml`, you can now specify the following:

```yml
webhook:
  url: http://myapi.${opt:stage}.com/example
```

When the command is invoked, the value of `webhook.url` will be deployed as `http://myapi.prod.com/example`.

-->
