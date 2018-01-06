---
alias: pua7soog4v
description: Authentication
---

TODO S: review content & polish

# Authentication

The GraphQL API of a Graphcool service is typically protected by a secret specified in `graphcool.yml`.

The secret is used to sign a JWT that can then be used in the Authorization header:

```
Authorization: Bearer ${jwt}
```

This is an example JWT:

```json
{
  "exp": 1300819380,
  "service": "my-service@prod",
  "roles": ["admin"]
}
```

## Claims

The JWT must contain different claims:

- **Expiration Time**: `exp`, the expiration time of the token.
- **Service Information**: `service`, the name and stage of the service
- **Roles**: `roles`, a list of roles granting access to the service.

Currently,`admin` is the only supported role. It grants full access.

> In the future we might support more fine grained roles such as `["write:Log", "read:*"]`

## Generating a Signed JWT

Consider this `graphcool.yml`:

```yml
service: my-service

stage: ${env:GRAPHCOOL_STAGE}
cluster: ${env:GRAPHCOOL_CLUSTER}

datamodel: database/datamodel.graphql

secret: ${env:GRAPHCOOL_SECRET}
```

A Node server could create a signed JWT for the stage `prod` of the service `my-service` like this:

```js
var jwt = require('jsonwebtoken');

jwt.sign({
    data: {
      "service": "my-service@" + process.env.GRAPHCOOL_STAGE,
      "roles": ["admin"]
    }
  }, process.env.GRAPHCOOL_SECRET, {
    expiresIn: '1h'
  }
);
```

## JWT verification

The Graphcool Database will verify the following aspect of the JWT:

- It must be signed with a secret configured for the service
- It must contain an `exp` claim with a time value in the future
- It must contain a `service` claim with service and stage matching the current request
- It must contain a `roles` claim that provides access to the current operation
