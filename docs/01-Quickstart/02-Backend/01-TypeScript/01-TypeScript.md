---
alias: rohd6ipoo4
description: Get started with in 5 min Graphcool and TypeScript by building a GraphQL backend and deploying it with Docker
github: https://github.com/graphcool-examples/react-graphql/tree/master/quickstart-with-apollo
---

# TypeScript

In this quickstart tutorial, you will learn how to build a GraphQL backend and deploy it locally with [Docker](https://docker.com/). The goal is to deploy a new Graphcool service that exposes a CRUD API for a simple data model. You will also add an API gateway that customizes the exposed operations using schema [stitching](https://dev.apollodata.com/tools/graphql-tools/schema-stitching.html) and [transformation](https://github.com/graphcool/graphql-transform-schema). Let's get started!

> The code for this project can be found on [GitHub](https://github.com/graphcool/graphcool/tree/master/examples/typescript-gateway-custom-schema). 


<Instruction>

Clone the example repository that contains the server code:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/typescript-gateway-custom-schema
cd typescript-gateway-custom-schema
```

</Instruction>

Here's the file structure of the project:

```(nocopy)
.
├── README.md
├── gateway
│   ├── index.ts
│   ├── package.json
│   ├── tsconfig.json
│   └── yarn.lock
└── service
    ├── graphcool.yml
    └── types.graphql
```

The `service` directory contains the [definition](!alias-opheidaix3) of your Graphcool service. [`graphcool.yml`](!alias-foatho8aip) is the main configuration file, it contains information about your [GraphQL types](!alias-eiroozae8u), [permission](!alias-iegoo0heez) setup, integrated [functions](!alias-aiw4aimie9) and more. The actual type definitions are specifed in `types.graphql` and referenced from inside `graphcool.yml`.

This service defition is based on the following data model:

```graphql
type User @model {
  id: ID! @isUnique # read-only (managed by Graphcool)
  name: String!
  alias: String! @isUnique
  posts: [Post!]! @relation(name: "UserPosts")
}

type Post @model {
  id: ID! @isUnique # read-only (managed by Graphcool)
  title: String!
  author: User! @relation(name: "UserPosts")
}
```

Graphcool services are managed with the [Graphcool CLI](!alias-zboghez5go). So before moving on, you first need to install it.

<Instruction>

Install the Graphcool CLI:

```sh
npm install -g graphcool
```

</Instruction>

> When starting out building a new service, you can use the [`graphcool init`](!alias-aiteerae6l#graphcool-init) command to bootstrap a new Graphcool service definition. In this example, we already provide you with a sample service definition.

In the next step, you're going to deploy the service locally using Docker. You don't need to use the Docker CLI directly, the deployment is completely based on the Graphcool CLI.

The first thing you need to do is [create a local cluster](!alias-ohs4asd0pe#create-a-local-cluster) in your global [`.graphcoolrc`](!alias-zoug8seen4). You can do this using the [`graphcool local up`](!alias-aiteerae6l#graphcool-local-up) command.

<Instruction>

Open a terminal and create a new local cluster with the following command:

```bash(path="")
graphcool local up
```

</Instruction>

This now created a new entry in the `clusters` list in your global `.graphcoolrc` (which is located in your _home_ directory), looking similar to the following:

```yml(nocopy)
clusters:
  local:
    host: 'http://localhost:60001'
    clusterSecret: >-
      eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDgwODI3NjMsImNsaWVudElkIjoiY2o4bmJ5bjE3MDAvMDAxNzdmNHZzN3FxNCJ9.sOyzwJplYF2x9YHXGVtnd-GneMuzEQauKQC9vLxBag0
```

Now that you have the Docker container running on `http://localhost:60001` and the corresponding cluster definition, you can [deploy](!alias-aiteerae6l#graphcool-deploy) the service to the cluster. 

<Instruction>

Navigate into the `service` directory and deploy the service:

```bash(path="")
cd service
graphcool deploy
```

When prompted which cluster you want to deploy to, choose the `local` cluster from the **Local (Docker)** section. This section lists all your local clusters that are defined in the global `.graphcoolrc`.

</Instruction>

This command now created a _local_ `.graphcoolrc` that contains information about the deployment targets for your service, looking similar to this:

```<nocopy></nocopy>
targets:
  dev: local/cj91c443l00050129uwb6a1u2
  default: dev
```

The `dev` deployment target `local/cj91c443l00050129uwb6a1u2` is composed of the cluster name (`local`) and a service ID. It's also set as the `default` target for subsequent CLI commands (so you don't have to pass the `--target` option every time using the CLI).

The output of the `deploy` command contains the endpoints for your service that you can use to run queries and mutations against the auto-generated CRUD [API](!alias-abogasd0go).

<Instruction>

From the output, _save_ the endpoint for the `Simple API`. You will need it in a bit.

</Instruction>

You'll now create some dummy data in the database using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

<Instruction>

Open a GraphQL Playground by executing the following command in the `service` directory:

```bash(path="service")
graphcool playground
```

</Instruction>

Once the Playground opened, you can send queries and mutations against your local API and its connected database. The following ([nested](!alias-ol0yuoz6go#nested-mutations)) mutation creates a new `User` node as well as three `Post` nodes, each of which have that `User` node set as the `author`.

<Instruction>

Paste the following mutation into the left pane of the Playground and click the _Play_-button (or use the keyboard shortcut CMD+Enter):

```grahpql
mutation {
  createUser(
    name: "John", 
    alias: "john", 
    posts: [
      { title: "GraphQL is awesome" }, 
      { title: "Declarative data fetching with GraphQL" },
      { title: "GraphQL & Serverless" }
    ]
  ) {
    id
  }
}
```

</Instruction>

> **Note**: It is important the `alias` of the User is set to `john`. Otherwise the API gateway won't return any data since the `alias` in this example is [hardcoded](https://github.com/graphcool/graphcool/blob/master/examples/typescript-gateway-custom-schema/gateway/index.ts#L43).

The next step is to setup and start the [API gateway](!alias-ucoohic9zu). In this example, the API gateway creates a custom schema on top of the CRUD API of the Graphcool service. 

This is what the custom schema, that defines the gateway's API, looks like:

```graphql(nocopy)
type Query {
  viewer: Viewer!
}

type Viewer {
  me: User
  topPosts(limit: Int): [Post!]!
}
```

When requests are sent to the API gateway, it will simply forward them to the underlying CRUD API where they will be resolved.

The `run` function in [`index.ts`](https://github.com/graphcool/graphcool/blob/master/examples/typescript-gateway-custom-schema/gateway/index.ts) performs four major steps in order to map CRUD API to the new schema:

1. Create local version of the CRUD API using [`makeRemoteExecutableSchema`](http://dev.apollodata.com/tools/graphql-tools/remote-schemas.html#makeRemoteExecutableSchema). [See the code](./gateway/index.ts#L13).
2. Define schema for the new API (the one exposed by the API gateway). [See the code](./gateway/index.ts#L21).
3. Merge remote schema with new schema using [`mergeSchemas`](http://dev.apollodata.com/tools/graphql-tools/schema-stitching.html#mergeSchemas). [See the code](./gateway/index.ts#L33).
4. Limit exposed operations from merged schemas (hiding all root fields except `viewer`) using [`transformSchema`](https://github.com/graphcool/graphql-transform-schema). [See the code](./gateway/index.ts#L56).


To get the API gateway up and running the first thing you need to do is connect it with the CRUD API. You can do this by pasting the endpoint of the CRUD API into [`index.ts`](https://github.com/graphcool/graphcool/blob/master/examples/typescript-gateway-custom-schema/gateway/index.ts).

<Instruction>

Open `./gateway/index.ts` and set the value for the `endpoint` constant to the endpoint of the `Simple API` that you saved in **Step 5**, replacing the current placeholder `__SIMPLE_API_ENDPOINT__`:

```js(path="gateway/index.ts")
const endpoint = '__SIMPLE_API_ENDPOINT__' // looks like: https://api.graph.cool/simple/v1/__SERVICE_ID__
```

</Instruction> 

> **Note**: If you ever lose your API endpoint, you can get access to it again by running `graphcool info` in the root directory of your service (where `graphcool.yml` is located).

The last step now is to start the API gateway.

<Instruction> 

Navigate to the `gateway` directory and start the server: 

```bash(path="gateway")
cd ../gateway
yarn install
yarn start
```

</Instruction> 

Now that the API gateway is deployed, you can send the exposed queries to it. 

<Instruction> 

Open a Playground for the API gateway by navigatint to [`http://localhost:3000/playground`](http://localhost:3000/playground) inside your browser.

To validate it works, you can now send the following query:

```
{
  viewer {
    me {
      id
      name
      posts(limit: 2) {
        title
      }
    }
  }
}
```

</Instruction> 

This will return two of of the info about the `User` and two of the three associated `Post` nodes.


### Learn more

* Get more practical experience with our [Guides](https://graph.cool/docs/tutorials)
* Secure your API by learning about [Authentication](!alias-bee4oodood) & [Permissions](!alias-iegoo0heez)
* Implement business logic with [Functions](!alias-aiw4aimie9)

