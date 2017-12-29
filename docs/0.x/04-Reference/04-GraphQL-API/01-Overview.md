---
alias: abogasd0go
description: Graphcool provides an automatically generated CRUD API for your data model. It also offers a realtime API using GraphQL subscriptions and a dedicated API for file management.
---

# Overview

The basic idea of Graphcool is to provide an automatically generated CRUD API based on a data model that you specify for your service. This API further contains capabilities for filtering, ordering and pagination.

Each Graphcool service further comes with a realtime API that's based on GraphQL subscriptions and allows to react to _events_ that are happening in the system.

Notice that every Graphcool service by default comes with two different APIs:

- **`Simple API`**: Intuitive CRUD operations and data modelling
- **`Relay API`**: Adheres to the schema requirements of [Relay](https://facebook.github.io/relay/)

Notice that both APIs are still accessing the same underlying database!

> Unless you are using Relay for as a GraphQL client, we highly recommend you to always use the `Simple API`.

## Making requests against the GraphQL API

The GraphQL API is supposed to be consumed by GraphQL clients such as Apollo Client or `graphql-request` and can be also consumed with plain HTTP requests.

### GraphQL Playground

The [GraphQL Playground](https://github.com/graphcool/graphql-playground) can be used to explore and run GraphQL mutations, queries and subscriptions.

Before diving into a specific implementation, **it's often better to get familiar with the available operations in the playground first**.

### Apollo Client

#### Usage

To set up the client with your endpoint:

```javascript
import { ApolloClient } from 'apollo-client'
import { HttpLink } from 'apollo-link-http'
import { InMemoryCache } from 'apollo-cache-inmemory'

const client = new ApolloClient({
  link: new HttpLink({ uri: 'http://api.graph.cool/simple/v1/__SERVICE_ID__' }),
  cache: new InMemoryCache()
})
```

Now you can use the `ApolloClient` instance to perform queries and mutations. If you are unsure about the setup, check the [quickstart](https://www.graph.cool/docs/quickstart/).

### graphql-request

[`graphql-request`](https://github.com/graphcool/graphql-request) is a minimal GraphQL client supporting Node and browsers for server-side scripts or simple applications.

#### Usage

To set up the client with your endpoint and use the `request` method to send a GraphQL query or mutation:

```javascript
const { request } = require('graphql-request')

const query = `{
  Movie(title: "Inception") {
    releaseDate
    actors {
      name
    }
  }
}`

request('https://api.graph.cool/simple/v1/movies', query).then(data => console.log(data))
```

### Plain HTTP

You can also communicate with the Simple API by using plain HTTP POST requests. For example, to query `allUsers`, do a POST request to your endpoint `https://api.graph.cool/simple/v1/__SERVICE_ID__`.

With `curl` you can query like this:

```bash
curl 'https://api.graph.cool/simple/v1/movies' -H 'content-type: application/json' --data-binary '{"query":"query {allMovies {id title}}"}' --compressed
```

returning

```json
{
  "data": {
    "allMovies": [
      {
        "id": "cixos5gtq0ogi0126tvekxo27",
        "title": "Inception"
      },
      {
        "id": "cixxhjs04pm0h015815qnrkyu",
        "title": "The Dark Knight"
      },
      {
        "id": "cixxhneo4qd4e01503f08d2hc",
        "title": "Batman Begins"
      },
      {
        "id": "cixxhupwksrq50150i50j3lha",
        "title": "The Dark Knight Rises"
      }
    ]
  }
}
```

Mutations work similarly (note the `query` property in the passed body):

```bash
curl 'https://api.graph.cool/simple/v1/movies' -H 'content-type: application/json' --data-binary '{"query":"mutation {createMovie(releaseDate: \"2016-11-18\" title: \"Moonlight\") {id}}"}' --compressed
```

With `fetch` you could do:

```javascript
const response = await fetch('https://api.graph.cool/simple/v1/__SERVICE_ID__', {
  method: 'post',
  headers: {
    'content-type': 'application/json'
  },
  body: JSON.stringify({
    query: `
      query {
        allMovies {
          id
          title
        }
      }
    `
  })
})

const responseJSON = await response.json()
const data = responseJSON.data
```

#### Subscriptions

Subscriptions can be created using WebSockets. Read more about [the GraphQL Subscription Protocol](!alias-duj3oonog5).
