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

2. **Init new project:**

  ```sh
  graphcool init
  ```

3. **Configure data model & project:**

  Edit `types.graphql` to define your data model and setup functions & permissions in  `graphcool.yml`.

  The `graphcool.yml` file is the core of your application and should look like this:

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

4. **Deploy your changes:**

  You can deploy your changes and migrate your database by running the following command:

  ```sh
  graphcool deploy
  ```

5. **Connect to your GraphQL endpoint:**

  Use the endpoint from step (2) in your frontend (or backend) applications to connect to your GraphQL API.

## Features


## Examples

* [crud-api](examples/crud-api): Simple CRUD-style GraphQL API
* [auth](examples/auth): Email/password-based authentication & user permissions
* [send-email](examples/send-email): Send email via Mailgun via GraphQL mutation
* [push-notifications](examples/push-notifications): Send push notification when new Post is created
* [env-variables](examples/env-variables): Function accessing environment variables
* [rest-wrapper](examples/rest-wrapper): Extend GraphQL API by wrapping existing REST endpoint
* [full-example](examples/full-example): Full example (webshop) demoing most available features

## Architecture

## Deployment

## Philosophy

## FAQ

## Roadmap

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
