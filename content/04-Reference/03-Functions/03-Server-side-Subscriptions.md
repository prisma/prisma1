---
alias: bboghez0go
description: Server-side subscriptions are a simple yet powerful event-based concept on top of GraphQL to implement custom business logic asynchronously.
---

# Server-side Subscriptions

Server-side Subscriptions (SSS) are a simple yet powerful concept to handle your custom business logic workflow. You can **subscribe to specific events**

> Unlike functions as part of the [Request Pipeline](!alias-pa6guruhaf), functions for server-side subscriptions are called **asynchronously**, after the mutation was processed.

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
