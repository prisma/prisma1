---
alias: to1ahf0ob6
description: Overview
---

# Overview

## Introduction

A server-side subscription is equivalent in power to normal GraphQL subscriptions. That means they have the same API, e.g. allowing to provide the same filters in order to only get notified for the events you are interested in.

When a server-side subscription is set up, Prisma will monitor data changes and execute the associated query when applicable, just like normal GraphQL Subscriptions. The difference is the delivery mechanism.

Server-side subscriptions are designed to work well with modern serverless infrastructure. Currently, Prisma support delivering events via webhooks and in the future we will add support for direct AWS Lambda invocation as well as different queue implementations.

## Configuration

You configure a server-side subscription by adding the [`subscriptions`](!alias-ufeshusai8#subscriptions-optional) property in the [`prisma.yml`](!alias-foatho8aip) file for your service.

### prisma.yml

```yml
service: my-service

stage: ${env:PRISMA_STAGE}
secret: ${env:PRISMA_SECRET}
cluster: ${env:PRISMA_CLUSTER}

datamodel: database/datamodel.graphql

subscriptions:
  userChangedEmail:
    webhook:
      url: http://example.org/sendSlackMessage
      headers:
        Content-Type: application/json
        Authorization: Bearer cha2eiheiphesash3shoofo7eceexaequeebuyaequ1reishiujuu6weisao7ohc
    query: |
      subscription {
        user(where: {
          mutation_in: [UPDATED]
        }) {
          node {
            name
            email
          }
        }
      }
```

### Example

The `userChangedEmail` subscription configured above would be triggered by a mutation like this:

```graphql
mutation {
  updateUser(
    data: { email: "new@email.com" },
    where: { id: "cjcgo976g5twb018740bzyy4q" }
  ) {
    id
  }
}
```
