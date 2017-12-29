---
alias: chig6ahxeo
description: Frontend Clients
---

# Frontend Clients

In most cases when working with a GraphQL API, you won't want to use plain HTTP to send your requests to the API. Instead, you can use a GraphQL client library that's abstracting away many of the boilerplate you otherwise would have to write.

> Learn how to get started with Relay and Apollo in our [Quickstart examples](https://www.graph.cool/docs/quickstart/) or on [How to GraphQL](https://www.howtographql.com/)

## Popular GraphQL client libraries

There are three major GraphQL client libraries at the moment, varying in complexity, flexibility and power: `graphql-request`, Apollo Client and Relay.

### `graphql-request`

[`graphql-request`](https://github.com/graphcool/graphql-request) is a lightweight GraphQL client for JavaScript. It basically is just a thin wrapper on top of `fetch` providing a convencience API for sending queries and mutations, or for configuring HTTP headers.

### Apollo Client

[Apollo Client](https://www.apollographql.com/client/) is a fully-fledged, flexible and community-driven GraphQL client. Its main features are a normalized cache that it maintains for you based on your previous queries and mutations, as well as integrations with all major UI libraries (like React, Angular or Vue.js) and mobile platforms like iOS and Android.

### Relay

[Relay](https://facebook.github.io/relay/) is Facebook's homegrown GraphQL client which was open-sourced alongside GraphQL in 2015. Relay is very powerful and heavily optimized for performance. Similar to Apollo Client, it also manages a cache for you and has further features like optimistic UI updates and subscription support (which are also available with Apollo).

> Note that Relay's API is a bit more cumbersome and less intuitive to work with than Apollo's. If you're just starting out with GraphQL, Apollo might be the better choice.
