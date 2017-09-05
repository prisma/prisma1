---
alias: duj3oonog5
path: /docs/reference/simple-api/subscription-requests
layout: REFERENCE
shorttitle: Subscription Requests
description: GraphQL Subscriptions can be created using subscriptions-transport-ws or any WebSocket client.
simple_relay_twin: eih4eew7re
tags:
  - simple-api
related:
  further:
  more:
---

# Subscription Requests

When using Apollo Client, you can use `subscription-transport-ws` to combine it with a WebSocket client. [Here's an example](https://github.com/graphcool-examples/react-graphql/tree/master/subscriptions-with-apollo-instagram).

You can also use the GraphQL Playground or any WebSocket client as described below.

## Playground

The [Graphcool Playground](!alias-oe1ier4iej) can be used to explore and run GraphQL subscriptions.

Before diving into a specific implementation, **it's often better to get familiar with the available operations in the playground first**.

## Plain WebSockets

### Establish connection

Subscriptions are managed through WebSockets. First establish a WebSocket connection and specify the `graphql-subscriptions` protocol:

```javascript
let webSocket = new WebSocket('wss://subscriptions.graph.cool/v1/__PROJECT_ID__', 'graphql-subscriptions');
```
### Initiate Handshake

Next you need to initiate a handshake with the WebSocket server. You do this by listening to the `open` event and then sending a JSON message to the server with the `type` property set to `init`:

```javascript
webSocket.onopen = (event) => {
  const message = {
      type: 'init'
  }

  webSocket.send(JSON.stringify(message))
}
```

### React to Messages

The server may respond with a variety of messages distinguished by their `type` property. You can react to each message as appropriate for your application:

```javascript
webSocket.onmessage = (event) => {
  const data = JSON.parse(event.data)

  switch (data.type) {
    case 'init_success': {
      console.log('init_success, the handshake is complete')
      break
    }
    case 'init_fail': {
      throw {
        message: 'init_fail returned from WebSocket server',
        data
      }
    }
    case 'subscription_data': {
      console.log('subscription data has been received', data)
      break
    }
    case 'subscription_success': {
      console.log('subscription_success')
      break
    }
    case 'subscription_fail': {
      throw {
        message: 'subscription_fail returned from WebSocket server',
        data
      }
    }
  }
}
```

### Subscribe to Data Changes

To subscribe to data changes, send a message with the `type` property set to `subscription_start`:

```javascript
const message = {
  id: '1',
  type: 'subscription_start',
  query: `
    subscription newPosts {
      Post(filter: {
        mutation_in: [CREATED]
      }) {
        mutation
        node {
          description
          imageUrl
        }
      }
    }
  `
}

webSocket.send(JSON.stringify(message))
```

You should receive a message with `type` set to `subscription_success`. When data changes occur, you will receive messages with `type` set to `subscription_data`. The `id` property that you supply in the `subscription_start` message will appear on all `subscription_data` messages, allowing you to multiplex your WebSocket connection.

### Unsubscribe from Data Changes

To unsubscribe from data changes, send a message with the `type` property set to `subscription_end`:

```javascript
const message = {
  id: '1',
  type: 'subscription_end'
}

webSocket.send(JSON.stringify(message))
```
