# Sending queries and mutations with `graphcool-lib`

This example demonstrates how to **send queries and mutations against your service's API using [`graphcool-lib`](https://github.com/graphcool/graphcool-lib)** from inside a Graphcool [function](https://graph.cool/docs/reference/functions/overview-aiw4aimie9).

## Get started

### 1. Download the example

Clone the full [graphcool](https://github.com/graphcool/graphcool) repository and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/graphcool/tar.gz/master | tar -xz --strip=2 graphcool-master/examples/graphcool-lib
cd graphcool-lib
```

Next, you need to create your GraphQL server using the [Graphcool CLI](https://graph.cool/docs/reference/graphcool-cli/overview-zboghez5go).

### 2. Install the Graphcool CLI

If you haven't already, go ahead and install the CLI first:

```sh
npm install -g graphcool
```

### 3. Create the GraphQL server

The next step will be to [deploy](https://graph.cool/docs/reference/graphcool-cli/commands-aiteerae6l#graphcool-deploy) the Graphcool service that's defined in this directory. 

To deploy the service and actually create your GraphQL server, invoke the following command:

```sh
graphcool deploy
```


When prompted which cluster you'd like to deploy, chose any of `Backend-as-a-Service`-options (`shared-eu-west-1`, `shared-ap-northeast-1` or `shared-us-west-2`) rather than `local`. 

> Note: Whenever you make changes to files in this directory, you need to invoke `graphcool deploy` again to make sure your changes get applied to the "remote" service.

## Testing the service

### Open a Playground

The easiest way to test the deployed service is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

You can open a Playground with the following command:

```sh
graphcool playground
```

### Create `User` with three `Post`s

In the Playground, send the following mutation:

```graphql
mutation {
  createUser(posts: [{
    imageURL: "https://dog.ceo/api/img/terrier-dandie/n02096437_806.jpg"
  }, {
    imageURL: "https://dog.ceo/api/img/mexicanhairless/n02113978_773.jpg"
  }, {
    imageURL: "https://dog.ceo/api/img/briard/n02105251_8951.jpg"
  }]) {
    id
  }
}
``` 

This creates a new `User` node along with three `Post` nodes that are connected via the `UserPosts` relation. Save the `id` of the newly created `User` node that's returned by the server.

### Test the `opeationBefore` hook function

Due to the [constraint](./src/checkPostCount.js#L27) implemented in [`checkPostCount.js`](./src/checkPostCount.js), a `User` can at most be associated with 3 `Post` nodes. The new `User` has exactly three `Post` nodes, so any subsequent `createPost`-mutations for that `User` node are expected to fail. 

Send the following mutation through the Playground. Don't forget to replace the placeholder `__AUTHOR_ID__` with the `id` that ewas returned in the `createUser` mutation in the previous step:

```graphql
mutation {
  createPost(authorId: "__AUTHOR_ID__", imageURL: "https://dog.ceo/api/img/hound-Ibizan/n02091244_569.jpg") {
    id
  }
}
```

### Test the custom `postRandomDogImage` mutation

The `resolver` function defined and implemented in [`postRandomDogImage.graphql`](./src/postRandomDogImage.graphql) and [`postRandomDogImage.js`](./src/postRandomDogImage.js) fetches a random dog image from a [public dog image API](https://dog.ceo/dog-api/) and stores it in the database.

You can invoke it as follows:

```graphql
mutation {
  postRandomDogImage(authorId: "__AUTHOR_ID__") {
    url
  }
}
```








