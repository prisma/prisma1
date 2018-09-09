# Server Side Subscription

This example demonstrates `subscriptions` property available to us in `prisma.yml` that can be used to implement event-driven business logic.

## Get started

### 1. Install the Prisma CLI
The `prisma` cli is the core component of your development workflow. `prisma` should be installed as a global dependency, you can install this with `npm install -g prisma`

### 2. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/prisma/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/server-side-subscriptions
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd server-side-subscriptions
yarn install
```

### 3. Deploy the Prisma database service

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
# Ensure docker is running the server's dependencies
docker-compose up
# Deploy the server
cd prisma
prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a demo server (rather than locally with Docker), please follow [this link](https://www.prisma.io/docs/quickstart/).

</details>

### 4. `subscriptions` property of `prisma.yml`

The `subscriptions` property is used to implement event-based business logic using POST endpoints.

Here is how we have defined the subscription in `prisma.yml` for this example.

```yml
subscriptions:
  welcomeNewUser:
    query: subscription.graphql
    webhook: http://ptsv2.com/t/prisma/post
```

Let us assume that we want to send a welcome email to new user. We will define that subscription as above.

`welcomeNewUser` is the name of the subscription.

`query: subscription.graphql` is the file that contains the subscription query. Note that we are listening to the "CREATED" mutation events.

```graphql
subscription {
 user(where: { mutation_in: [CREATED] }) {
  mutation
  node {
   id
   name
  }
 }
}
```

`webhook: http://ptsv2.com/t/prisma/post` is a mock endpoint consuming this query. On creation of a new `User` Prisma service will call this endpoint with Post data selected in the subscription query.

### 5. Testing the server side subscription

The best way to test this subscription logic is to create a user and observe the mock POST endpoint.

#### Open a Playground

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
yarn playground
```

Or you can open a Playground by navigating to [http://localhost:4466/server-side-subscriptions](http://localhost:4466/server-side-subscriptions) in your browser.

#### Run the following mutation to create a user

```graphql
mutation M {
 createUser(data: { name: "Prisma" }) {
  id
  name
 }
}
```

#### Observe the mock POST endpoint

http://ptsv2.com/t/prisma

You will notice a dump of your request on this page of this shape:

```json
{"data":{"user":{"mutation":"CREATED","node":{"id":"cjjk6a0op000w0982jlzt4m55","name":"Prisma"}}}}

```
