---
alias: ohm2ouceuj
description: How to use the API
---

# How to use the API

The GraphQL API of your Graphcool database service API is consumed via HTTP. Depending on your environment, can either use plain HTTP to communicate with the API or a [GraphQL client](!alias-chig6ahxeo) library to facilitate the process.

In the following, we'll provide an overview of the different ways you can interface with the API.

## Authentication

TODO

## GraphQL Playground

The [GraphQL Playground](https://github.com/graphcool/graphql-playground) can be used to explore and run GraphQL mutations, queries and subscriptions.

To open up a Playground for your database service, simply run the `graphcool playground` command in the root directory of your service or paste your service's HTTP endpoint into the address bar of your browser.

## Apollo Client

### Usage

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

## graphql-request

[`graphql-request`](https://github.com/graphcool/graphql-request) is a minimal GraphQL client supporting Node and browsers for server-side scripts or simple applications.

### Usage

To set up the client with your endpoint and use the `request` method to send a GraphQL query or mutation:

```javascript
const { request } = require('graphql-request')

const query = `
query {
  movies {
    releaseDate
    actors {
      name
    }
  }
}`

request('https://api.graph.cool/simple/v1/movies', query).then(data => console.log(data))
```

## Plain HTTP

You can also communicate with the Simple API by using plain HTTP POST requests. For example, to query `movies`, do a POST request to your endpoint `https://api.graph.cool/simple/v1/__SERVICE_ID__`.

With `curl` you can query like this:

```bash
curl 'https://api.graph.cool/simple/v1/movies' -H 'content-type: application/json' --data-binary '{"query":"query {movies {id title}}"}' --compressed
```

This returns the following response:

```json
{
  "data": {
    "movies": [
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
        movies {
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
