# Permissions with GraphQL Shield

This example demonstrates how to use the simple and declarative [GraphQL Shield](https://github.com/maticzav/graphql-shield) library for authorization by protecting mutations and queries from unauthorized access.

## Get Started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/prismagraphql/prisma/tar.gz/permissions-with-shield | tar -xz --strip=2 prisma-master/examples/permissions-with-shield
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd permissions-with-shield
yarn install
```

### 2. Deploy the Prisma database service

You can deploy locally but for simplicity, use the Prisma free demo server. To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1.  Create a `.env` file and copy over the content of `.env.example` to it.
1.  Run `prisma deploy` to create a new Prisma service.
1.  Replace the value of `PRISMA_ENDPOINT` in your `.env` file with the URL of your service instance. The URL looks similar to this: `https://us1.prisma.sh/your-username-fd2dcf/service-name/stage`
1.  Remember to change the `APP_SECRET` in the `.env` file too.

### 3. Explore the API

To start the server, run the following command

```
yarn dev
```

### Open a Playground

The easiest way to explore this deployed service and play with the API generated from the data model is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

The fastest way to open one is by going to [http://localhost:7200](http://localhost:7200)

### Testing the flow

Run the following mutation to signup:

```graphql
mutation {
  signup(username: "test", email: "test@gmail.com", password: "pass") {
    token
    user {
      email
      username
      createdAt
    }
  }
}
```

You can also login:

```graphql
mutation {
  login(email: "test@gmail.com", password: "pass") {
    token
    user {
      email
      username
      createdAt
    }
  }
}
```

Try quering for data:

```graphql
query {
  posts {
    content
  }
}
```

You will get an `Not Authorized` error. This is because you need to be authorized as a user to read posts. Run the following:

```graphql
mutation AssignRole {
  assignRole(role: ADMIN, assigneeEmail: "test@gmail.com") {
    id
  }
}
```

This will assign the USER role to the just created user.

> `assignRole` mutation is open so you don't lock yourself out of your own system :). This kind of mutation should be restricted to only Admins.

Here is what the [restrictions/permissions](./src/permissions/index.js#L52-L62) look like:

```js
const permissions = shield({
  Query: {
    posts: rules.isUser,
  },
  Mutation: {
    createPost: or(rules.isAdmin, rules.isAuthor, rules.isEditor),
    updatePost: or(rules.isEditor, rules.isPostOwner),
    assignRole: rules.isAdmin,
    createRole: rules.isAdmin,
  },
})
```
