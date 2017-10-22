---
alias: bee4oodood
description: An overview of Graphcool's authentication system.
---

# Overview

Graphcool offers a very flexible authentication system that's based on [functions](!alias-aiw4aimie9).

In general, Graphcool allows you to specify that some of your available [API](!alias-abogasd0go) operations require authentication. This effectively that means the HTTP request that's carrying the operation needs to have a valid [authentication token](!alias-eip7ahqu5o) in its `Authorization` header. If that's not the case, the request will fail with a permission error.

To authenticate requests from your users, you need to generate a [node token](!alias-eip7ahqu5o#node-tokens) for them. A node token always needs to be associated with one particular node from your database.

> There are other token kinds than _node tokens_. Read more in the [Authentication Tokens](!alias-eip7ahqu5o) chapter.

Node tokens are generated with [`resolver`](!alias-su6wu3yoo2) functions. If you want to implement authentication, you first need to setup a corresponding `resolver` in your service that returns a node token.

In order to get started quickly with authentication, you can use one of the predefined [templates](https://github.com/graphcool/templates/tree/master/auth/).

<InfoBox>

To learn how to implement authentication with React & Apollo on the frontend, check out the corresponding guides: [email-password-authencation](!alias-cu3jah9ech) and [facebook-login](!alias-yi9jeuwohl).

To only learn how authentication is implemented in the backend, you can refer to [this](ttps://github.com/graphcool/graphcool/tree/master/examples/auth) example.

</InfoBox>
