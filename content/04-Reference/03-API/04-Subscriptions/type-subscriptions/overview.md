---
alias: ohc0oorahn
path: /docs/reference/simple-api/type-subscriptions
layout: REFERENCE
shorttitle: Type Subscriptions
description: For every available type mutation in your GraphQL schema, certain subscriptions are automatically generated.
simple_relay_twin:
tags:
  - simple-api
  - subscriptions
related:
  further:
  more:
---

# Type Subscriptions in the Simple API

For every available [type](!alias-ij2choozae) mutation in your [GraphQL schema](!alias-ahwoh2fohj), certain subscriptions are automatically generated.

For example, if your schema contains a `Post` type:

```graphql
type Post {
  id: ID!
  title: String!
  description: String
}
```

a `Post` subscription is available that you can use to be notified whenever certain nodes are [created](!alias-oe8oqu8eis), [updated](!alias-ohmeta3pi4) or [deleted](!alias-bag3ouh2ii).
