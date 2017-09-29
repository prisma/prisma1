# cli-demo

This example helps you to get familiar with the [Graphcool CLI](https://github.com/graphcool/graphcool-cli).

Provided is a final version of the Graphcool project, including detailed step-by-step instructions to build it yourself!

# Overview

You can also watch [the introduction video first](https://www.youtube.com/watch?v=gg_SJ8a5xpA) or **[watch complementary walkthrough video](https://www.youtube.com/watch?v=gg_SJ8a5xpA)**

[![](https://imgur.com/GlBnYv5.png)](https://www.youtube.com/watch?v=gg_SJ8a5xpA)

## Getting Started

- installing the CLI
- initializing a project
- getting familiar with the project structure
- adding a type and exploring the API in the GraphQL Playground

## Github Authentication Module + Extending the API

- integrating with Github OAuth by adding the `github` module
- getting familiar with environment variables
- extending the API with a custom mutation
- learning more about advanced API capabilities like nested mutations and filters

## Business Logic

- setting up a  server side subscriptions
- getting familiar with function logs

---

# Instructions

## Getting Started
- install the CLI

```sh
npm install -g graphcool@beta
```

- initialize a new project
```sh
graphcool init # choose blank project when prompted
```

- open `graphcool.yml` and get familiar with the project structure

  - `types`: type definitions, including model types (reflected in DB and API) and more types
  - `modules`: used to structure your code in logical components
  - `functions`: extend your API or implement business logic
  - `permissions`: control Authorization for your project
  - `rootTokens`: used by functions and scripts to get full API access

- open types.graphql and add `Post` type

```graphql
type Post {
  id: ID! @isUnique
  description: String!
  imageUrl: String!
}
```

- deploy changes

```sh
graphcool deploy
```

- explore basic API for `Post` type

```sh
graphcool playground
```

You can try these operations:

```graphql
query {
  allPosts {
    id
    description
  }
}
```

```graphql
mutation {
  createPost(
    description: "A great holiday"
    imageUrl: "https://images.unsplash.com/photo-1500531279542-fc8490c8ea4d"
  ) {
    id
    imageUrl
  }
}
```

## Github Authentication Module
- download the github module

```sh
graphcool module add graphcool/modules/authentication/github
```

- add the module to your `graphcool.yml` file

```yml
modules:
  github: modules/github/graphcool.yml
```

- create a relation `AuthorPosts` between the new type `GithubUser` in `modules/github/types.graphql` and `Post` in `types.graphql`

  - add `posts: [Post!]! @relation(name: "AuthorPosts")` to `GithubUser` type
  - add `author: GithubUser @relation(name: "AuthorPosts")` to `Post`

```graphql
# in modules/github/types.graphql
type GithubUser {
  id: ID! @isUnique
  githubUserId: String @isUnique
  posts: [Post!]! @relation(name: "AuthorPosts")
}
```

```graphql
# in types.graphql
type Post {
  id: ID! @isUnique
  description: String!
  imageUrl: String!
  author: GithubUser @relation(name: "AuthorPosts")
}
```

- follow the instructions in the `github` module to create a new OAuth app at Github and configure your environment with the needed environment variables, for example by creating an `.envrc` file that you use with [direnv](https://direnv.net/)

```
export CLIENT_ID=
export CLIENT_SECRET=
```

- deploy the changes, including a new type, relation and function

```sh
graphcool deploy
```

- replace `__CLIENT_ID__` in `modules/github/login.html` with the client id of your OAuth app to create a Github code for testing the function

- generate a new test token:

```sh
cd modules/github/
python -m SimpleHTTPServer
open localhost:8000/login.html
# authenticate with Github and find your code in the DevTools of your browser
```

- Note that the token only works once! You can authenticate again to obtain a new token.

- use Github code to create a new user in Graphcool and obtain a Graphcool token

```sh
graphcool playground
```

```graphql
mutation {
  authenticateGithubUser(githubCode: "<code from index.html above>") {
    token
  }
}
```

- query the new user

```graphql
query {
  allGithubUsers {
    id
    githubUserId
    # we could have read more information from the Github API to in Graphcool here, for example the Github handle
  }
}
```

- copy the resulting `id` of the GithubUser for the following steps!

## API
- explore the API further, for example with a nested mutation

```graphql
# this creates a new post and connects it to the user via the `AuthorPosts` relation

mutation {
  createPost(
    authorId: "<id from above>"
    description: "A great holiday"
    imageUrl: "https://images.unsplash.com/photo-1500531279542-fc8490c8ea4d"
  ) {
    id
  }
}
```

## Business Logic
- add a new type `Like` with a relation to `Post` and `GithubUser` each

```graphql
# in types.graphql

type Post {
  # ...
  likes: [Like!]! @relation(name: "LikeOnPost")
}

type Like {
  id: ID! @isUnique
  post: Post @relation(name: "LikeOnPost")
  likedBy: GithubUser @relation(name: "LikeOnGithubUser")
}
```

```graphql
# in modules/github/types.graphql

type GithubUser {
  # ...
  likes: [Like!]! @relation(name: "LikeOnGithubUser")
}
```

- add a new subscription function by replacing `functions: {}` with the following in the `graphcool.yml` file:

```yml
# functions
functions:
  like:
    handler:
      code:
        src: ./src/like.js
    type: subscription
    query: ./src/like.graphql
```

- copy the code from `src/like.js` and `src/like.graphql` in this repository in your own project

- deploy the changes

```sh
graphcool deploy
```

- hook into the function logs

```sh
graphcool logs -f like --tail
```

- test the new function by creating a new post

```sh
graphcool playground
```

```graphql
mutation {
  createPost(
    authorId: "<id from above>"
    description: "A great holiday"
    imageUrl: "https://images.unsplash.com/photo-1500531279542-fc8490c8ea4d"
  ) {
    id
  }
}
```

- verify that the like has been created

```graphql
query {
  allGithubUsers {
    likes {
      id
      post {
        id
        description
      }
    }
  }
}
```
