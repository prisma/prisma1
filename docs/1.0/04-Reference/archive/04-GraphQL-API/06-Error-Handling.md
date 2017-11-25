---
alias: aecou7haj9
description: Query or mutation errors in GraphQL are handled as part of the query response, where you can find further information to solve them.
---

# Error management

When an error occurs for one of your queries or mutations, the `data` field of the query response will usually be `null` and the error `code`, the error `message` and further information will be included in the `errors` field of the response JSON.

## API Errors

An error returned by the API usually indicates that something is not correct with the query or mutation you are trying to send from your client application to the API endpoint.

Try to investigate your input for possible errors related to the error message.

Maybe the syntax of a request is not correct, or you forgot to include a required query argument?
Another possibility is that the supplied data could not be found on our servers, so check any provided id if it points to an existing node.

Here is an overview of possible errors that you might encounter:

**Code 3000: GraphQLArgumentsException**

**Code 3001: IdIsInvalid**

**Code 3002: DataItemDoesNotExist**

**Code 3003: IdIsMissing**

**Code 3004: DataItemAlreadyExists**

**Code 3005: ExtraArguments**

**Code 3006: InvalidValue**

**Code 3007: ValueTooLong**

**Code 3008: InsufficientPermissions**

**Code 3009: RelationAlreadyFull**

**Code 3010: UniqueConstraintViolation**

**Code 3011: NodeDoesNotExist**

**Code 3012: ItemAlreadyInRelation**

**Code 3013: NodeNotFoundError**

**Code 3014: InvalidConnectionArguments**

**Code 3015: InvalidToken**

**Code 3016: ProjectNotFound**

**Code 3018: InvalidSigninData**

**Code 3019: ReadonlyField**

**Code 3020: FieldCannotBeNull**

**Code 3021: CannotCreateUserWhenSignedIn**

**Code 3022: CannotSignInCredentialsInvalid**

**Code 3023: CannotSignUpUserWithCredentialsExist**

**Code 3024: VariablesParsingError**

**Code 3025: Auth0IdTokenIsInvalid**

**Code 3026: InvalidFirstArgument**

**Code 3027: InvalidLastArgument**

**Code 3028: InvalidSkipArgument**

**Code 3031: GenericServerlessFunctionError**

**Code 3032: RelationIsRequired**

**Code 3033: FilterCannotBeNullOnToManyField**

**Code 3034: UnhandledFunctionError**

> For example, when you try to update a post but specify a non-existing id:

```graphql
---
endpoint: https://api.graph.cool/simple/v1/cixne4sn40c7m0122h8fabni1
disabled: true
---
mutation {
  updatePost(
    id: "wrong-id"
    title: "My new Title"
  ) {
    id
  }
}
---
{
  "data": {
    "updatePost": null
  },
  "errors": [
    {
      "locations": [
        {
          "line": 21,
          "column": 3
        }
      ],
      "path": [
        "updatePost"
      ],
      "code": 3002,
      "message": "'Post' has no item with id 'wrong-id'",
      "requestId": "cixnho5zctixo0143pjqwaqry"
    }
  ]
}
```

**Internal Server Errors**

*Internal server errors* indicate that something went wrong with our service - whoops! Please contact us from the Console (https://console.graph.cool) or [via email](mailto:support@graph.cool) and include your Request ID so we can help you out and fix the issue.

## Function Errors

Graphcool Functions offer their own [error handling](!alias-geihakoh4e) concept that allows you to return user-friendly error messages from within a function.
