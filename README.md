<!--
<p align="center"><a href="https://www.prismagraphql.com"><img src="https://imgur.com/he8RLRs.png"></a></p>
-->

[Website](https://www.prismagraphql.com) • [Docs](https://www.prismagraphql.com/docs/) • [Blog](https://blog.graph.cool/) • [Forum](https://www.graph.cool/forum) • [Slack](https://slack.graph.cool/) • [Twitter](https://twitter.com/graphcool)

[![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool) [![npm version](https://badge.fury.io/js/graphcool.svg)](https://badge.fury.io/js/graphcool)

**Prisma - turn your database into a GraphQL API**
With Prisma you can design your data model and have a production ready [GraphQL](https://www.howtographql.com/) API online in minutes.

Prismas GraphQL API provides powerful abstractions and building blocks to develop flexible, scalable GraphQL backends:

1. **Type-safe API** that can be used from frontend and backend, including filters, aggregations and transactions
2. **Data modelling** with declarative SDL. Prisma migrates your underlying database automatically
3. **Realtime API** using GraphQL Subscriptions
4. **Advanced API composition** using GraphQL Binding and schema stitching
5. **Works with all frontend frameworks** like React, Vue.js, Angular ([Quickstart Examples](https://prismagraphql.com/docs/quickstart/))

## Contents

<!--
<img align="right" width="400" src="https://imgur.com/EsopgE3.gif" />
-->

* [Quickstart](#quickstart)
* [Features](#features)
* [Examples](#examples)
* [Architecture](#architecture)
* [Community](#community)
* [Contributing](#contributing)

## Quickstart

[Watch this 5 min tutorial](https://www.youtube.com/watch?v=xmri5pNR9-Y) or follow the steps below to get started with Prisma:

1. **Install the CLI via NPM:**

```sh
npm install -g prisma-cli
```

2. **Create a new service:**

The following command creates all files you need for a new [service](https://www.prismagraphql.com/docs/reference/service-configuration/overview-ieshoo5ohm).

```sh
prisma init
```

3. **Define your data model:**

Edit `datamodel.graphql` to define your data model using the [GraphQL SDL notation](<https://www.prismagraphql.com/docs/reference/service-configuration/data-modelling-(sdl)-eiroozae8u>).

```graphql
type Tweet {
  id: ID! @unique
  createdAt: DateTime!
  text: String!
  owner: User!
  location: Location!
}

type User {
  id: ID! @unique
  createdAt: DateTime!
  updatedAt: DateTime!
  handle: String! @unique
  name: String
  tweets: [Tweet!]!
}

type Location {
  latitude: Float!
  longitude: Float!
}
```

5. **Deploy your service:**

To deploy your service simply run the following command and select one of the hosted development clusters or setup a local Docker-based development environment:

```sh
prisma deploy
```

6. **Connect to your GraphQL endpoint:**

Use the endpoint from the previous step in your frontend (or backend) applications to connect to your GraphQL API.

## Examples

[![Alt text](https://imgur.com/21kBMfD.png)](https://www.prismagraphql.com/docs/quickstart/)

## Architecture

Prisma is a secure API layer that sits in front of your database. Acting as a proxy, Prisma exposes a powerful GraphQL API and manages Rate-Limiting, Authentication, Logging and a host of other features. Because Prisma is a standalone process, it can be scaled independently from your application layer and provide scalable subscriptions infrastructure.

![](https://www.prismagraphql.com/static/media/frontend@2x.fef72487.png)

## GraphQL API

The most important component in Prisma is the GraphQL API:

* Query, mutate & stream data via GraphQL CRUD API
* Define and evolve your data model using GraphQL SDL

Try the online demo: [open GraphQL Playground](https://www.prismagraphql.com/features)

## Community

Prisma has a community of thousands of amazing developers and contributors. Welcome, please join us! 👋

* [Forum](https://www.graph.cool/forum)
* [Slack](https://slack.graph.cool/)
* [Twitter](https://twitter.com/graphcool)
* [Facebook](https://www.facebook.com/GraphcoolHQ)
* [Meetup](https://www.meetup.com/graphql-berlin)
* [Email](hello@graph.cool)

## Contributing

Your feedback is **very helpful**, please share your opinion and thoughts!

### +1 an issue

If an existing feature request or bug report is very important to you, please go ahead and :+1: it or leave a comment. We're always open to reprioritize our roadmap to make sure you're having the best possible DX.

### Requesting a new feature

We love your ideas for new features. If you're missing a certain feature, please feel free to [request a new feature here](https://github.com/graphcool/prisma/issues/new). (Please make sure to check first if somebody else already requested it.)
