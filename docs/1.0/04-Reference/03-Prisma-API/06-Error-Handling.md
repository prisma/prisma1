---
alias: sha0ojohxu
description: Query or mutation errors in GraphQL are handled as part of the query response and contain further information to solve them.
---

# Error Handling

When an error occurs for one of your queries or mutations, the response contains an `errors` property with more information about the error `code`, the error `message` and more.

In general, there are two kind of API errors:

* application errors usually indicate that your request was invalid. Try to
* internal server errors usually means that something unexpected happened in the service. Check your service logs for more information.

## Application Errors

An error returned by the API usually indicates that something is not correct with the requested query or mutation. Try to investigate your input for possible errors related to the error message.

### Troubleshooting

Here is a list of common errors that you might encounter:

#### Authentication

##### Insufficient Permissions or Invalid Token


```json
{
  "errors": [
    {
      "code": 3015,
      "requestId": "api:api:cjc3kda1l000h0179mvzirggl",
      "message": "Your token is invalid. It might have expired or you might be using a token from a different project."
    }
  ]
}
```

Check if the token you provided has not yet expired and is signed with an active [secret](!alias-).

## Internal Server Errors

Consult the service logs for more information on the error. For the local cluster, this can be done using the [graphcool logs](!alias-) command.
