---
alias: nu5oith4da
description: Variables
---

# Using Variables

Variables allow you to dynamically replace configuration values in your service definition file `graphcool.yml`.

> Variables are especially useful when providing _secrets_ for your service and when you have a multi-staging developer workflow.

To use variables inside `graphcool.yml`, you need to reference the values enclosed in `${}` brackets:

```yml
# graphcool.yml file
yamlKeyXYZ: ${myVariable} # see list of current variable sources below
# this is an example of providing a default value as the second parameter
otherYamlKey: ${myVariable, defaultValue}
```

A _variable source_ can be either of the following two options:

- A _recursive self-reference_ to another value inside the same service
- An _environment variable_
- An _option from the command line_

> Note that you can only use variables in property **values** - not in property keys. So you can't use variables to generate dynamic logical IDs in the custom resources section for example.

### Recursive self-reference

You can recursively reference other property values that live inside the same `graphcool.yml` file.

When using a recursive self-reference as a variable, the value that you put into the bracket is composed of:

- the _prefix_ `self:`
- (optional) the _path_ to the referenced property

> If no path is specified, the value of the variable will be the full YAML file.

In the following example, the `createCRMEntry` function uses the same subscription query as the `sendWelcomeEmail` function:

```yml
functions:
  sendWelcomeEmail:
    handler:
      code:
        src: ./src/sendWelcomeEmail.js
    type: subscription
    query: ./src/newUserSubscription.graphql
  createCRMEntry:
    handler:
      code:
        src: ./src/createCRMEntry.js
    type: subscription
    query: ${self:functions.sendWelcomeEmail.handler.query}
```


### Environment variable

You can reference [environment variables](https://en.wikipedia.org/wiki/Environment_variable) inside the service definition file.

When using an environment variable, the value that you put into the bracket is composed of:

- the _prefix_ `env:`
- the _name_ of the environment variable

In the following example, an environment variable is referenced to specify the URL and the authentication token for a webhook:

```yml
functions:
  initiatePayment:
    handler:
      webhook:
        url: ${env:PAYMENT_URL}
        headers:
            Content-Type: application/json
            Authorization: Bearer ${env:AUTH_TOKEN}
    type: subscription
```


### CLI options

You can reference CLI options that you passed when invoking a [`graphcool` command](!alias-aiteerae6l) inside your `graphcool.yml` service definition file.

When referencing a CLI option, the value that you put into the bracket is composed of:

- the _prefix_ `opt:`
- the _name_ of the CLI option

> Note: It is valid to use the _empty string_ as the _name_ of the CLI option. This looks like `${opt:}` and the result of declaring this in your `graphcool.yml` is to embed the complete options object (i.e. all the command line options from your `graphcool` command).

For the following example, assume the following `graphcool` command was just ran in the terminal:

```sh
graphcool deploy --stage prod
```

To reference the value of the `stage` option inside `graphcool.yml`, you can now specify the following:

```
webhook:
  url: http://myapi.${opt:stage}.com/example
```

When the command is invoked, the value of `webhook.url` will be deployed as `http://myapi.prod.com/example`.
