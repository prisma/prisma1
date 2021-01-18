---
alias: vbadiyyee9
description: Learn how to access Prisma from a Node script using Prisma Bindings.
---

# Access Prisma from a Node script using Prisma Bindings

This tutorial teaches you how to access an existing Prisma service from a simple Node script. You will access your Prisma service using a nice API generated with _Prisma bindings_.

The tutorial assumes that you already have a running Prisma service, so please make sure to have the _endpoint_ of it available. If you're unsure about how you can get started with your own Prisma service, check one of these tutorials:

* [Setup Prisma on a Demo server](!alias-ouzia3ahqu)
* [Setup Prisma with a new MySQL Database](!alias-gui4peul2u)
* [Setup Prisma with a new Postgres Database](!alias-eiyov7erah)
* [Setup Prisma by connecting your empty MySQL Database](!alias-dusee0nore)
* [Setup Prisma by connecting your empty Postgres Database](!alias-aiy1jewith)

<InfoBox>

To ensure you're not accidentally skipping an instruction in the tutorial, all required actions are highlighted with a little _counter_ on the left.
<br />
💡 **Pro tip**: If you're only keen on getting started but don't care so much about the explanations of what's going on, you can simply jump from instruction to instruction.

</InfoBox>

## Step 1: Update the data model

You already have your existing Prisma service, but for this tutorial we need to make sure that it has the right data model for the upcoming steps.

Note that we're assuming that your data model lives in a single file called `datamodel.graphql`. If that's not the case, please adjust your setup.

<Instruction>

Open `datamodel.graphql` and update its contents to look as follows:

```graphql
type User {
  id: ID! @unique
  name: String!
  posts: [Post!]!
}

type Post {
  id: ID! @unique
  title: String!
  content: String!
  published: Boolean! @default(value: "false")
  author: User!
}
```

</Instruction>

<Instruction>

After you saved the file, open your terminal and navigate into the root directory of your Prisma service (the one where `prisma.yml` is located) and run the following command to update its GraphQL API:

```sh
prisma1 deploy
```

</Instruction>

The GraphQL API of your Prisma service now exposes CRUD operations for the `User` as well as the `Post` type that are defined in your data model and also lets you modify _relations_ between them.

## Step 2: Setup project directory

Now that you have updated your data model, you will setup your directory structure.

<Instruction>

Navigate into a new directory and paste the following commands in your terminal:

```sh
mkdir -p my-node-script/src
touch my-node-script/src/index.js
cd my-node-script
yarn init -y
```

</Instruction>

<Instruction>

Next, move the root directory of your Prisma service into `my-node-script` and rename it to `prisma`.

```sh
cd ..
mkdir my-node-script/prisma
mv datamodel.graphql prisma.yml my-node-script/prisma
cd my-node-script
```

</Instruction>

```
.
└── my-node-script
    ├── package.json
    ├── prisma
    │   ├── datamodel.graphql
    │   └── prisma.yml
    └── src
        └── index.js
```

<Instruction>

Next, install `prisma-binding`.

```sh
yarn add prisma-binding graphql
```

</Instruction>

## Step 3: Download the Prisma database schema

The next step is to download the GraphQL schema of Prisma's GraphQL API (also referred to as _Prisma database schema_) into your project so you can point [Prisma binding](https://github.com/prisma/prisma-binding) to them.

