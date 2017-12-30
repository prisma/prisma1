---
alias: sha0ojohxu
description: Query or mutation errors in GraphQL are handled as part of the query response, where you can find further information to solve them.
---

# Error Handlings

When an error occurs for one of your queries or mutations, the `data` field of the query response will usually be `null` and the error `code`, the error `message` and further information will be included in the `errors` field of the response JSON.

## API Errors

An error returned by the API usually indicates that something is not correct with the query or mutation you are trying to send from your client application to the API endpoint.

Try to investigate your input for possible errors related to the error message.

Maybe the syntax of a request is not correct, or you forgot to include a required query argument?
Another possibility is that the supplied data could not be found on our servers, so check any provided id if it points to an existing node.

Here is an overview of possible errors that you might encounter:

<!-- TODO
- new error codes
-->

> For example, when you try to update a post but specify a non-existing id:

```graphql
mutation {
  updatePost(
    id: "wrong-id"
    title: "My new Title"
  ) {
    id
  }
}
```

## Internal Server Errors

_Internal server errors_ indicate that something went wrong with our service - whoops! Please contact us from the Console (https://console.graph.cool) or [via email](mailto:support@graph.cool) and include your request ID so we can help you out and fix the issue.
