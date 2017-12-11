---
alias: aix9viidei 
description: An overview of how authentication works in Graphcool.
---

# Authentication

## Token-based Authentication

Graphcool uses a token-based authentication system. This means that in order to be considered logged in (or *authenticated*), a user needs to attach a valid token to a request that's made on their behalf.

> With this approach, the API is stateless and there is no need to manage user sessions on the server-side. Instead, the required information is encoded in the authentication token so that the server can retrieve everything it needs directly from the token, rather than storing session-related data itself.

There generally are two kinds of tokens:

* **Temporary authentication tokens**: One temporary authentication token belong to a concrete user and serves to authenticate the requests of that user. As the name states, the token will become invalid after a certain period. When implementing authentication with serverless functions, theses tokens are issued using `graphcool-lib` (you can find an example [here](https://github.com/graphcool-examples/functions/blob/8ca9577233c1831832a97ffba336666d88549034/authentication/facebook-authentication/facebook-authentication.js#L53)).
* **Permanent authentication tokens (PAT)**: A permanent authentication token can be generated in the Graphcool console (in the *Project Settings*) or using the CLI. It provides full read and write access to the project that it's associated with and thus should only be distributed among project administrators.

Note that authentication in Graphcool is not bound to a specific type. This means that you can generate a temporary authentication token for *any* node in the Graphcool system, no matter what type it has, and attach it to subsequent requests.


## Implementing Login Mechanisms with Serverless Functions

With Graphcool, you are in full control over how you want to offer authentication to your users. The approach that's taken is based on *serverless functions* where you can implement very customized login approaches but likewise take advantage of already [existing authentication mechanisms](https://github.com/graphcool-examples/functions/tree/master/authentication) and don't have the overhead of implementing authentication themselves. 

In general, there are (at least) two steps that are required to implement an authentication mechanism:

1. Extend the `Mutation` type using a Schema Extension.
2. Write the  corresponding serverless function for the Schema Extension in which you generate a temporary access token from `graphcool-lib` and return it to the client. The client can then use the token to authenticate following requests.

Depending on your exact approach, there will be additional steps and likely additional Schema Extensions that you need to set up. For example, to offer a standard [email-and-password-based login mechanism](https://github.com/graphcool-examples/functions/blob/master/authentication/email-user-management/functions/update-email/schema-extension.graphql), you also need to take care of registration, password reset and updating email address and password. All of these features need to be added by means of a Schema Extension and the corresponding serverless function.
