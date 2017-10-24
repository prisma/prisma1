---
alias: ucoohic9zu
description: Information about implementing an API Gateway for Graphcool Services.
---

# Overview

Every Graphcool service comes with an auto-generated [CRUD API](!alias-abogasd0go) for your [data model](!alias-eiroozae8u). The API gateway can be used to _customize_ the API that's exposed to your client applications. This customization can be implemented using the [schema stitching](http://dev.apollodata.com/tools/graphql-tools/schema-stitching.html) API from [`graphql-tools`](https://github.com/apollographql/graphql-tools).

The API gateway is a dedicated HTTP endpoint acting as a proxy in front of the CRUD API. It can be implemented as a regular web server, e.g. using node.js and express, or even as a serverless function that can be deployed with AWS Lambda or another Functions-as-a-Service provider.

[Here](https://github.com/graphcool/graphcool/tree/master/examples/typescript-gateway-custom-schema) is an example that demonstrates how to implement a simple API gateway on top of a Graphcool service, using node.js and express.


## Use cases

The API gateway is an extremely powerful concept that has a number of very different use cases:

- **Implementing custom queries and mutations** with your own [GraphQL resolvers](http://graphql.org/learn/execution/#root-fields-resolvers), it thus has similar use cases as [resolver functions](!alias-su6wu3yoo2):
  - Wrapping an existing API
  - Providing "shortcuts" to the service's CRUD API
  - Authentication workflows
-  **Validating HTTP requests** which would allow to e.g. block requests coming from certain IP addresses
- **Transforming and validating** incoming data
