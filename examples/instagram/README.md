# instagram

This example demonstrates how to implement a simple "Email & Password"-based authentication workflow for a simple Instagram clone with Graphcool. It therefore uses the [`email-password-authentication`](https://github.com/graphcool/modules/tree/master/authentication/email-password) module and customizes the `EmailUser` type that the module provides.

## Functionality

### Data Model

The app is based on the following data model:

```graphql
type Post {
  id: ID! @isUnique
  createdAt: DateTime!
  description: String!
  imageUrl: String!
  author: EmailUser @relation(name: "UsersPosts")
}

type EmailUser implements Node {
  id: ID! @isUnique
  email: String @isUnique
  password: String
  name: String
  posts: [Post!]! @relation(name: "UsersPosts")
}
```

### Features

- Signup and login with email & password
- Check if a user is currently logged in
- Reading, creating, updating and deleting posts

### Authorization Rules

- Everyone can read posts
- Only authenticated users can create new posts
- Only the author of a post can update and delete


## Try it out

### Setup

To try out the example, first clone the repository and create your own Graphcool project based on the existing project definition in this directory:

```sh
git clone git@github.com:graphcool-examples/graphcool-examples.git
cd graphcool-examples/instagram
npm install -g graphcool@beta # if you don't have the latest CLI version installed
graphcool init
```

`graphcool init` creates a new Graphcool project in your Graphcool account based on the definition in [`graphcool.yml`](./graphcool.yml). It also creates a `.graphcoolrc` file where it adds this new project as the default environment.


### Playground

You can now open up a GraphQL playground to execute queries and mutations against your project:

```sh
graphcool playground
```

### Function Logs

If you want to see when the functions of the project are invoked and what their input and output is, check the function logs for any of the three functions that are configured for the project:

```sh
graphcool logs -f authenticate
# or: graphcool logs -f signup
# or: graphcool logs -f authenticatedEmailUser
```


## Project Structure

```sh
.
├── README.md
├── code
│   └── authenticatedEmailUser.js
├── graphcool.yml
├── modules
│   └── email-password
│       ├── code
│       │   ├── authenticate.js
│       │   └── signup.js
│       ├── graphcool.yml
│       ├── schemas
│       │   ├── authenticate.graphql
│       │   └── signup.graphql
│       └── types.graphql
├── schemas
│   └── authenticatedEmailUser.graphql
└── types.graphql
```
