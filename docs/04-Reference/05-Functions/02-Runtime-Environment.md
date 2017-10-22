---
alias: lah7reidae
description: Graphcool Functions can run either inline or using a remote webhook.
---

# Runtime Environment

## Function `context` input argument

All functions will be able to access meta information through the `context` field of the function's input `event`.

This `context` has the following structure:

```
{
  # authentication info
  auth: {
    typeName # if request is authenticated, this is the name of the corresponding type, e.g. `User`
    nodeId # if request is authenticated, this is the `id` of the corresponding node
    token # if request is authenticated, this is the valid authentication token
  },
  # project info
  graphcool: {
    projectId # this will be deprecated shortly, use serviceId instead
    serviceId
    alias
    pat # this will be deprecated shortly, use rootToken instead
    rootToken
  }
}
```

## Runtime environment for managed functions

**Managed functions** are deployed to [AWS Lambda](https://aws.amazon.com/lambda/).

<InfoBox type=warning>

Notice that the **maximum execution time of a managed function is 15 seconds**.

</InfoBox>

### Node runtime

Here is a [list](http://docs.aws.amazon.com/lambda/latest/dg/programming-model.html) of supported node versions:

- Node.js runtime v6.10 (runtime = nodejs6.10)
- Node.js runtime v4.3 (runtime = nodejs4.3)


### Environment variables

You can provide environment variables to your functions by adding them to the function definition in [`graphcool.yml`](!alias-foatho8aip) under the `environment` property:

```yml
functions:
  # Resolver for authentication
  authenticateCustomer:
    handler:
      # Specify a managed function as a handler
      code:
        src: ./src/authenticate.js
        # Define environment variables to be used in function
        environment:
          SERVICE_TOKEN: aequeitahqu0iu8fae5phoh1joquiegohc9rae3ejahreeciecooz7yoowuwaph7
          STAGE: prod
    type: resolver
```

Inside the function, you can access it them as follows:

```js
const serviceToken = provess.env['SERVICE_TOKEN']
```

> See an example for using environment variables inside functions [here](https://github.com/graphcool/graphcool/tree/master/examples/env-variables-in-functions).

## Webhooks

Functions can also be deployed as **webhooks** using Function-as-a-Service (FaaS) providers such as [Serverless](https://serverless.com/) and [AWS Lambda](https://aws.amazon.com/lambda/), [Google Cloud Functions](https://cloud.google.com/functions/), [Microsoft Azure Functions](https://azure.microsoft.com/) or by hosting the function yourself. Then you need to provide the webhook URL.

