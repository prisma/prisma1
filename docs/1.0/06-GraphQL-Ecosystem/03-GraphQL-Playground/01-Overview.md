---
alias: chaha125ho
description: GraphQL Playground is a GraphQL IDE for better development workflows (GraphQL subscriptions, interactive docs & collaboration).
---

# graphql-playground

🎮 [`graphql-playground`](https://github.com/graphcool/graphql-playground/) is a GraphQL IDE for better development workflows (GraphQL subscriptions, interactive docs & collaboration).

**You can download the [desktop app](https://github.com/graphcool/graphql-playground/releases) or use the web version at graphqlbin.com: [Demo](https://www.graphqlbin.com/RVIn)**

[![](https://i.imgur.com/AE5W6OW.png)](https://www.graphqlbin.com/RVIn)

## Features

* ✨ Context-aware autocompletion & error highlighting
* 📚 Interactive, multi-column docs (keyboard support)
* ⚡️ Supports real-time GraphQL Subscriptions
* ⚙  GraphQL Config support with multiple Projects & Endpoints
* 🚥 Apollo Tracing support

## FAQ

### How is this different from [GraphiQL](https://github.com/graphql/graphiql)?

GraphQL Playground uses components of GraphiQL under the hood but is meant as a more powerful GraphQL IDE enabling better (local) development workflows. Compared to GraphiQL, the GraphQL Playground ships with the following additional features:

* Interactive, multi-column schema documentation
* Automatic schema reloading
* Support for GraphQL Subscriptions
* Query history
* Configuration of HTTP headers
* Tabs

See the following question for more additonal features.

### What's the difference between the desktop app and the web version?

The desktop app is the same as the web version but includes these additional features:

* Support for [graphql-config](https://github.com/graphcool/graphql-config) enabling features like multi-environment setups.
* Double click on `*.graphql` files.

### How does GraphQL Bin work?

You can easily share your Playgrounds with others by clicking on the "Share" button and sharing the generated link. You can think about GraphQL Bin like Pastebin for your GraphQL queries including the context (endpoint, HTTP headers, open tabs etc).

![](https://imgur.com/H1n64lL.png)

> You can also find the announcement blog post [here](https://blog.graph.cool/introducing-graphql-playground-f1e0a018f05d).

## Usage

### Properties

All interfaces, the React component `<Playground />` and all middlewares expose the same set of options:

+ `properties`
  + `endpoint` [`string`] - the GraphQL endpoint url.
  + `subscriptionEndpoint` [`string`] - the GraphQL subscriptions endpoint url.
  + `setTitle` [`boolean`] - reflect the current endpoint in the page title

### As React Component

#### Install

```sh
yarn add graphql-playground
```

#### Use

GraphQL Playground provides a React component responsible for rendering the UI and Session management.
There are **3 dependencies** needed in order to run the `graphql-playground` React component.

1. _Open Sans_ and _Source Code Pro_ fonts
1. Including `graphql-playground/playground.css`
1. Rendering the `<Playground />` component

The GraphQL Playground requires **React 16**.

Including Fonts (`1.`)

```html
<link href="https://fonts.googleapis.com/css?family=Open+Sans:300,400,600,700|Source+Code+Pro:400,700" rel="stylesheet">
```

Including stylesheet and the component (`2., 3.`)

```js
import React from 'react'
import ReactDOM from 'react-dom'
import Playground from 'graphql-playground'
import 'graphql-playground/playground.css'

ReactDOM.render(<Playground endpoint="https://api.graph.cool/simple/v1/swapi" />, document.body)
```

### As Server Middleware

#### Install

```sh
# Pick the one that matches your server framework
yarn add graphql-playground-middleware-express  # for Express or Connect
yarn add graphql-playground-middleware-hapi
yarn add graphql-playground-middleware-koa
yarn add graphql-playground-middleware-lambda
```

#### Usage with example

We have a full example for each of the frameworks below:

- **Express:** See [packages/graphql-playground-middleware-express/examples/basic](https://github.com/graphcool/graphql-playground/tree/master/packages/graphql-playground-middleware-express/examples/basic)

- **Hapi:** See [packages/graphql-playground-middleware/examples/hapi](https://github.com/graphcool/graphql-playground/tree/master/packages/graphql-playground-middleware/examples/hapi)

- **Koa:** See [packages/graphql-playground-middleware/examples/koa](https://github.com/graphcool/graphql-playground/tree/master/packages/graphql-playground-middleware/examples/koa)

- **Lambda (as serverless handler):** See [serverless-graphql-apollo](https://github.com/serverless/serverless-graphql-apollo) or a quick example below.

### As serverless handler

#### Install

```sh
yarn add graphql-playground-middleware-lambda
```

#### Usage

`handler.js`

```js
import lambdaPlayground from 'graphql-playground-middleware-lambda'
// or using require()
// const lambdaPlayground = require('graphql-playground-middleware-lambda').default

exports.graphqlHandler = function graphqlHandler(event, context, callback) {
  function callbackFilter(error, output) {
    // eslint-disable-next-line no-param-reassign
    output.headers['Access-Control-Allow-Origin'] = '*'
    callback(error, output)
  }

  const handler = graphqlLambda({ schema: myGraphQLSchema })
  return handler(event, context, callbackFilter)
}

exports.playgroundHandler = lambdaPlayground({
  endpoint: '/dev/graphql',
})
```

`serverless.yml`

```yaml
functions:
  graphql:
    handler: handler.graphqlHandler
    events:
    - http:
        path: graphql
        method: post
        cors: true
  playground:
    handler: handler.playgroundHandler
    events:
    - http:
        path: playground
        method: get
        cors: true
```
