---
alias: nohcao8loo
description: Get started in 5 min with iOS, GraphQL and Apollo Client by building a simple Instagram clone.
github: https://github.com/graphcool-examples/ios-graphql/tree/master/quickstart-with-apollo
---

# iOS & Apollo Quickstart

For this quickstart tutorial, we have prepared a [repository](https://github.com/graphcool-examples/ios-graphql/tree/master/quickstart-with-apollo) that contains the full Swift code for the Instagram clone. All you need to do is create the Graphcool service that will expose the GraphQL API and connect it with the iOS application. Let's get started! 

<Instruction>

Clone the example repository that contains the iOS application:

```sh
git clone https://github.com/graphcool-examples/ios-graphql.git
cd ios-graphql/quickstart-with-apollo
```

</Instruction>

Feel free to get familiar with the code. The app contains the following [`view controllers`](https://github.com/graphcool-examples/ios-graphql/tree/master/quickstart-with-apollo/Instagram/Instagram):

- `PostCell`: Renders a single post item
- `PostTableViewController`: Renders a list of post items
- `AddPostViewController`: Allows to create a new post item

Graphcool services are managed with the [Graphcool CLI](!alias-zboghez5go). So before moving on, you first need to install it.

<Instruction>

Install the Graphcool CLI:

```sh
npm install -g graphcool
```

</Instruction>

Now that the CLI is installed, you can use it to create the file structure for new service with the [`graphcool init`](!alias-zboghez5go#graphcool-init) command.

<Instruction>

Create the local file structure for a new Graphcool service inside a directory called `server`:

```sh(path="")
# Create a local service definition in a new directory called `server`
graphcool init server
```

</Instruction>

`graphcool init` creates the local service structure inside the specified `server` directory:

```(nocopy)
.
└── server
    ├── graphcool.yml
    ├── types.graphql
    └── src
        ├── hello.graphql
        └── hello.js
```

Each of the created files and directories have a dedicated purpose inside your Graphcool service:

- `graphcool.yml`: Contains your [service definition](!alias-opheidaix3).
- `types.graphql`: Contains the [data model](!alias-eiroozae8u) and any additional type definitions for your Graphcool service, written in the GraphQL [Schema Definition Language](https://medium.com/@graphcool/graphql-sdl-schema-definition-language-6755bcb9ce51) (SDL).
- `src`: Contains the source code (and if necessary GraphQL queries) for the [functions](!alias-aiw4aimie9) you've configured for your service. Notice that a new service comes with a default "Hello World"-function (called `hello` in `graphcool.yml`) which you can delete if you don't want to use it.

Next you need to configure the [data model](!alias-eiroozae8u) for your service.

<Instruction>

Open `./server/types.graphql` and add the following type definition to it:

```graphql(path="server/types.graphql")
type Post {
  id: ID! @isUnique    # read-only (managed by Graphcool)
  createdAt: DateTime! # read-only (managed by Graphcool)
  updatedAt: DateTime! # read-only (managed by Graphcool)

  description: String!
  imageUrl: String!
}
```

</Instruction>

The changes you introduced by adding the `Post` type to the data model are purely _local_ so far. So the next step is to actually [deploy](!alias-aiteerae6l#graphcool-deploy) the service!

<Instruction>

Navigate to the `server` directory and deploy your service:

```sh(path="")
cd server
graphcool deploy
```

When prompted which cluster you want to deploy to, choose any of the **Backend-as-a-Service** options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`).

</Instruction>

> **Note**: If you haven't authenticated with the Graphcool CLI before, this command is going to open up a browser window and ask you to login. Your authentication token will be stored in the global [`~/.graphcoolrc`](!alias-zoug8seen4).

You service is now deployed and available via the HTTP endpoints that were printed in the output of the command! The `Post` type is added to your data model and the corresponding CRUD operations are generated and exposed by the [GraphQL API](!alias-abogasd0go).

Notice that this command also created the _local_ [`.graphcoolrc`](!alias-zoug8seen4) inside the current directory. It's used to manage your _deployment targets_.

<Instruction>

Save the HTTP endpoint for the `Simple API` from the output of the `graphcool deploy` command, you'll need it later!

</Instruction>

> **Note**: If you ever lose the endpoint for your GraphQL API, you can simply get access to it again by using the `graphcool info` command. When using Apollo, you need to use the endpoint for the `Simple API`.

You can test the API inside a [GraphQL Playground](https://github.com/graphcool/graphql-playground) which you can open with the `graphcool playground` command. Feel free to try out the following query and mutation.

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

The next step is to connect the iOS application with the GraphQL API from your Graphcool service.

<Instruction>

Paste the HTTP endpoint for the `Simple API` that you saved after running `graphcool deploy` into `AppDelegate.swift` as the `uri` argument in the `createNetworkInterface` call:

```js(path="Instagram/AppDelegate.swift")
// replace `__SIMPLE_API_ENDPOINT__` with the endpoint from the previous step
let apollo = ApolloClient(url: URL(string: "__SIMPLE_API_ENDPOINT__")!)
```

</Instruction>

To use the Apollo iOS Client, you need to install [`apollo-codegen`](https://github.com/apollographql/apollo-codegen), a command line tool that will generate Swift types from your GraphQL queries & mutations at build-time.

<Instruction>

Install `apollo-codegen` with the following command in your terminal:

```sh
npm install -g apollo-codegen
```

</Instruction>

You can find more info the installation process in the [Apollo docs](http://dev.apollodata.com/ios/installation.html).



That's it. The last thing to do is actually launching the application 🚀

<Instruction>

Install dependencies and run the app in Xcode:

```sh(path="")
carthage update
```

</Instruction>


### Learn more

* Get more practical experience with our [Guides](https://graph.cool/docs/tutorials)
* Secure your API by learning about [Authentication](!alias-bee4oodood) & [Permissions](!alias-iegoo0heez)
* Implement business logic with [Functions](!alias-aiw4aimie9)
