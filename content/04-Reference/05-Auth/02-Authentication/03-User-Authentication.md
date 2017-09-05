---
alias: geekae9gah
path: /docs/reference/auth/authentication
layout: REFERENCE
description: Authentication allows you to manage users in your GraphQL backend. Use authentication providers like Auth0 and Facebook.
tags:
  - authentication
related:
  further:
    - seimeish6e
    - iegoo0heez
  more:
---

# Authentication

## Signup

To signup a user, you can use the `createUser` mutation. For a signed up user, you can generate an according authentication token using [`graphcool-lib`](!alias-kaegh4oomu) on the server-side.

To allow the creation of new user accounts to everyone, you need to setup your [permissions](!alias-iegoo0heez) so that `EVERYONE` can call the `createUser` mutation. Make sure to only return data in the `createUser` [mutation payload](!alias-gahth9quoo) that `EVERYONE` can access as well.

## User Login

The [API](!alias-heshoov3ai) is stateless - that means that users are not considered logged in or logged out per se. In fact, only requests can be considered authenticated or not. In an application, a user can therefore be seen as logged in if she is in possession of [a valid authentication token](!alias-eip7ahqu5o) associated with her account.

You can use the [`user` query](!alias-gieh7iw2ru) to validate a given token. For valid tokens, it returns user data, while invalid tokens return no data.
