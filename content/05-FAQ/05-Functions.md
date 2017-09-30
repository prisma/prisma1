---
alias: ietheucoh2
description: Frequently asked questions everything about the integration of serverless functions with Graphcool.
---

# Functions

### What is a managed function?

A managed function is a function that's managed and deployed by Graphcool (think of Graphcool as your [FaaS](https://en.wikipedia.org/wiki/Function_as_a_Service) provider for managed functions). If you're using a managed function in your Graphcool project, all you need to do is write the code for it, add it to your [project definition file](!alias-opheidaix3#project-definition) and run `graphcool deploy`. Graphcool will then make sure the function is deployed for you 


### In what environment are managed functions executed with Graphcool? Can I use ES6 for my JS code?

Functions are executed with [Auth0 Extend](https://auth0.com/extend/). You can use ES6 if you write the string `'use-latest'` in the first line of your function. You can read more about the runtime environment [here](!alias-aiw4aimie9).


### How can I validate and transform the payload of a request before data gets written to the database?

There are two ways of doing this:

- Using the [Hook](!alias-pa6guruhaf) function of type `operationBefore`
- Using the [API proxy layer](!alias-thei2kephu#api-proxy-layer)

For simple use cases, such as validating an email address or sanitizing a credit card number, using a hook function is usually a good choice.

### What is the `graphcool-lib` and how can I use it?

The [`graphcool-lib`](https://github.com/graphcool/graphcool-lib) provides a convenient interface to the Graphcool system:

Here's some functionality it offers:

- convenience API for sending queries and mutations to a Graphcool project (based on [`graphql-request`](https://github.com/graphcool/graphql-request))
- generating a temporary authentication token for a node in your database

If you want to use it inside a managed function, you can simply `require` it (similar to [here](https://github.com/graphcool/modules/blob/master/authentication/email-password/code/loggedInUser.js#L1)). In a webhook, you can install it as a dependency with npm. 

<!--

### How can I test my serverless functions?

-->

