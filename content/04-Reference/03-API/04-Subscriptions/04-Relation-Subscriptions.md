---
alias: riegh2oogh
description: Subscribe to relation changes by touching nodes.
---

# Relation Subscriptions

Currently, subscriptions for relation updates are only available with a workaround using [update subscriptions](!alias-ohmeta3pi4).

## Subscribing to relation changes

You can force a notification changes by _touching_ nodes. Add a `dummy: String` field to the type in question and update this field for the node whose relation status just changed.

```graphql
mutation updatePost {
  updatePost(
    id: "some-id"
    dummy: "dummy" # do a dummy change to trigger update subscription
  )
}
```

If you're interested in a direct relation trigger for subscriptions, [please join the discussion at GitHub](https://github.com/graphcool/feature-requests/issues/146).
