<p align="center"><a href="https://www.graph.cool"><img src="https://imgur.com/he8RLRs.png"></a></p>

[Website](https://www.graph.cool/) • [Docs](https://docs-next.graph.cool/) • [Blog](https://blogs.graph.cool/) • [Forum](https://www.graph.cool/forum) • [Chat](https://slack.graph.cool/) • [Twitter](https://twitter.com/graphcool)

[![CircleCI](https://circleci.com/gh/graphcool/graphcool.svg?style=shield)](https://circleci.com/gh/graphcool/graphcool) [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://img.shields.io/badge/npm%20package-next-brightgreen.svg)](https://badge.fury.io/js/graphcool)

**Graphcool is a GraphQL backend framework** to develop and deploy production-ready GraphQL microservices.</br>
Think about it like Rails, Django or Meteor but based on [GraphQL](https://www.howtographql.com/) and designed for today's cloud infrastructure.

The framework currently supports Node.js & Typescript and is compatible with existing libraries and tools like [GraphQL.js](https://github.com/graphql/graphql-js) and [Apollo Server](https://github.com/apollographql/apollo-server). Graphcool comes with a CLI and a Docker-based runtime which can be deployed to any server or cloud.

The framework provides powerful abstractions and building blocks to develop flexible, scalable GraphQL backends:

1. **GraphQL-native database mapping** to easily evolve your data schema & migrate your database
2. **Flexible auth workflows** using the JWT-based authentication & permission system
3. **Real-time API** using GraphQL Subscriptions
4. **Highly scalable architecture** enabling asynchronous, event-driven flows using serverless functions
5. **Works with all frontend frameworks** like React, Vue.js, Angular

## Contents

<img align="right" width="400" src="https://imgur.com/EsopgE3.gif" />

* [Quickstart](#quickstart)
* [Features](#features)
* [Examples](#examples)
* [Architecture](#architecture)
* [Deployment](#deployment)
* [Philosophy](#philosophy)
* [FAQ](#faq)
* [Roadmap](#roadmap)
* [Community](#community)
* [Contributing](#contributing)

## Quickstart

> **Note:** This is a preview version of the Graphcool Framework (latest `0.7`). More information in the [forum](https://www.graph.cool/forum/t/feedback-new-cli-beta/949).

[Watch this 2 min tutorial](https://www.youtube.com/watch?v=gg_SJ8a5xpA) or follow the steps below to get started with the Graphcool framework:

1. **Install the CLI via NPM:**

  ```sh
  npm install -g graphcool@next
  ```

2. **Create a new service:**

  The following command creates all files you need for a new [service](https://docs-next.graph.cool/reference/service-definition/overview-opheidaix3).

  ```sh
  graphcool init
  ```

3. **Define your data model:**

  Edit `types.graphql` to define your data model using the [GraphQL SDL notation](https://docs-next.graph.cool/reference/database/data-modelling-eiroozae8u). `@model` types map to the database.
  
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
  
    # Every relation also required a back-relation (to determine 1:1, 1:n or n:m)
    author: User! @relation(name: "UserPosts")
  }

  ```

4. **Define permissions and functions:**

  `graphcool.yml` is the root definition of a service where `types`, `permissions` and `functions` are referenced.

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
  
5. **Implement API Gateway layer (optional):**

6. **Deploy your service:**

  To deploy your service simply run the following command and select either a hosted BaaS cluster or automatically setup a local Docker-based development environment:

  ```sh
  graphcool deploy
  ```

7. **Connect to your GraphQL endpoint:**

  Use the endpoint from the previous step in your frontend (or backend) applications to connect to your GraphQL API.

## Features

#### Graphcool enables rapid development

* Extensible & incrementally adoptable
* No vendor lock-in through open standards
* Rapid development using powerful abstractions and building blocks

#### Includes everything needed for a GraphQL backend

* GraphQL-native database mapping & migrations
* JWT-based authentication & flexible permission system
* Real-time GraphQL Subscription API
* GraphQL specfication compliant
* Compatible with existing libraries and tools (such as GraphQL.js & Apollo)

#### Scalable serverless architecture designed for the cloud

* Docker-based cluster runtime deployable to AWS, Google Cloud, Azure or any other cloud
* Enables asynchronous, event-driven workflows using serverless functions
* Easy function deployment to AWS Lambda and other FaaS providers

#### Integrated developer experience from zero to production

* Rapid local development workflow – also works offline
* Supports multiple languages including Node.js and Typescript
* [GraphQL Playground](https://github.com/graphcool/graphql-playground): Interactive GraphQL IDE
* Supports complex continuous integration/deployment workflows

## Examples

### Service examples

* [auth](examples/auth): Email/password-based authentication
* [crud-api](examples/crud-api): Simple CRUD-style GraphQL API
* [env-variables-in-functions](examples/env-variables-in-functions): Function accessing environment variables
* [full-example](examples/full-example): Full example (webshop) demoing most available features
* [typescript-gateway-custom-schema](examples/typescript-gateway-custom-schema): Define a custom schema using an API gateway
* [permissions](examples/permissions): Configure permission rules
* [rest-wrapper](examples/rest-wrapper): Extend GraphQL API by wrapping existing REST endpoint
* [yaml-variables](examples/yaml-variables): Use variables in your `graphcool.yml`

### Frontend examples

* [react-graphql](https://github.com/graphcool-examples/react-graphql): React code examples with GraphQL, Apollo, Relay, Auth0 & more
* [react-native-graphql](https://github.com/graphcool-examples/react-native-graphql): React Native code examples with GraphQL, Apollo, Relay, Auth0 & more
* [vue-graphql](https://github.com/graphcool-examples/vue-graphql): Vue.js code examples with GraphQL, Apollo & more
* [angular-graphql](https://github.com/graphcool-examples/angular-graphql): Angular code examples with GraphQL, Apollo & more
* [ios-graphql](https://github.com/graphcool-examples/ios-graphql): React code examples with GraphQL, Apollo, Relay, Auth0 & more

## Architecture

![](https://imgur.com/zkN1wWT.png)


## Deployment

### Local Development

### Graphcool Cloud

## Philosophy

## FAQ

### Wait a minute – isn't Graphcool a Backend-as-a-Service?

While Graphcool started out as a Backend-as-a-Service (like Firebase or Parse), [we're currently in the process](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333) of turning Graphcool into a backend development framework. No worries, you can still deploy your Graphcool services to the BaaS platform as before but additionally you can now also run Graphcool on your own machine.

### Why is Graphcool Core written in Scala?

### Is the API Gateway layer needed?

## Roadmap

### Latest release

### Open feature proposals

Help us shape the future of the Graphcool Framework by :thumbsup: [existing Feature Requests](https://github.com/graphcool/graphcool/issues?q=is%3Aopen+is%3Aissue+label%3Akind%2Ffeature) or [creating new ones](https://github.com/graphcool/graphcool/issues/new)

We are in the process of setting up a formal road map. Check back here in the coming weeks
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

We love your ideas for new features. If you're missing a certain feature, please feel free to [request a new feature here](https://github.com/graphcool/graphcool/issues/new). (Please make sure to check first if somebody else already requested it.)
