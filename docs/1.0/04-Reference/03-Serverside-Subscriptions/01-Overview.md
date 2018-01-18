---
alias: to1ahf0ob6
description: Overview
---

# Overview

## Introduction

A Serverside Subscription is equivalent in power to normal GraphQL subscriptions. That means you can use filters to retrieve only the events you are interested in and perform an arbitrary query on the selection set to get related data.

When a Serverside Subscription is set up, Prisma will monitor data changes and execute the associated query when applicable, just like normal GraphQL Subscriptions. The difference is the delivery mechanism.

Serverside Subscriptions are designed to work well with modern serverless infrastructure. Currently, Prisma support delivering events via webhooks and in the future we will add support for direct AWS Lambda invocation as well as different queue implementations.

## Configuration

You configure a Serverside Subscription by adding the `subscriptions` section in the `prisma.yml` file for your service.

### Prisma.yml

```yml
service: my-service

stage: ${env:GRAPHCOOL_STAGE}
secret: ${env:GRAPHCOOL_SECRET}
cluster: ${env:GRAPHCOOL_CLUSTER}

datamodel: database/datamodel.graphql

subscriptions:
  userChangedEmail:
    webhook:
      url: http://example.org/sendSlackMessage
      headers:
        Content-Type: application/json
        Authorization: Bearer cha2eiheiphesash3shoofo7eceexaequeebuyaequ1reishiujuu6weisao7ohc
    query: |
      {
        user({
          where: {
            mutation_in: UPDATED,
            updatedFields_contains: "email"
          }
        }) {
          name
          email
        }
      }
```

## Example

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
