---
alias: nohcao8loo
description: Get started in 5 min with iOS, GraphQL and Apollo Client by building a simple Instagram clone.
---

# iOS & Apollo Quickstart

* [Apollo Client](https://github.com/apollographql/apollo-client): Fully-featured, production ready caching GraphQL client
* [Graphcool](https://www.graph.cool): Flexible backend framework combining GraphQL + AWS Lambda

<Instruction>

*Clone example repository:*

```sh
git clone https://github.com/graphcool-examples/ios-graphql.git
cd ios-graphql/quickstart-with-apollo
```

</Instruction>

<Instruction>

*Create Graphcool project:*

```sh
# Install Graphcool CLI
npm install -g graphcool

# Create a new "blank" project inside a directory called "graphcool"
graphcool init graphcool --template blank
```

</Instruction>

This creates a new project inside your Graphcool account as well as the local project structure inside the `graphcool` directory:

```
.
└── graphcool
    ├── code
    │   ├── hello.graphql
    │   └── hello.js
    ├── graphcool.yml
    └── types.graphql

```

Read the documentation to learn more about the file structure and [project configuration](https://www.graph.cool/docs/reference/basics/project-configuration-t%28yaml%29-opheidaix3).

<Instruction>

*Next you need to configure your data model. Open `./graphcool/types.graphql` and add the following type definition to it:*

```graphql
type Post {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  description: String!
  imageUrl: String!
}
```

</Instruction>

<Instruction>

*Now apply the changes you just made locally to the remote project in your Graphcool account:*

```sh
cd graphcool
graphcool deploy
```

</Instruction>

The `Post` type is now added to your data model and the corresponding CRUD operations are generated.

<Instruction>

*The next step is to connect the app with your GraphQL API. Copy the `Simple API` endpoint into `AppDelegate.swift`  to instantiate the `ApolloClient`:*

```js
// replace `__SIMPLE_API_ENDPOINT__` with the endpoint from the previous step
let apollo = ApolloClient(url: URL(string: "__SIMPLE_API_ENDPOINT__")!)
```

</Instruction>

<Instruction>

To use the Apollo iOS Client, you need to install `apollo-codegen`, a command line tool that will generate Swift types from your GraphQL queries & mutations at build-time. 

```sh
npm install -g apollo-codegen
```

</Instruction>

You can find out more more in the [Apollo docs](http://dev.apollodata.com/ios/installation.html).

<Instruction>

That's it. You can now install the dependencies and run the app:


```sh
carthage update
```

Start the app in Xcode 🚀

</Instruction>


## Next steps

* [Advanced GraphQL features](https://blog.graph.cool/advanced-graphql-features-of-the-graphcool-api-5b8db3b0a71)
* [Authentication & Permissions](https://www.graph.cool/docs/reference/auth/overview-ohs4aek0pe/)
* [Implementing business logic with serverless functions](https://www.graph.cool/docs/reference/functions/overview-aiw4aimie9/)
* [Dive deeper into GraphQL on How to GraphQL](https://www.howtographql.com)


## Help & Community [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool)

Say hello in our [Slack](http://slack.graph.cool/) or visit the [Graphcool Forum](https://www.graph.cool/forum) if you run into issues or have questions. We love talking to you!

![](http://i.imgur.com/5RHR6Ku.png)
