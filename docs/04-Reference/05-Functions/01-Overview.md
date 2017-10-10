---
alias: aiw4aimie9
description: An overview of how to integrate functions with Graphcool. 
---

# Overview

Graphcool lets you seamlessly integrate serverless functions inside your project to react to events, execute business logic and hook into the GraphQL engine.

There generally are three kinds of functions that you can implement

- [Hooks](!alias-pa6guruhaf) allow you to synchronously invoke a function right before or after a database operation (commonly used for data transformation and validation).
- [Subscriptions](!alias-bboghez0go) allow you to asynchronously invoke a function when an specific event occurs in the [GraphQL engine](!alias-thei2kephu#graphql-engine).
- [Resolvers](!alias-su6wu3yoo2) allow you to extend your GraphQL schema with more functionality that goes beyond the auto-generated CRUD capabilities. Resolvers are also commonly used to implement authentication mechanisms.

## Adding a function to your Graphcool project

To add a function to your Graphcool project, you need to add it to the [project definition file](!alias-opheidaix3#project-definition) under the `functions` section and apply the changes using `graphcool deploy`.

When being added, a function needs the following information:

- `handler`: Specifies _how_ the function should be invoked (either as a webhook or a managed function that will be deployed by Graphcool directly)
- `type`: Specifies which of the four available function types (`operationBefore`, `operationAfter`, `subscription`, `resolver`) this function is.

Only if `type` is `subscription`:

- `query` : Points to a file containing the subscription query (which determines the input type of the function).

Only if `type` is `operationBefore` or `operationAfter`:

- `operation `: Specifies for which operation on which model type this function should be invoked, e.g. `User.update` or `Article.delete`.

Only if `subscription` is `operationBefore`:

- `schema`: Defines the necessary extensions on the `Query` or `Mutation` type (and potentially additional types that represent the input or return types of the new field).

## Managed functions vs Webhooks

The `handler` of a function can either refer to a _managed function_ or a _webhook_.

### Managed functions

With a managed function, you don't have to worry about explicit deployment of your function since it will get deployed along with your Graphcool project when you're running `graphcool deploy`.

The code for a managed function needs to be located in your project directory, typically in a folder called `code`.

Here is an example of a managed function in the project configuration file:

```yaml 
functions:
  hello:
    handler:
      code:
        src: ./code/hello.js
```

For conciseness, you can also refer to the file as the value for `code` or directly for `handler`. The following two notations are equivalent to the previous one:

```yaml 
functions:
  hello:
    handler:
      code: ./code/hello.js
```

```yaml 
functions:
  hello:
    handler: ./code/hello.js
```


### Webhooks

A webhook needs a `url` that specifies where it can be invoked. You can further specify the HTTP `headers` that should be attached to the request that's sent to the webhook.

Here is an example of a function that's specified as a webhook:

```yaml
functions:
  hello:
    handler:
      webhook:
        url: http://example.org/hello
        headers:
          Content-Type: application/json
```

If you don't want to specify any headers, you can simply write the configuration like so:

```yaml
functions:
  hello:
    handler:
      webhook: http://example.org/hello
```
