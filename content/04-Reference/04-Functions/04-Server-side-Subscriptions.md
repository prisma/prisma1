---
alias: bboghez0go
description: Subscriptions are a simple yet powerful event-based concept on top of GraphQL to implement business logic asynchronously.
---

# Subscriptions

Subscriptions are a simple yet powerful concept to handle business logic in your project. You can **subscribe to specific events**.

> Unlike hooks, functions for subscriptions are called **asynchronously**, _after_ a mutation was entirely processed.

## Trigger and Input Data

The trigger and input data for the function that is called as a reaction to server-side subscriptions follow the [Subscription API](!alias-aip7oojeiv).

#### Example

> Send an email when a new customer is created

```js
const fetch = require('isomorphic-fetch')
const Base64 = require('Base64')
const FormData =require('form-data')

const apiKey = '__MAILGUN_API_KEY__'
const url = '__MAILGUN_URL__'

module.exports = function (event) {

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

## Current Limitations

Currently, no Server-Side Subscriptions are triggered for the `File` type.
