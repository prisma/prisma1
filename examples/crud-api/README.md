# CRUD API

Basic example demonstrating the generation of the CRUD-style GraphQL API based on your data model.

> You can use the **[read-only demo endpoint](https://graphqlbin.com/LgqiP)** to explore the generated GraphQL API.

## Setup

Download the example or [clone the repo](https://github.com/graphcool/graphcool):

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/crud-api
cd crud-api
```

Install the CLI (if you haven't already):

```sh
npm install -g graphcool
```

Deploy the Graphcool backend and open the GraphQL API endpoint in your browser

```sh
graphcool deploy
# next step: open `https://api.graph.cool/simple/v1/xxx` in your browser
```

When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

## Usage

### Add initial data via `create` mutation

Run the following mutation to add some initial data. (Feel free to change this as you want.)

```graphql
mutation {
  elon: createUser(
    firstName: "Elon"
    lastName: "Musk"
    posts: [{
      title: "Earth Travel"
      description: "Fly to most places on Earth in under 30 mins and anywhere in under 60. Cost per seat should be about the same as full fare economy in an aircraft. Forgot to mention that."
    }, {
      title: "Mars City"
      description: "Opposite of Earth. Dawn and dusk sky are blue on Mars and day sky is red."
    }]
  ) {
    id
  }
  
  tim: createUser(
    firstName: "Tim"
    lastName: "Cook"
    posts: [{
      title: "State Affairs"
      description: "A pleasure to host Secretary of Defense James Mattis at Amazon HQ in Seattle today"
    }]
  ) {
    id
  }
}
```

### Query & filter data

You can run the following query to get all `Post` nodes including the releated `User` node which satisfy the `filter` condition:

```graphql
{
  allPosts(filter: {
    author: {
      firstName: "Elon"
    }
  }) {
    title
    description
    author {
      firstName
      lastName
    }
  }
}
```
