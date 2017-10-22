---
alias: geihakoh4e
description: Graphcool Functions allow you to return customized error messages or JSON objects as a response to a GraphQL mutation.
---

# Error Handling for Functions

## Overview

Functions follow a unified concept for error handling.

> To return an error from a webhook function, it's important **to respond with status code 200**!

## Provoking an error

By returning an object that contains an `error` property, you can populate errors from your functions. If your function is called as part of the [request pipeline](!alias-pa6guruhaf), further processing of the incoming request is aborted.

### Returning error messages

To return an error message, returns an object with the key `error` that is a String:

```js
export default event => {
  return {
    error: "Invalid email!"
  }
}
```

Then the response to the request that resulted in that error will include the supplied string and a generic error code is included as GraphQL error:

```json
{
  "data": null,
  "errors": [
    {
      "code": 5001,
      "message": "Function execution error: 'Invalid email!'",
      "functionError": "Invalid email!"
    }
  ]
}
```

### Returning error objects

For debugging or user-facing errors or other situations where more custom behaviour is needed you can return a JSON object instead of a string as the `error` property in your function:

```js
export default event => {
  return {
    error: {
      code: 42,
      message: "Invalid email!",
      debugMessage: "We should add validation in the frontend as well!",
      userFacingMessage: "Please supply a valid email address!"
    }
  }
}
```

Then the response to the request that resulted in that error will include the `error` JSON object as well as a generic error code as a GraphQL error:

```json
{
  "data": null,
  "errors": [
    {
      "code": 5002,
      "message": "Function execution error",
      "functionError": {
        "code": 42,
        "message": "Invalid email!",
        "debugMessage": "We should add validation in the frontend as well!",
        "userFacingMessage": "Please supply a valid email address!"
      }
    }
  ]
}
```

## Unexpected errors

In case the function failed in an unexpected way, we return a generic error message and an `executionId`. Have a look at your function to fix this problem or reach out to us if you have further questions.

```json
{
  "data": null,
  "errors": [
    {
      "code": 5000,
      "message": "A function returned an unhandled error. Please check the logs for executionId 'cj27leckw31to0153whdva5b2'"
    }
  ]
}
```
