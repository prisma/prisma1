---
alias: su6wu3yoo2
description: Resolvers allow you to extend your GraphQL API with custom queries and mutations that are resolved by a function.
---


# Resolvers

## Overview

Resolvers allow you to extend your GraphQL API with custom queries and mutations that are resolved by a function (the _resolver_).

Resolver functions are called when the corresponding query or mutation is executed. Similiar to hooks, resolvers are executed _synchronously_.

## Use cases

Resolvers are one of the most powerful features offered by Graphcool. As such, they cover a wide range of use cases. Here's a list of some of the most common ones:

* You can build **authentication** workflows, like an integration with services like Auth0, Firebase Auth, AWS Cognito and AccountKit, Social Login providers like Facebook, Twitter and GitHub or an email-password based login. A few of these implementations are already [available as templates](https://github.com/graphcool/templates/tree/master/auth).
* You can **wrap an external API**, for example a weather API, geolocation API or any other REST API. You can check some examples [here](https://github.com/graphcool/templates).
* You can **wrap your Graphcool API**, for example to introduce a specific mutation or return data in a different shape.

The [Graphcool Functions collection](https://github.com/graphcool/templates/) on GitHub contains a lot of examples that can inspire you for more use cases, see also [custom mutations](!alias-nia9nushae#custom-mutations) and [custom queries](!alias-ol0yuoz6go#custom-queries).

## Input type

The input type for a resolver function is defined by the input arguments of the field that's added to the GraphQL schema.

## Adding a Resolver function to the project

When you want to create a resolver function in your Graphcool project, you need to add it to the [project definition file](!alias-opheidaix3#project-definition) under the `functions` section.

### Example

Here is an example of a subscription function:

```yaml
functions:
  loadWeather:
    type: resolver
    schema: weatherQuery.graphql
    handler:
      webhook: http://example.org/load-weather
```

This is what the referred `weatherQuery.graphql` contains:

```graphql
extend type Query {
  weather(unit: TemperatureUnit): Weather
}

type Weather {
  temperature: Int
}

enum TemperatureUnit {
  CELCIUS
  FAHRENHEIT
}
```

`loadWeather ` is invoked _after_ a `User` node was created and is defined as a _webhook_. It receives as input the requested `TemnperatureUnit` (as this is the only argument for the `weather` field on the `Query` type) and returns a new type called `Weather`.

### Properties

Each function that's specified in the project configuration file needs to have the `type` and `handler` properties.

For resolver functions, you additionally need to specify the `schema ` property which points to a file containing your extension of the `Query` or `Mutation` type as well as any additional types that you're defining for this operation.


## Extending the schema

Each resolver can only add a single query field or a single mutation field to the GraphQL API. This field can receive several scalar input arguments and has to return a payload that is also specified in the same SDL document.

See also examples for [custom mutations](!alias-nia9nushae#custom-mutations) and [custom queries](!alias-ol0yuoz6go#custom-queries).

[Error handling](!alias-geihakoh4e) works similarly to other Graphcool Functions, if an object containing the `error` key is returned.

## Current limitations

* Input and output fields can [only be of scalar types](https://github.com/graphcool/graphcool/issues/743) at the moment.
* [Only one query or mutation field](https://github.com/graphcool/graphcool/issues/326) can be added per resolver.