Downloading the Prisma database schema is done using the [GraphQL CLI](https://oss.prisma.io/content/GraphQL-CLI/01-Overview.html) and [GraphQL Config](https://oss.prisma.io/content/GraphQL-Config/Overview.html).

<Instruction>

Install the GraphQL CLI using the following command:

```sh
yarn global add graphql-cli
```

</Instruction>

<Instruction>

Next, create your `.graphqlconfig` in the root directory of the server (i.e. in the `my-node-script` directory):

```sh
touch .graphqlconfig.yml
```

</Instruction>

<Instruction>

Put the following contents into it, defining Prisma's GraphQL API as a _project_:

```yml
projects:
  prisma:
    schemaPath: src/generated/prisma.graphql
    extensions:
      prisma: prisma/prisma.yml
```

</Instruction>

The information you provide in this file is used by the GraphQL CLI as well as the GraphQL Playground.

<Instruction>

To download the Prisma database schema to `src/generated/prisma.graphql`, run the following command in your terminal:

```sh
graphql get-schema
```

</Instruction>

The Prisma database schema which defines the full CRUD API for your database is now available in the location you specified in the `projects.prisma.schemaPath` property in your `.graphqlconfig.yml` (which is `src/generated/prisma.graphql`).

<InfoBox>

💡 **Pro tip**: If you want the Prisma database schema to update automatically every time you deploy changes to your Prisma services (e.g. an update to the data model), you can add the following post-deployment [hook](!alias-ufeshusai8#hooks-optional) to your `prisma.yml` file:

```yml
hooks:
  post-deploy:
    - graphql get-schema -p prisma
```

</InfoBox>

## Step 4: Send queries and mutations

In this step, you will communicate with your Prisma service using Prisma binding.

Therefore, you first instnatiate a Prisma binding by pointing it to the schema you downloaded in the previous step. The binding instance needs to connect to your Prisma API, so you also need to provide the `endpoint` of your Prisma API (which you can find stored in `prisma/prisma.yml`).

The Prisma binding instance acts as a "JavaScript SDK" for your Prisma service. You can use its API to send queries and mutations to your Prisma database.

![](https://cdn-images-1.medium.com/max/2800/1*RzlbHpIbo1g46x3x2lPUkQ.png)

<Instruction>

Put the following code in `src/index.js`. It creates a `Prisma` binding instance and uses it to send queries and mutations to your Prisma service.

<InfoBox type=warning>

⚠️ **Important**: Remember to replace the value of `__YOUR_PRISMA_ENDPOINT__` with your Prisma endpoint, which is stored in `prisma/prisma.yml`.

</InfoBox>

```js
const { Prisma } = require("prisma-binding")

const prisma = new Prisma({
  typeDefs: "src/generated/prisma.graphql",
  endpoint: "__YOUR_PRISMA_ENDPOINT__"
})

prisma.mutation
  .createUser({ data: { name: "Alice" } }, "{ id name }")
  .then(console.log)
  // { id: 'cjhcidn31c88i0b62zp4tdemt', name: 'Alice' }
  .then(() => prisma.query.users(null, "{ id name }"))
  .then(response => {
    console.log(response)
    // [ { id: 'cjhcidn31c88i0b62zp4tdemt', name: 'Alice' } ]
    return prisma.mutation.createPost({
      data: {
        title: "Prisma rocks!",
        content: "Prisma rocks!",
        author: {
          connect: {
            id: response[0].id
          }
        }
      }
    })
  })
  .then(response => {
    console.log(response)
    /*
      { id: 'cjhcidoo5c8af0b62kv4dtv3c',
        title: 'Prisma rocks!',
        content: 'Prisma rocks!',
        published: false }
    */
    return prisma.mutation.updatePost({
      where: { id: response.id },
      data: { published: true }
    })
  })
  .then(console.log)
  /*
    { id: 'cjhcidoo5c8af0b62kv4dtv3c',
      title: 'Prisma rocks!',
      content: 'Prisma rocks!',
      published: true }
  */
  .then(() => prisma.query.users(null, "{ id posts { title } }"))
  .then(console.log)
  // [ { id: 'cjhcidn31c88i0b62zp4tdemt', posts: [ [Object] ] } ]
  .then(() => prisma.mutation.deleteManyPosts())
  .then(console.log)
  // { count: 1 }
  .then(() => prisma.mutation.deleteManyUsers())
  .then(console.log)
// { count: 1 }
```

</Instruction>

<Instruction>

Run the script to see the query results printed in your terminal.

```bash
node src/index.js
```

</Instruction>

## Step 5: Check for existence of specific nodes

Besides `query` and `mutation`, Prisma binding provides a handy property called `exists` which allows you to check whether a node with certain properties exists in the database that is managed by Prisma. The `exists` property exposes one function per type in your data model - each function is named after the type it represents (in your case that's `prisma.exists.User(filter)` and `prisma.exists.Post(filter)`). These functions receive _filter_ arguments and always return `true` or `false`.

Because the schema you created in **Step 1** has two models, `User` and `Post`, Prisma generated `exists.User` and `exists.Post`.

<Instruction>

Put the following code in `src/index.js`. It instantiates a Prisma binding instance and uses it to send queries and mutations to your Prisma service.

<InfoBox type=warning>

⚠️ **Important**: Remember to replace the value of `__YOUR_PRISMA_ENDPOINT__` with your Prisma endpoint, which is stored in `prisma/prisma.yml`.

</InfoBox>

```js
const { Prisma } = require("prisma-binding")

const prisma = new Prisma({
  typeDefs: "src/generated/prisma.graphql",
  endpoint: "__YOUR_PRISMA_ENDPOINT__"
})

prisma.mutation
  .createUser({ data: { name: "Alice" } }, "{ id name }")
  .then(response => {
    return prisma.mutation.createPost({
      data: {
        title: "Prisma rocks!",
        content: "Prisma rocks!",
        author: {
          connect: {
            id: response.id
          }
        }
      }
    })
  })
  .then(() => prisma.exists.User({ name: "Alice" }))
  .then(response => console.log(response))
  // true
  .then(() => prisma.exists.Post({ title: "Prisma rocks" }))
  .then(response => console.log(response))
  // true
  .then(() => prisma.mutation.deleteManyPosts())
  .then(() => prisma.mutation.deleteManyUsers())
  .then(() => prisma.exists.Post({ title: "Prisma rocks" }))
  .then(response => console.log(response))
  // false
  .then(() => prisma.exists.User({ name: "Alice" }))
  .then(console.log)
  // false
```

</Instruction>

<Instruction>

Run the script to see the query results printed in your terminal:

```bash
node src/index.js
```

</Instruction>

## Step 6: Send raw queries and mutations

Prisma binding also lets you send queries and mutations as strings to your Prisma service using the `request` property. This is more verbose because you need to give spell out the entire query/mutation, and also their responses have a little more overhead because they include the _name_ of the query/mutation as a top level key.

> Prisma binding's `request` uses [`graphql-request`](https://github.com/prismagraphql/graphql-request) under the hood and therefore has the same API.

<Instruction>

Replace the contents of `src/index.js` with the following code.

<InfoBox type=warning>

⚠️ **Important**: Remember to replace the value of `__YOUR_PRISMA_ENDPOINT__` with your Prisma endpoint, which is stored in `prisma/prisma.yml`.

</InfoBox>

```js
const { Prisma } = require("prisma-binding")

const prisma = new Prisma({
  typeDefs: "src/generated/prisma.graphql",
  endpoint: "__YOUR_PRISMA_ENDPOINT__"
})

const query = `
  {
    users {
      id
      name
    }
  }
`
const mutation = `
  mutation CreateUser($name: String!) {
    createUser(data: { name: $name }) {
      id
      name
    }
  }
`

const variables = { name: 'Bob' }

prisma.mutation
  .createUser({ data: { name: 'Alice' } }, '{ id name }')
  .then(console.log)
  // { id: 'cjhcijh30cgww0b622rwkkvbo', name: 'Alice' }
  .then(() => prisma.request(mutation, variables))
  .then(console.log)
  // { createUser: { id: 'cjhcijjndch0d0b62qux6o52a', name: 'Bob' } }
  .then(() => prisma.query.users(null, '{ id name }'))
  .then(console.log)
  /*
    [ { id: 'cjhciiacxcf850b62mcuaa3uz', name: 'Alice' },
      { id: 'cjhcijjndch0d0b62qux6o52a', name: 'Bob' } ]
  */
  .then(() => prisma.request(query))
  .then(console.log)
  /*
    { users:
      [ { id: 'cjhciiacxcf850b62mcuaa3uz', name: 'Alice' },
        { id: 'cjhcijjndch0d0b62qux6o52a', name: 'Bob' } ] }
  */
```

</Instruction>

<Instruction>

Run the script to see the query results printed in your terminal.

```bash
node src/index.js
```

</Instruction>
