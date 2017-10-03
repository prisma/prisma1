---
alias: tijghei9go
description: Get started in 5 min with [React](https://facebook.github.io/react/), [Apollo Client](https://github.com/apollographql/apollo-client) and [GraphQL](https://www.graphql.org) and learn how to build a simple Instagram clone.
github: https://github.com/graphcool-examples/react-graphql/tree/master/quickstart-with-apollo
---

# React & Apollo Quickstart

For this quickstart tutorial, we have prepared a [repository](https://github.com/graphcool-examples/react-graphql/tree/master/quickstart-with-apollo) that contains the full React code for the Instagram clone. All you need to do is create the Graphcool project that will expose the GraphQL API and connect it with the React application. Let's get started! 

<Instruction>

*Clone the example repository that contains the React application:*

```sh
git clone https://github.com/graphcool-examples/react-graphql.git
cd react-graphql/quickstart-with-apollo
```

</Instruction>

Feel free to get familiar with the code. The project contains the following React [`components`](https://github.com/graphcool-examples/react-graphql/tree/master/quickstart-with-apollo/src/components):

- `Post`: Renders a single post item
- `ListPage`: Renders a list of post items
- `CreatePage`: Allows to create a new post item
- `DetailPage`: Renders the details of a post item and allows to update and delete it

Graphcool projects are managed with the [Graphcool CLI](!alias-zboghez5go). So before moving on, you first need to install it.

<Instruction>

*Install the Graphcool CLI:*

```sh
npm install -g graphcool
```

</Instruction>

Now that the CLI is installed, you can use it to create a new project with the [`graphcool init`](!alias-zboghez5go#graphcool-init) command.

<Instruction>

Create a new _blank_ Graphcool project inside a directory called `graphcool`:

```sh(path="")
graphcool init graphcool --template blank
```

From the output in the console, copy the endpoint for the GraphQL API and save it. You'll need it in _Step 6_.

</Instruction>

> Note: If you haven't authenticated with the Graphcool CLI before, this command is going to open up a browser window and ask you to login. Your authentication token will be stored in `~/.graphcool`.

`graphcool init` creates a new project inside your Graphcool account as well as the local project structure inside the specified `graphcool` directory:

```(nocopy)
.
└── graphcool
    ├── graphcool.yml
    ├── types.graphql
    ├── .graphcoolrc
    └── code
        ├── hello.graphql
        └── hello.js
  ```

Each of the created files and directories have a dedicated purpose inside your Graphcool project:

- `graphcool.yml`: Contains your [project definition](opheidaix3#project-definition).
- `types.graphql`: Contains all the type definitions for your project, written in the GraphQL [Schema Definitiona Language](https://medium.com/@graphcool/graphql-sdl-schema-definition-language-6755bcb9ce51) (SDL).
- `.graphcoolrc`: Contains information about the [environments](!alias-opheidaix3#environments) that you have configured for your project.
- `code`: Contains the source code (and if necessary GraphQL queries) for the [functions](!alias-aiw4aimie9) you've configured for your project. Notice that a _blank_ project comes with a default "Hello World"-function which you can delete if you don't want to use it.

Next you need to configure the [data model](!alias-eiroozae8u) for your project.

<Instruction>

*Open `./graphcool/types.graphql` and add the following type definition to it:*

```graphql(path="graphcool/types.graphql")
type Post {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  description: String!
  imageUrl: String!
}
```

</Instruction>

The changes you introduced by adding the `Post` type to the data model are purely _local_ so far. So the next step is to sync these changes with the remote project that's in your Graphcool account!

<Instruction>

*Navigate into the `graphcool` directory and apply your local changes to the remote project in your Graphcool account:*

```sh(path="")
cd graphcool
graphcool deploy
```

</Instruction>


The `Post` type is now added to your data model and the corresponding CRUD operations are generated and exposed by the GraphQL API.

You can test the API inside a [GraphQL Playground](!alias-uh8shohxie#playground) which you can open with the `graphcool playground` command. Try out the following query and mutation.

**Fetching all posts:**

```graphql
query {
  allPosts {
    id
    description
    imageUrl
  }
}
```

**Creating a new post:**

```graphql
mutation {
  createPost(
    description: "A rare look into the Graphcool office"
    imageUrl: "https://media2.giphy.com/media/xGWD6oKGmkp6E/200_s.gif"
  ) {
    id
  }
}
```

![](https://imgur.com/w95UEi9.gif)


The next step is to connect the React application with the GraphQL API from your Graphcool project.

<Instruction>

*Copy the endpoint for the GraphQL API endpoint (from Step 3)  to `./src/index.js` as the `uri` argument in the `createNetworkInterface` call:*

```js(path="src/index.js")
// replace `__SIMPLE_API_ENDPOINT__` with the endpoint from the previous step
const networkInterface = createNetworkInterface({ uri: '__SIMPLE_API_ENDPOINT__' })
```

</Instruction>

> **Note**: If you ever lose your GraphQL endpoint, you can simply use the `graphcool info` command which will print it again.

That's it. The last thing to do is actually launching the application 🚀

<Instruction>

Install dependencies and run the app:

```sh(path="")
yarn install
yarn start # open http://localhost:3000 in your browser
```

</Instruction>


### Learn more

* [Advanced GraphQL features](https://blog.graph.cool/advanced-graphql-features-of-the-graphcool-api-5b8db3b0a71)
* [Authentication & Permissions](https://www.graph.cool/docs/reference/auth/overview-ohs4aek0pe/)
* [Implementing business logic with serverless functions](https://www.graph.cool/docs/reference/functions/overview-aiw4aimie9/)
