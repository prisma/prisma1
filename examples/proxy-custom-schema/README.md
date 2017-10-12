# Custom Schema (API Proxy)

## Overview

[read-only Demo](https://graphqlbin.com/lx9I1)

## Get started

### 1. Download the example

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/proxy-custom-schema
cd proxy-custom-schema
```

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the [Graphcool CLI](https://docs-next.graph.cool/reference/graphcool-cli/overview-zboghez5go):

```sh
npm install -g graphcool@next
```

### 3. Deploy the Graphcool service

The next step is to [deploy](https://docs-next.graph.cool/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined inside the [`service`](./service) directory:

```sh
cd service
graphcool deploy
```

Copy the endpoint for the `Simple API` as you'll need it in the next step.

The service you just deployed provides a CRUD API for the `User` and `Post` model types that are defined in [./service/types.graphql](./service/types.graphql).

The goal of the proxy server is now to create a _custom_ GraphQL API that only exposes variants of the underlying CRUD API.

### 4. Configure and start the API proxy server

#### 4.1. Set the endpoint for the GraphQL CRUD API

You first need to connect the proxy to the CRUD API. Pase the the HTTP endpoint for the `Simple API` from the previous step into [./proxy/index.ts](./proxy/index.ts) as the value for `endpoint`, replacing the current placeholder `__SIMPLE_API_ENDPOINT__`:

```
const endpoint = '__SIMPLE_API_ENDPOINT__' // looks like: https://api.graph.cool/simple/v1/__SERVICE_ID__
```

> **Note**: If you ever lose your API endpoint, you can get access to it again by running `graphcool info` in the root directory of your service (where [`graphcool.yml`](./service/graphcool.yml) is located).


#### 4.2. Start the server

Navigate into the [`proxy`](./proxy) directory, install the node dependencies and start the server:

```sh
cd ../proxy
yarn install
yarn start
```

#### 4.3. Open GraphQL Playground

The API that's exposed by the proxy is now available inside a GraphQL Playground under the following URL:

[`http://localhost:3000/playground`](http://localhost:3000/playground)

