---
alias: eip7ahqu5o
description: GraphQL requests are authenticated using an authentication token. For server-side requests, permanent authentication tokens can be used.
---

# Authentication Tokens

Requests to your [API](!alias-abogasd0go) are authenticated using **authentication tokens** that are typically [JWT (JSON Web Tokens)](https://jwt.io/).

There generally are two types of authentication tokens:

- **Temporary authentication tokens**  are associated with a specific node of any of your model types and have a certain validity duration. They can be issued using the [`graphcool-lib` package](!alias-kaegh4oomu).
- **Root tokens** are useful for scripts or serverless functions that need access to your API. You can manage them in your [project settings](!alias-uh8shohxie#other-settings) or using the [CLI](!alias-zboghez5go#graphcool-get-root-token).

## Authenticating a request

Authentication tokens need to be passed in the `Authorization` HTTP header:

```plain
Authorization: Bearer <authentication token>
```

If a request to your endpoint contains a valid authentication token, it is considered _authenticated_ with regards to the [permission system](!alias-iegoo0heez). A request with an invalid authentication token in its header is treated as if the token would not be passed at all.


## Root tokens


<InfoBox type=warning>

Be **very** careful where you use the root tokens. Everyone with a permanent authentication token has full read and write access to your data, so you should never include them anywhere client-side, like on a public website or a mobile app.

</InfoBox>


### Obtaining a root token

There are generally two ways how you can obtain a root token:

- Using the Graphcool Console (only for [non-ejected](!alias-opheidaix3#non-ejected-projects) projects)
- Using the Graphcool CLI and project definition  (only for [ejected](!alias-opheidaix3#ejected-projects) projects)

#### Using the Graphcool Console

For non-ejected projects, you need to create new root tokens in the Graphcool Console. To do so, navigate to your [project settings](!alias-uh8shohxie#other-settings) and then select the **Authentication**-tab. Then click the **add permanent access token**, set the name for the token and confirm.

![](./copy-pat.gif?width=400)

#### Using the Graphcool CLI and project definition

For ejected projects, you need to create new root tokens using the project definition file and the CLI.

To add a new token, simply write the token name under the `rootTokens` section in your `graphcool.yml` file and run `graphcool deploy`. The token will then be added to your project and you can download it using [`graphcool get-root-token --token <token-name>`](!alias-zboghez5go#graphcool-get-root-token).

### Using a root token inside a serverless function

Root tokens are particularly useful inside serverless functions to authenticate access against the API. When implementing a serverless function, a root token is carried inside the event that's passed into the function:

```js
module.exports = function(event) {

  const rootToken = event.context.graphcool.pat

  // ...

}
```

> `pat` stands for _permanent access token_ which is the deprecated term for a root token. 


## Temporary authentication tokens

A temporary authentication always needs to be associated with a particular node (often of type `User` or something similar) in your database. When the token is contained in the `Authorization` header of a request that's executed against your API, it means that the request is made on behalf of the node that it's associated with.

### Generating a temporary authentication token

You need to use [`graphcool-lib`](https://github.com/graphcool/graphcool-lib) in order to generate an authentication token.

Here is how it works:

```js
const fromEvent = require('graphcool-lib').fromEvent

module.exports = function(event) {

  const nodeId = ...

  const graphcool = fromEvent(event)
  const tmpAuthToken = graphcool.generateAuthToken(nodeId, 'User')

  // ...
}
```

> To see fully implemented authentication flows using serverless functions, check our [functions](https://github.com/graphcool-examples/functions/tree/master/authentication/) examples.










