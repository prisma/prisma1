---
alias: le1kohahng
description: Get started in 5 min with [React](https://facebook.github.io/react/), [Apollo Client](https://github.com/apollographql/apollo-client) and [GraphQL](https://www.graphql.org) and learn how to build a simple Instagram clone.
github: https://github.com/graphcool/frontend-examples/tree/master/react
---

# React & Apollo Quickstart

In this quickstart tutorial, you'll learn how to connect your React app directly to a Graphcool service. Note that this approach will only allow to perform CRUD operations on your data model. You won't be able to implement any custom business logic or other common features like authentication - if you need these features, see the corresponding [fullstack tutorial](!alias-tijghei9go).

> The code for this project can be found on [GitHub](https://github.com/graphcool/frontend-examples/tree/master/react).

<Instruction>

Clone the example repository that contains the React application:

```sh
git clone https://github.com/graphcool/frontend-examples.git
cd frontend-examples/react
```

</Instruction>

Feel free to get familiar with the code. The app contains the following React [`components`](https://github.com/graphcool-examples/react-graphql/tree/master/quickstart-with-apollo/src/components):

- `Post`: Renders a single post item
- `ListPage`: Renders a list of post items
- `CreatePage`: Allows to create a new post item
- `DetailPage`: Renders the details of a post item and allows to update and delete it

Graphcool services are managed with the [Graphcool CLI](!alias-zboghez5go). So before moving on, you first need to install it.

<Instruction>

Install the Graphcool CLI:

```sh
npm install -g graphcool
```

</Instruction>

<Instruction>

Navigate to the `database` directory and deploy your service:

```sh(path="")
cd database
graphcool deploy
```

</Instruction>


When prompted which cluster you want to deploy to, choose any of the _public cluster_ options (`graphcool-eu1` or `graphcool-us1`).

> **Note**: If you haven't authenticated with the Graphcool CLI before, this command is going to open up a browser window and ask you to login.

You service is now deployed and available via the HTTP endpoint that was printed in the output of the command! The `Post` type is added to your data model and the corresponding CRUD operations are generated and exposed by the GraphQL API.

<Instruction>

Save the HTTP endpoint for the GraphQL API from the output of the `graphcool deploy` command, you'll need it later!

</Instruction>

> **Note**: If you ever lose the endpoint for your GraphQL API, you can simply get access to it again by using the `graphcool info` command. When using Apollo, you need to use the endpoint for the GraphQL API.

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

The next step is to connect the React application with the GraphQL API from your Graphcool service.

<Instruction>

Paste the HTTP endpoint for the GraphQL API that you saved after running `graphcool deploy` into `./src/index.js` as the `uri` argument in the `HttpLink` constructor call:

```js(path="src/index.js")
// replace `__API_ENDPOINT__` with the endpoint from the previous step
const httpLink = new HttpLink({ uri: '__API_ENDPOINT__' })
```

</Instruction>

That's it. The last thing to do is actually launching the application 🚀

<Instruction>

Install dependencies and run the app:

```sh(path="")
yarn install
yarn start # open http://localhost:3000 in your browser
```

</Instruction>
