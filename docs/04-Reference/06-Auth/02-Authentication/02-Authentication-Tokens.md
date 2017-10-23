---
alias: eip7ahqu5o
description: GraphQL requests are authenticated using an authentication token. For server-side requests, permanent authentication tokens can be used.
---

# Authentication Tokens

## Overview

Requests to your [CRUD API](!alias-abogasd0go) (or the System API) are authenticated using **authentication tokens** that are attached to the `Authorization` header of the request. Graphcool uses [JWT](https://jwt.io/) (JSON Web Tokens) as a token format.

Grpahcool offers several types of authentication tokens:

- **Node tokens**: A node token is associated with a specific node in your database (no matter which [type](!alias-eiroozae8u#model-types)) and has a certain validity duration (the default is 30 days). They can be issued using the [`generateNodeToken(nodeId: string, typeName: string, payload?: ScalarObject)`](https://github.com/graphcool/graphcool-lib/blob/master/src/index.ts#L58) function in [`graphcool-lib`](!alias-kaegh4oomu) or by directly calling the `generateNodeToken`-mutation of the Graphcool [System API](https://api.graph.cool/system) for which different validity durations can be specified.
- **Root tokens** (previously called permanent access tokens (PATs)): A root token grants full access to all API operations. There are two kinds of root tokens:
  - **Regular**: Useful for scripts or other applications that need access to your API. You can manage them in your [service settings](!alias-uh8shohxie#other-settings) or using the [CLI](!alias-zboghez5go#graphcool-get-root-token).
  - **Temporary**: Every [function](!alias-aiw4aimie9) receives a temporary root token as an input argument so you are able to run queries and mutation against your API without additional authentication overhead. 
- **Platform tokens**: A platform token authenticates requests against the Graphcool [System API](https://api.graph.cool/system). You can obtain it by logging in to the Graphcool platform. After a successful login, the token will be stored in the global [`.graphcoolrc`](!alias-zoug8seen4) in your home directory and used by the [CLI](!alias-zboghez5go) for any platform requests that require authentication.
- **Cluster secrets**: When deploying a Graphcool instance with Docker, a cluster secret (also sometimes called _master token_) is required to manage the cluster.

## Authenticating a request

Authentication tokens need to be passed in the `Authorization` field of the HTTP header:

```plain
Authorization: Bearer <authentication token>
```

Here is a sample [CURL](https://curl.haxx.se/) request that carries the `Authorization` field:

```sh
curl 'https://api.graph.cool/simple/v1/cj8sj0xes01o8017095vw1tw0'\
   -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE1MTEwMDEzNTIsImlhdCI6MTUwODQwOTM1MiwicHJvamVjdElkIjoiY2o4c2oweGVzMDFvODAxNzA5NXZ3MXR3MCIsInVzZXJJZCI6ImNqOHliemQ5ZjFmajUwMTMwaHh4ZTZreHUiLCJtb2RlbE5hbWUiOiJVc2VyIn0.h36n5cPk4glRptEO882Ngf-0u_OWPquGZMW0F94j_8M'\
   -H 'Content-Type: application/json'\
   -d '{"query":"{ loggedInUser { id } }"}'
``` 

If a request to your endpoint contains a valid authentication token, it is considered _authenticated_ with regards to the [permission system](!alias-iegoo0heez). A request with an invalid authentication token in its header is treated as if the token would not be passed at all.

## Node tokens

A node token always needs to be associated with a particular node (often of type `User` or something similar) in your database. When the token is contained in the `Authorization` header of a request that is executed against your service's API, it means that the request is made _on behalf_ of the node that it is associated with.

### Generating a node token with `graphcool-lib`


You can use the [`generateAuthToken`](https://github.com/graphcool/graphcool-lib#generatenodetokennodeid-modelname) function in [`graphcool-lib`](https://github.com/graphcool/graphcool-lib) to generate a new node token.

Here is how it works:

```js
const fromEvent = require('graphcool-lib').fromEvent

module.exports = event => {

  const nodeId = ... // e.g. 

  const graphcool = fromEvent(event)
  const validityDuration = 864000 // 864000 seconds are 10 days

  // validityDuration is an optional argument, the default is 30 days 
  const node = graphcool.generateNodeToken(nodeId, 'User', validityDuration)

  // ...
}
```

> To see fully implemented authentication flows, you can review the predefined [authentication templates](https://github.com/graphcool/templates/tree/master/auth).

### Generating a node token with the Graphcool System API

Another option to generate node tokens is by directly talking to the Graphcool [System API](https://api.graph.cool/system).

Here is a sample mutation to generate a node token for a `User` model type:

```graphql
mutation GenerateRootToken($rootToken: String!, $serviceId: ID!, $nodeId: ID!) {
  generateNodeToken(input: {
    rootToken: $rootToken,
    serviceId: $serviceId,
    nodeId: $nodeId,
    modelName: "User",
    expirationInSeconds: 864000, # 864000 seconds are 10 days (default is 30)
    clientMutationId: ""
  }) {
    token
  }
}
```

## Root tokens

<InfoBox type=warning>

Be **very** careful where you use the root tokens. Everyone with a root token has **full read and write access to your data**, so you should never include them anywhere client-side, like on a public website or a mobile app.

</InfoBox>


### Creating a _regular_ root token

_Regular_ root tokens are created with the Graphcool [CLI](!alias-aiteerae6l#graphcool-root-token) and the [service definition](!alias-foatho8aip#roottokens).

To create a new root token, you need to add a new entry to the `rootTokens` list in your [`graphcool.yml`](!alias-foatho8aip). The entry defines the _name_ of the root token. Here is an example where a project has two root tokens, called `myToken1` and `myToken2`:

```yml
rootTokens:
  - myToken1
  - myToken2
```

After modifying the `rootTokens` list, you need to apply the changes by invoking the [`graphcool deploy`](!alias-aiteerae6l#graphcool-deploy) command. 

> Note: In the case of [legacy](!alias-aemieb1aev) projects, root tokens are managed through the Graphcool Console - not the CLI. To create a new root token in the Console, navigate to your [project settings](!alias-uh8shohxie#other-settings) and select the **Authentication**-tab. Then click the **add permanent access token**, set the name for the token and confirm.

### Obtaining a _regular_ root token

When your service is deployed, the corresponding [target](!alias-zoug8seen4#managing-targets-in-a-local-graphcoolrc) will be associated with the root tokens defined in [`graphcool.yml`](!alias-foatho8aip).

You can obtain the value of the root token using the [`graphcool root-token`](!alias-aiteerae6l#graphcool-root-token) command:

```sh
graphcool root-token --token myToken1
```

If you don't pass the `--token` option to the command, it will simply print the names of all the root tokens associated with this target.


### Using a _temporary_ root token inside functions

Root tokens are particularly useful inside [functions](!alias-aiw4aimie9) to authenticate requests against the service's API. The input `event` for functions carries a _temporary_ root token that you can access as follows:

```js
module.exports = function(event) {

  const rootToken = event.context.graphcool.pat

  // ...

}
```

> `pat` stands for _permanent access token_ which is the deprecated term for a root token. 

When using the `fromEvent` function from [`graphcool-lib`](https://github.com/graphcool/graphcool-lib) to create a GraphQL client for your service's API, the root token will automatically be set in the `Authorization` header. 










