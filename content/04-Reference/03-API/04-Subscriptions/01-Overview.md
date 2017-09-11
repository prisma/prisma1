---
alias: aip7oojeiv
description: Use subscriptions to receive data updates in realtime. Subscriptions in the GraphQL schema are derived from types and relations.
---

# Subscriptions

*GraphQL subscriptions* allow you to be notified in realtime of changes to your data. This is an example subscription that notifies you whenever a new post is created:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cj03vcl4777hv0180zqjk5a6q
disabled: true
---
subscription newPosts {
  Post(
    filter: {
      mutation_in: [CREATED]
    }
  ) {
    mutation
    node {
      description
      imageUrl
    }
  }
}
---
{
  "data": {
    "Post": {
      "mutation": "CREATED",
      "node": {
        "description": "#bridge",
        "imageUrl": "https://images.unsplash.com/photo-1420768255295-e871cbf6eb81"
      }
    }
  }
}
```

Subscriptions use [a special websocket endpoint](!alias-yahph3foch#project-endpoints).

Here's a list of available subscriptions. To explore them, use the [playground](!alias-oe1ier4iej) inside your project.

* For every [type](!alias-ij2choozae) in your [GraphQL schema](!alias-ahwoh2fohj), a [type subscription query](!alias-ohc0oorahn) is available to listen for changes to nodes of this.
* Currently, connecting or disconnecting nodes in a [relation](!alias-goh5uthoc1) does not trigger any subscription yet. Read more about [available workaround](!alias-riegh2oogh) for this limitation.

You can [combine multiple subscription triggers](!alias-kengor9ei3) into one subscription query to control exactly what events you want to be notified of.
