---
alias: eip7ahqu5o
description: GraphQL requests are authenticated using an authentication token. For server-side reqeusts, permanent authentication tokens can be used.
---

# Authentication Tokens

Requests to your [API](!alias-heshoov3ai) are authenticated using **authentication tokens** that are typically [JWT (JSON Web Tokens)](https://jwt.io/).

## Authenticating a request

Authentication tokens need to be passed in the `Authorization` HTTP header:

```plain
Authorization: Bearer <authentication token>
```

If a request to your endpoint contains a valid authentication token, it is considered _authenticated_ with regards to the [permission system](!alias-iegoo0heez). A request with an invalid authentication token in its header is treated as if the token would not be passed at all.

## Token types

**Temporary authentication** tokens are associated with a specific node of any of your model types and have a certain validity duration. They can be issued using the [`graphcool-lib` package](!alias-kaegh4oomu).

**Root tokens** are useful for server-side scripts that need access to your data. You can manage them in your [project settings](!alias-aechi6iequ).

![](./copy-pat.gif?width=400)


<InfoBox type=warning>

Be **very** careful where you use the permanent authentication tokens. Everyone with a permanent authentication token has full read and write access to your data, so you should never include them anywhere client-side, like on a public website or a mobile app.

</InfoBox>
