---
alias: bboghez0go
description: Subscriptions are a simple yet powerful event-based concept on top of GraphQL to implement business logic asynchronously.
---

# Subscriptions

## Overview

Subscriptions are a simple yet powerful concept to handle business logic in your project. You can **subscribe to specific events** that are happening inside the GraphQL engine. All (successful) mutations are considered events.

> Unlike hooks, functions for subscriptions are called **asynchronously**, _after_ a database transaction was entirely processed.

## Input type

The input data for subscription functions is determined by the subscription query that you write for the subscription.

The concrete shape of the subscription query is determined by the [Subscription API](!alias-aip7oojeiv).

## Adding a Subscription function to the project

When you want to create a subscription function in your Graphcool project, you need to add it to the project configuration file under the `functions` section. 

### Example

Here is an example of a subscription function:

```yaml
functions:
  sendWelcomeEmail:
    type: subscription
    query: newUser.graphql
    handler:
      webhook: http://example.org/welcome-email
```

This is what the referred `newUser.graphql ` contains:

```graphql
subscription {
  User(filter: {
    operation_in: [CREATED]
  }) {
    node {
      id
      email
      name
    }
  }
}
```

`sendWelcomeEmail ` is invoked _after_ a `User` node was created and is defined as a _webhook_. It receives as input the payload of the subscription query that's defined in `newUser.graphql`, i.e. the new user's `id`, `name` and `email`.

### Properties

Each function that's specified in the project configuration file needs to have the `type` and `handler` properties.

For subscription functions, you additionally need to specify the `query` property which points to a file containing a regular GraphQL subscription query.


## Example

#### Calling out to 3rd-party APIs

> Send an email when a new customer is created

```js
const fetch = require('isomorphic-fetch')
const Base64 = require('Base64')
const FormData =require('form-data')

const apiKey = '__MAILGUN_API_KEY__'
const url = '__MAILGUN_URL__'

export default event => {

  const form = new FormData()
  form.append('from', 'Nilan <nilan@graph.cool>')
  form.append('to', 'Nikolas <nikolas@graph.cool>')
  form.append('subject', 'Test')
  form.append('text', 'Hi')

  return fetch(url, {
    headers: {
      'Authorization': `Basic ${Base64.btoa(apiKey)}`
    },
    method: 'POST',
    body: form
  })
}
```

## Current limitations

Currently, no Subscriptions are triggered for the `File` type.
