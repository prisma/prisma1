<p align="center"><a href="https://www.graph.cool"><img src="https://imgur.com/NNp55eL.png" width="314"></a></p>

[![CircleCI](https://circleci.com/gh/graphcool/graphcool.svg?style=shield)](https://circleci.com/gh/graphcool/graphcool) [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://img.shields.io/badge/npm%20package-next-brightgreen.svg)](https://badge.fury.io/js/graphcool)

[Website](https://www.graph.cool/) • [Docs](https://docs-next.graph.cool/) • [Forum](https://www.graph.cool/forum) • [Chat](https://slack.graph.cool/) • [Twitter](https://twitter.com/graphcool)


**Graphcool is a GraphQL backend development framework.** Think about it like Rails or Meteor but based on GraphQL and designed for today's (serverless) infrastructure.

* GraphQL abstraction over your database
* Enables scalable, event-driven architectures
* Compatible with existing tools (GraphQL.js, Apollo, Serverless)

## Contents

> **Note:** This is a preview version of the Graphcool Framework. More information in the [forum](https://www.graph.cool/forum/t/feedback-new-cli-beta/949).<br>
> This readme is currently WIP and there are still some [bugs & missing features]() in the framework.

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

  Next edit `types.graphql` to define your data model using the [GraphQL SDL notation](https://docs-next.graph.cool/reference/database/data-modelling-eiroozae8u).
  
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

4. **Implement permissions & business logic:**

  The `graphcool.yml` file is the core of the framework and can be used to implement any kind of authorization and business logic.

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

5. **Deploy your service:**

  To deploy your service simply run the following command and select either a hosted BaaS cluster or automatically setup a local Docker-based development environment:

  ```sh
  graphcool deploy
  ```

6. **Connect to your GraphQL endpoint:**

  Use the endpoint from the previous step in your frontend (or backend) applications to connect to your GraphQL API.

## Features


## Examples

* [crud-api](examples/crud-api): Simple CRUD-style GraphQL API
* [auth](examples/auth): Email/password-based authentication & user permissions
* [env-variables](examples/env-variables): Function accessing environment variables
* [rest-wrapper](examples/rest-wrapper): Extend GraphQL API by wrapping existing REST endpoint
* [full-example](examples/full-example): Full example (webshop) demoing most available features

## Architecture

Graphcool is a new kind of framework. We are in the process of writing technical articles explaining the architecture. Meanwhile you can check out this article detailing how we use the Graphcool Framework to operate a global GraphQL Backend as a Service:

[Graphcool Infrastructure Deep Dive](https://blog.graph.cool/new-regions-and-improved-performance-7bbc0a35c880)

## Deployment

## Philosophy

## FAQ

### Wait a minute – isn't Graphcool a Backend-as-a-Service?

While Graphcool started out as a Backend-as-a-Service (like Firebase or Parse), [we're currently in the process](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333) of turning Graphcool into a backend development framework. No worries, you can still deploy your Graphcool services to the BaaS platform as before but additionally you can now also run Graphcool on your own machine.

## Roadmap

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
