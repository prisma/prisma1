<p align="center"><a href="https://www.graph.cool"><img src="https://imgur.com/he8RLRs.png"></a></p>

[Website](https://www.graph.cool/) • [Docs](https://docs-next.graph.cool/) • [Blog](https://blogs.graph.cool/) • [Forum](https://www.graph.cool/forum) • [Slack](https://slack.graph.cool/) • [Twitter](https://twitter.com/graphcool)

[![CircleCI](https://circleci.com/gh/graphcool/graphcool.svg?style=shield)](https://circleci.com/gh/graphcool/graphcool) [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://img.shields.io/badge/npm%20package-next-brightgreen.svg)](https://badge.fury.io/js/graphcool)

**Graphcool is a GraphQL backend framework** to develop and deploy production-ready GraphQL microservices.</br>
Think about it like Rails, Django or Meteor but based on [GraphQL](https://www.howtographql.com/) and designed for today's cloud infrastructure.

The framework currently supports Node.js & Typescript and is compatible with existing libraries and tools like [GraphQL.js](https://github.com/graphql/graphql-js) and [Apollo Server](https://github.com/apollographql/apollo-server). Graphcool comes with a CLI and a Docker-based runtime which can be deployed to any server or cloud.

<!-- 
Add note that Graphcool can still be used with other langs via webhooks??
-->

The framework provides powerful abstractions and building blocks to develop flexible, scalable GraphQL backends:
    
1. **GraphQL-native [database](https://docs-next.graph.cool/reference/database/overview-viuf8uus7o) mapping** to easily evolve your data schema & migrate your database
2. **Flexible [auth](https://docs-next.graph.cool/reference/auth/overview-ohs4aek0pe) workflows** using the JWT-based authentication & permission system
3. **Realtime API** using GraphQL [Subscriptions](https://docs-next.graph.cool/reference/graphql-api/subscription-api-aip7oojeiv)
4. **Highly scalable architecture** enabling asynchronous, event-driven flows using serverless [functions](https://docs-next.graph.cool/reference/functions/overview-aiw4aimie9)
5. **Works with all frontend frameworks** like React, Vue.js, Angular ([Quickstart Examples](https://docs-next.graph.cool/quickstart/))

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

  [`graphcool.yml`](https://docs-next.graph.cool/reference/service-definition/graphcool.yml-foatho8aip) is the root definition of a service where `types`, `permissions` and `functions` are referenced.

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

  To deploy your service simply run the following command and select either a hosted BaaS [cluster](https://docs-next.graph.cool/reference/graphcool-cli/.graphcoolrc-zoug8seen4) or automatically setup a local Docker-based development environment:

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

* GraphQL-native database mapping & migrations
* JWT-based authentication & flexible permission system
* Realtime GraphQL Subscription API
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

![](https://imgur.com/zkN1wWT.png)

## Deployment

Graphcool services can be deployed with [Docker](https://docker.com/) or the [Graphcool Cloud](http://graphcool-v3.netlify.com/cloud).

### Docker

You can deploy a Graphcool service to a local environment using Docker. The CLI offers the `graphcool local` commands with a number of _subcommands_ for that.

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

## Philosophy

**Building applications should be as easy as composing Lego bricks** - enabling this modularity is what the Graphcool Framework is striving for. Simple yet powerful abstractions and well-defined interfaces are key for this mission.

> The Graphcool Framework is heavily inspired by the [12-Factor App](https://12factor.net/) manifesto for modern application development.

**Declarative programming** allows for highly expressive and concise code by _describing_ the functionality of an application in dedicated terms. Graphcool provides you with powerful, declarative building blocks while giving you the flexibility to deal with custom application requirements.

A key principle from the above mentioned [12-Factor App](https://12factor.net/) manifesto is a **clear separation between _configuration_ and actual _code_**. The Graphcool Framework is strongly based on this idea:

- The `graphcool.yml` serves as the root configuration file for your Graphcool service, it describes all major components in your backend. Further configuration can be done with environment variables that can be accessed inside `graphcool.yml` or to be used in functions.
- The data model is specified in a declarative manner using the GraphQL SDL. The database mapping and generation of CRUD is performed by the framework.

**Separating stateful from stateless infrastructure is key in order to enable high scalability for your application**. In the Graphcool Framework, the database (_stateful_) is decoupled from the functions that implement business logic (_stateless_). The communication happens via an _event gateway_, thus enabling you to architect your applications in an event-driven manner. Despite the loose coupling of application components, all communication is typesafe thanks to the GraphQL schema.

**Graphcool services are designed from the ground up to run in cloud environments.** The Graphcool Framework supports automated CI/CD workflows for rapid deployment cycles. 

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

While Graphcool started out as a Backend-as-a-Service (like Firebase or Parse), [we're currently in the process](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333) of turning Graphcool into a backend development framework. No worries, you can still deploy your Graphcool services to the BaaS platform as before but additionally you can now also run Graphcool on your own machine.

### Why is Graphcool Core written in Scala?

Graphcool is an extremely complex and ambitious framework. We started out building Graphcool with Node but soon realized that it wasn't the right choice for the complexity Graphcool needed to deal with.

To be able to develop safely while iterating quickly, a powerful type system is an indispensable tool - neither TypeScript nor Flow were appropriate options here. Scala's support for functional programming techniques and its strong overall performance are further language/runtime properties that made Scala a great fir for our use case. 

Another important consideration was that Scala had one of the most mature GraphQL reference implementations ([Sangria](https://github.com/sangria-graphql)) when we started building Graphcool. 


### Is the API Gateway layer needed?

The API gateway is an _optional_ layer for your API, adding it to your service is not required. It is however an extremely powerful tool catering many real-world use cases, for example:

- Tailor your GraphQL schema and expose custom operations (based on the underlying CRUD API)
- Intercept HTTP requests before they reach the CRUD API; adjust the HTTP response before it's returned
- Implement persisted queries
- Integrate existing systems into your service's GraphQL API
- File management

Also realize that when you're not using an API gateway, _your service endpoint allows everyone to view all the operations of your CRUD API_. The entire data model can be deduced from the exposed CRUD operations.

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
