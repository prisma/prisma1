---
alias: su6wu3yoo2
description: Schema extensions allow you to add custom GraphQL queries and mutations to your GraphQL API that are resolved by a Graphcool Function.
---


# Schema Extensions

Schema Extensions (SE) allow you to add custom GraphQL queries and mutations to your GraphQL API that are resolved by a Graphcool Function.

Functions for schema extensions are called **on demand**, when the according query or mutation is executed.

## Use Cases

Schema Extensions are one of the most powerful features offered with Graphcool. As such, they cover a tool which is applicable for a wide range of situations. Here's a list of some of the most common use cases:

* You can build **custom authentication** workflows, like an integration with services like Auth0, Firebase Auth, AWS Cognito and AccountKit, Social Login providers like Facebook, Twitter and GitHub or an email-password based login.
* You can **wrap an external API**, for example a weather API, geolocation API or any other REST API.
* You can **wrap your Graphcool API**, for example to introduce a specific mutation or return data in a different shape.

The [Graphcool Functions collection](https://github.com/graphcool-examples/functions/) on GitHub contains a lot of examples that can inspire you for more use cases, see also [custom mutations](!alias-thiele0aop) and [custom queries](!alias-nae4oth9ka).

## Trigger and Input Data

The trigger and input data for the Schema Extension function is defined by extending the `Mutation` or `Query` type with a new field that returns a custom payload.

## Extending the Schema

Each schema extension can only add a single query field or a single mutation field to the GraphQL API. This field can receive several scalar input arguments and has to return a payload that is also specified in the same SDL document.

See also examples for [custom mutations](!alias-thiele0aop) and [custom queries](!alias-nae4oth9ka).

[Error handling](!alias-quawa7aed0) works similarly to other Graphcool Functions, if an object containing the `error` key is returned.

## Current Limitations

* Input and output fields can [only be of scalar types](https://github.com/graphcool/feature-requests/issues/318) at the moment.
* [Only one query or mutation field](https://github.com/graphcool/feature-requests/issues/326) can be added per schema extension.
