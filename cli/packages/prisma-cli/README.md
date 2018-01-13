<p align="center"><a href="https://www.graph.cool"><img src="https://imgur.com/he8RLRs.png"></a></p>

[Website](https://www.graph.cool/) • [Docs](https://graph.cool/docs/) • [Blog](https://blogs.graph.cool/) • [Forum](https://www.graph.cool/forum) • [Slack](https://slack.graph.cool/) • [Twitter](https://twitter.com/graphcool)

[![CircleCI](https://circleci.com/gh/graphcool/graphcool.svg?style=shield)](https://circleci.com/gh/graphcool/graphcool) [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://img.shields.io/badge/npm%20package-next-brightgreen.svg)](https://badge.fury.io/js/graphcool)

**The Graphcool backend development framework** is designed to help you develop and deploy production-ready GraphQL microservices. With Graphcool you can design your data model and have a production ready [GraphQL](https://www.howtographql.com/) API online in minutes.

The framework integrates with cloud-native serverless functions and is compatible with existing libraries and tools like [GraphQL.js](https://github.com/graphql/graphql-js) and [Apollo Server](https://github.com/apollographql/apollo-server). Graphcool comes with a CLI and a Docker-based runtime which can be deployed to any server or cloud.

<!-- 
Add note that Graphcool can still be used with other langs via webhooks??
-->

The framework provides powerful abstractions and building blocks to develop flexible, scalable GraphQL backends:
    
1. **GraphQL database** to easily evolve your data schema & migrate your database
2. **Flexible auth** using the JWT-based authentication & permission system
3. **Realtime API** using GraphQL Subscriptions
4. **Highly scalable architecture** enabling asynchronous, event-driven flows using serverless functions
5. **Works with all frontend frameworks** like React, Vue.js, Angular ([Quickstart Examples](https://graph.cool/docs/quickstart/))

## Contents

<img align="right" width="400" src="https://imgur.com/EsopgE3.gif" />

* [Quickstart](#quickstart)
* [Features](#features)
* [Examples](#examples)
* [Architecture](#architecture)
* [Deployment](#deployment)
* [FAQ](#faq)
* [Roadmap](#roadmap)
* [Community](#community)
* [Contributing](#contributing)

## Quickstart

> **Note:** This is a preview version of the Graphcool Framework (latest `0.8`). More information in the [forum](https://www.graph.cool/forum/t/feedback-new-cli-beta/949).

[Watch this 5 min tutorial](https://www.youtube.com/watch?v=xmri5pNR9-Y) or follow the steps below to get started with the Graphcool framework:

1. **Install the CLI via NPM:**

  ```sh
  npm install -g graphcool
  ```

2. **Create a new service:**

  The following command creates all files you need for a new [service](https://graph.cool/docs/reference/service-definition/overview-opheidaix3).

  ```sh
  graphcool init
  ```

3. **Define your data model:**

  Edit `types.graphql` to define your data model using the [GraphQL SDL notation](https://graph.cool/docs/reference/database/data-modelling-eiroozae8u). `@model` types map to the database.
  
  ```graphql
  type User @model {
    id: ID! @isUnique
    name: String!
    dateOfBirth: DateTime
  
    # You can declare relations between models like this
    posts: [Post!]! @relation(name: "UserPosts")
  }
  
  
  type Post @model {
    id: ID! @isUnique
    title: String!
  
    # Relations always have two fields
    author: User! @relation(name: "UserPosts")
  }

  ```

4. **Define permissions and functions:**

  [`graphcool.yml`](https://graph.cool/docs/reference/service-definition/graphcool.yml-foatho8aip) is the root definition of a service where `types`, `permissions` and `functions` are referenced.

  ```yml
  # Define your data model here
  types: types.graphql

  # Configure the permissions for your data model
  permissions:
  - operation: "*"

  # tokens granting root level access to your API
  rootTokens: []

  # You can implement your business logic using functions
  functions:
    hello:
      handler:
        code: src/hello.js
      type: resolver
      schema: src/hello.graphql
  ```

<!--
5. **Implement API Gateway layer (optional):**
-->

5. **Deploy your service:**

  To deploy your service simply run the following command and select either a hosted BaaS [cluster](https://graph.cool/docs/reference/graphcool-cli/.graphcoolrc-zoug8seen4) or setup a local Docker-based development environment:

  ```sh
  graphcool deploy
  ```

6. **Connect to your GraphQL endpoint:**

  Use the endpoint from the previous step in your frontend (or backend) applications to connect to your GraphQL API.

## Features

#### Graphcool enables rapid development

* Extensible & incrementally adoptable
* No vendor lock-in through open standards
* Rapid development using powerful abstractions and building blocks

#### Includes everything needed for a GraphQL backend

* GraphQL Database with automatic migrations
* JWT-based authentication & flexible permission system
* Realtime GraphQL Subscription API
* GraphQL specfication compliant
* Compatible with existing libraries and tools (such as GraphQL.js & Apollo)

#### Scalable serverless architecture designed for the cloud

* Docker-based cluster runtime deployable to AWS, Google Cloud, Azure or any other cloud
* Enables asynchronous, event-driven workflows using serverless functions
* Http based database connections optimised for serverless functions

#### Integrated developer experience from zero to production

* Rapid local development workflow – also works offline
* Supports multiple languages including Node.js and Typescript
* [GraphQL Playground](https://github.com/graphcool/graphql-playground): Interactive GraphQL IDE
* Supports complex continuous integration/deployment workflows

## Examples

### Service examples

* [auth](auth): Email/password-based authentication
* [crud-api](crud-api): Simple CRUD-style GraphQL API
* [env-variables-in-functions](env-variables-in-functions): Function accessing environment variables
* [full-example](full-example): Full example (webshop) demoing most available features
* [typescript-gateway-custom-schema](typescript-gateway-custom-schema): Define a custom schema using an API gateway
* [graphcool-lib](graphcool-lib): Use `graphcool-lib` in functions to send queries and mutations to your service
* [permissions](permissions): Configure permission rules
* [rest-wrapper](rest-wrapper): Extend GraphQL API by wrapping existing REST endpoint
* [subscriptions](subscriptions): Use subscription functions to react to asynchronous events
* [yaml-variables](yaml-variables): Use variables in your `graphcool.yml`


### Frontend examples

* [react-graphql](https://github.com/graphcool-examples/react-graphql): React code examples with GraphQL, Apollo, Relay, Auth0 & more
* [react-native-graphql](https://github.com/graphcool-examples/react-native-graphql): React Native code examples with GraphQL, Apollo, Relay, Auth0 & more
* [vue-graphql](https://github.com/graphcool-examples/vue-graphql): Vue.js code examples with GraphQL, Apollo & more
* [angular-graphql](https://github.com/graphcool-examples/angular-graphql): Angular code examples with GraphQL, Apollo & more
* [ios-graphql](https://github.com/graphcool-examples/ios-graphql): React code examples with GraphQL, Apollo, Relay, Auth0 & more

## Architecture

Graphcool is a new kind of framework that introduces clear boundaries between your business logic and stateful components. This separation allows the framework to take advantage of modern cloud infrastructure to scale the stateful components without restricting your choice of programming language and development workflow.

![](https://imgur.com/zaaFVnF.png)

## GraphQL Database

The most important component in the Graphcool Framework is the GraphQL Database:

 - Query, mutate & stream data via GraphQL CRUD API
 - Define and evolve your data model using GraphQL SDL

 If you have used the Graphcool Backend as a Service before, you are already familiar with the benefits of the GraphQL Database.
 
The CRUD API comes out of the box with advanced features such as pagination, expressive filters and nested mutations. These features are implemented within an effecient data-loader engine, to ensure the best possible performance.
 

## Deployment

Graphcool services can be deployed with [Docker](https://docker.com/) or the [Graphcool Cloud](http://graph.cool/cloud).

### Docker

You can deploy a Graphcool service to a local environment using Docker. To run a graphcool service locally, use the `graphcool local` sub commands.

This is what a typical workflow looks like:

```sh
graphcool init     # bootstrap new Graphcool service
graphcool local up # start local cluster
graphcool deploy   # deploy to local cluster
```

### Graphcool Cloud (Backend-as-a-Service)

Services can also be deployed to _shared_ clusters in the Graphcool Cloud. When deploying to a shared cluster, there is a **free developer plan** as well as a convienent and efficient **pay-as-you-go pricing** model for production applications. 

The Graphcool Cloud currently supports three [regions](https://blog.graph.cool/new-regions-and-improved-performance-7bbc0a35c880):

- `eu-west-1` (EU, Ireland)
- `asia-northeast-1` (Asia Pacific, Tokyo)
- `us-west-1` (US, Oregon)


<!--

#### Consumer-driven API contracts

- https://martinfowler.com/articles/consumerDrivenContracts.html

### Open source & Community

The Graphcool Framework is completely open-source and based on open standards. We highly value 

- Open Source & Based on open standards

-->

<!--

#### Powerful core

At the core of every Graphcool service is the auto-generated CRUD API that offers a convenient GraphQL-based abstraction over your database.

#### Flexible shell

Business logic, authentication and permissions are implemented with serverless functions that seamlessly integrate with the CRUD API. All communication between different parts of the framework is typesafe thanks to the GraphQL schema.

The API gateway is another tool that provides the power and flexibility needed to build modern applications.

-->

## FAQ

### Wait a minute – isn't Graphcool a Backend-as-a-Service?

While Graphcool started out as a Backend-as-a-Service (like Firebase or Parse), [we're currently in the process](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333) of turning Graphcool into a backend development framework. You can still deploy your Graphcool services to the [Graphcool Cloud](https://graph.cool/cloud), and additionally you can run Graphcool locally or deploy to your own infrastructure.

### Why is Graphcool Core written in Scala?

At the core of the Graphcool Framework is the GraphQL Database, an extremely complex piece of software. We developed the initial prototype with Node but soon realized that it wasn't the right choice for the complexity Graphcool needed to deal with.

We found that to be able to develop safely while iterating quickly, we needed a powerful typesystem. Scala's support for functional programming techniques coupled with the strong performance of the JVM made it the obvious choice for Graphcool. 

Another important consideration is that the most mature GraphQL implementation - [Sangria](https://github.com/sangria-graphql) - is written in Scala. 


### Is the API Gateway layer needed?

The API gateway is an _optional_ layer for your API, adding it to your service is not required. It is however an extremely powerful tool suited for many real-world use cases, for example:

- Tailor your GraphQL schema and expose custom operations (based on the underlying CRUD API)
- Intercept HTTP requests before they reach the CRUD API; adjust the HTTP response before it's returned
- Implement persisted queries
- Integrate existing systems into your service's GraphQL API
- File management

Also realize that when you're not using an API gateway, _your service endpoint allows everyone to view all the operations of your CRUD API_. The entire data model can be deduced from the exposed CRUD operations.

## Roadmap


Help us shape the future of the Graphcool Framework by :thumbsup: [existing Feature Requests](https://github.com/graphcool/framework/issues?q=is%3Aopen+is%3Aissue+label%3Akind%2Ffeature) or [creating new ones](https://github.com/graphcool/framework/issues/new)

We are in the process of setting up a formal roadmap. Check back here in the coming weeks
to see the new features we are planning!

## Community

Graphcool has a community of thousands of amazing developers and contributors. Welcome, please join us! 👋

- [Forum](https://www.graph.cool/forum)
- [Slack](https://slack.graph.cool/)
- [Stackoverflow](https://stackoverflow.com/questions/tagged/graphcool)
- [Twitter](https://twitter.com/graphcool)
- [Facebook](https://www.facebook.com/GraphcoolHQ)
- [Meetup](https://www.meetup.com/graphql-berlin)
- [Email](hello@graph.cool)


## Contributing

Your feedback is **very helpful**, please share your opinion and thoughts!

### +1 an issue

If an existing feature request or bug report is very important for you, please go ahead and :+1: it or leave a comment. We're always open to reprioritize our roadmap to make sure you're having the best possible DX.

### Requesting a new feature

We love your ideas for new features. If you're missing a certain feature, please feel free to [request a new feature here](https://github.com/graphcool/framework/issues/new). (Please make sure to check first if somebody else already requested it.)
