# subscriptions

## Getting Started

```
yarn
yarn prisma deploy
yarn start
```

## Run Example

First, open the playground:

```sh
yarn playground
```

Then, in one tab, start a subscription for published posts:

```graphql
subscription {
  publications {
    node {
      id
      title
      text
    }
  }
}
```

Open another tab and create a new unpublished post:

```graphql
mutation {
  writePost(
    title: "Secret Post"
    text: "This is my biggest secret"
    isPublished: false
  ) {
    id
  }
}
```

Note that no subscription event is fired in the subscription tab.

Next, switch to another tab and create a new published post: 

```graphql
mutation {
  writePost(
    title: "Public Service Announcement"
    text: "GraphQL is awesome!"
    isPublished: true
  ) {
    id
  }
}
```

Note that a new event has been fired in the subscriptions tab.
