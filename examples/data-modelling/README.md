# Data Modelling

This example demonstrates various [data modelling](<https://www.prisma.io/docs/reference/service-configuration/data-model/data-modelling-(sdl)-eiroozae8u>) features of Prisma.

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/data-modelling | tar -xz --strip=2 prisma-master/examples/data-modelling
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd data-modelling
yarn install
```

### 2. Deploy the Prisma database service

You can now [deploy](https://www.prisma.io/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1.  Remove the `cluster` property from `prisma.yml`
1.  Run `yarn prisma deploy`
1.  When prompted by the CLI, select a demo cluster (e.g. `prisma-eu1` or `prisma-us1`)
1.  Replace the [`endpoint`](./src/index.js#L23) in `index.js` with the HTTP endpoint that was printed after the previous command

</details>

### 3. Explore the generated datamodel

This example seeds some data into the database for us to explore some queries and features of the data model. Please take a look at `seed.graphql` for reference. Feel free to add/remove more data via mutations.

The easiest way to explore this deployed service and play with the API generated from the data model is by using the [GraphQL Playground](https://github.com/graphcool/graphql-playground).

### Open a Playground

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
yarn playground
```

Or you can open a Playground by navigating to [http://localhost:4466/data-modelling](http://localhost:4466/data-modelling) in your browser.

This example illustrates a few important concepts when working with your data model:

### There are three types Tweet, User and Location

These types are mapped to tables in the database. We can query any of these types (say Tweet) in the following ways

##### Get one tweet by its id (or any other field with @unique directive)

```graphql
query Tweet {
  tweet(where: { id: "<tweet-id>" }) {
    id
    text
  }
}
```

##### Get multiple tweets with pagination

```graphql
query Tweets {
  tweets(first: 10, skip: 20) {
    id
    text
  }
}
```

##### Get multiple tweets sorted by their `createdAt` field value

```graphql
query Tweets {
  tweets(orderBy: createdAt_DESC) {
    id
    text
  }
}
```

##### Get multiple tweets with conditions like tweet text should contain `"GraphQL"` string and it should not be from the user `"Graphcool"`

```graphql
query Tweets {
  tweets(
    where: {
      AND: [{ text_contains: "GraphQL" }, { owner: { name_not: "Graphcool" } }]
    }
  ) {
    id
    text
  }
}
```

### There is a bidirectional relation between User and Tweet

##### Get user for a tweet

```graphql
query Tweets {
  tweets {
    id
    text
    owner {
      id
      name
    }
  }
}
```

##### Get tweets for a user and sort the tweets by their `createdAt` field.

```graphql
query UserTweets {
  user(where: { handle: "graphcool" }) {
    id
    name
    tweets(orderBy: createdAt_DESC) {
      id
      text
    }
  }
}
```

### GraphQL Directives

##### Unique

`@unique` - The `@unique` directive marks a scalar field as unique. Unique fields will have a unique index applied in the underlying database. In this data model, amongst other fields, the field `handle` on the type `User` is marked with `@unique` directive. Which is why we are able to query the user using their `handle`.

```graphql
query UserTweets {
  user(where: { handle: "graphcool" }) {
    id
    name
  }
}
```

##### Relation

[`@relation`](<https://www.prisma.io/docs/reference/service-configuration/data-model/data-modelling-(sdl)-eiroozae8u#the-@relation-directive>) - The directive `@relation(name: String, onDelete: SET_NULL)` can be attached to a relation field.

In this data model, we have added the following directive on tweets field in `User` type `@relation(name: "UserTweets", onDelete: CASCADE)`

The deletion behaviour in this example is as follows:

* When a `User` node gets deleted, all its related `Tweet` nodes will be deleted as well.

* When a `Tweet` node gets deleted, it will simply be removed from the tweets list on the related `User` node.

Note that `deleteMany` does not activate a cascade delete yet. This feature is being tracked [here](https://github.com/prismagraphql/prisma/issues/1936).

##### Default

`@default` - The directive `@default(value: String!)` sets a default value for a scalar field. Note that the value argument is of type String for all scalar fields (even if the fields themselves are not strings). In this data model, we have provided a `@default` directive on the `name` field of `User` type as `@default(value: "")`. This will set the default value to `""` when it is not provided by a mutation.
