---
alias: su6wu3yoo2
description: Resolvers allow you to extend your GraphQL API with custom queries and mutations that are resolved by a serverless function.
---


# Resolvers

Resolvers allow you to extend your GraphQL API with custom queries and mutations that are resolved by a serverless function (the _resolver_).

Resolver functions are called when the corresponding query or mutation is executed. Similiar to hooks, resolvers are executed _synchronously_.

## Use Cases

Resolvers are one of the most powerful features offered with Graphcool. As such, they cover a tool which is applicable for a wide range of situations. Here's a list of some of the most common use cases:

* You can build **authentication** workflows, like an integration with services like Auth0, Firebase Auth, AWS Cognito and AccountKit, Social Login providers like Facebook, Twitter and GitHub or an email-password based login. A few of these implementations are already [available as modules](https://github.com/graphcool/modules/tree/master/authentication).
* You can **wrap an external API**, for example a weather API, geolocation API or any other REST API.
* You can **wrap your Graphcool API**, for example to introduce a specific mutation or return data in a different shape.

The [Graphcool Functions collection](https://github.com/graphcool-examples/functions/) on GitHub contains a lot of examples that can inspire you for more use cases, see also [custom mutations](!alias-thiele0aop) and [custom queries](!alias-nae4oth9ka).

## Input Type

The input type for a resolver function is defined by the input arguments of the field that's added to the GraphQL schema.

## Adding a Resolver function to the project

When you want to create a resolver function in your Graphcool project, you need to add it to the project configuration file under the `functions` section. 

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


## Extending the Schema

Each schema extension can only add a single query field or a single mutation field to the GraphQL API. This field can receive several scalar input arguments and has to return a payload that is also specified in the same SDL document.

See also examples for [custom mutations](!alias-thiele0aop) and [custom queries](!alias-nae4oth9ka).

[Error handling](!alias-quawa7aed0) works similarly to other Graphcool Functions, if an object containing the `error` key is returned.

## Current Limitations

* Input and output fields can [only be of scalar types](https://github.com/graphcool/feature-requests/issues/318) at the moment.
* [Only one query or mutation field](https://github.com/graphcool/feature-requests/issues/326) can be added per schema extension.
