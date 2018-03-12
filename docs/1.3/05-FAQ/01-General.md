---
alias: iph5dieph9
description: Frequently asked questions about basic topics and general issues all around Prisma.
---

# General

### What is Prisma?

Prisma an _abstraction layer_ that turns your database into a GraphQL API. Instead of having to deal with SQL or the APIs of a NoSQL database, you'll be able to use the full power of [GraphQL](http://graphql.org/) to interact with your data.

GraphQL is the future of API development, many big companies (like Facebook, Twitter, Yelp, IBM and [more](http://graphql.org/users/)) as well as smaller startups and teams are using it in production already today. Prisma makes it easy to build your own GraphQL server while taking advantage of the rich [GraphQL ecosystem](https://www.prismagraphql.com/docs/graphql-ecosystem/).

> Note that GraphQL as well as Prisma are entirely **open source** - there is absolutely **no vendor lock-in** when building a GraphQL server with Prisma!

### How can I use Prisma?

There are two major ways for using Prisma:

- Use Prisma as the foundation for your own GraphQL server
- Access Prisma's GraphQL API directly from the frontend

With the first use case you have all the power and flexibility that comes along when building your own server. You can implemented your own business logic, provide custom authentication workflows and talk to 3rd-party APIs.

The second use case gets you up and running with a hosted GraphQL API very quickly. It doesn't allow for much business logic as the API only provides CRUD operations for the types in your data model (including features like sorting, filtering and pagination). It therefore is better suited for simple applications that only need to fetch and store some data, for prototyping or if you just want to learn how to use GraphQL!

Prisma services are managed with the [Prisma CLI](https://github.com/graphcool/prisma/tree/master/cli) which you can install as follows:

```sh
npm install -g prisma
```

The best way to get started is through our [Quickstart](https://www.prismagraphql.com/docs/quickstart/) page.

### What programming languages does Prisma work with?

Prisma (like GraphQL) is language agnostic and will work with any programming language. That said, the best support for building GraphQL servers is currently provided by the JavaScript ecosystem, thanks to tooling like `graphql-js` or [GraphQL bindings](https://blog.graph.cool/reusing-composing-graphql-apis-with-graphql-bindings-80a4aa37cff5). However, it is definitely feasible to use GraphQL in different programming languages as well. Prisma itself is implemented in Scala and TypeScript.

### What platforms and cloud providers does Prisma work with?

Prisma is based on [Docker](https://www.docker.com/) which really makes it up to the developer _where_ and _how_ Prisma should be deployed.

A few examples are [Digital Ocean](https://www.digitalocean.com/) (find a deployment tutorial [here](!alias-texoo9aemu)), [Microsoft Azure](https://azure.microsoft.com/en-us/), [Google Compute Engine](https://cloud.google.com/compute/), [AWS](https://aws.amazon.com/), [Heroku](https://www.heroku.com/).

<InfoBox>

The easiest way to run Prisma in production is with [Prisma Cloud](https://www.prismagraphql.com/cloud/).

</InfoBox>

### Who should use Prisma?

Prisma is _the_ tool for everyone who wants to build production-ready and scalable GraphQL servers - no matter the team size or project scope.
