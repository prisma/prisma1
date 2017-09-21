---
alias: fae2ooth2u
description: Frequently asked questions everything about Graphcool's authentication and authorization systems.
---

# Auth

### How can I define permission rules with Graphcool? Is there something similar to ACL?

With Graphcool, you can specify permission rules per API operation. By default, no API operation is allowed, so you'll explicitly have to create permissions for all of them. Permissions are specfied inside the [project definition file](!alias-opheidaix3#project-definition) under the `permissions` section.

A single permission that's attached to an operation can express either of three things:

- _everyone_ is able to perform that operation
- _only authenticated_ users are allowed to perform that operation
- a _permission query_ defines exactly who is allowed to perform that operation



### How to get information about the currently logged in user?

This depends on the authentication mechanism you're using! If you're using one of Graphcool's predefined [authentication modules](https://github.com/graphcool/modules/tree/master/authentication), you can use the `loggedInUser` query which is going to return information about the currently logged in user if there is one. 


### Does Graphcool support 3rd party logins (e.g. Facebook login, Github login or firebase phone authentication)?

Graphcool offers a very flexible authentication system. Essentially, you can develop any authentication mechanism you like using [Resolver](!alias-su6wu3yoo2) functions.

If you don't have custom requirements for your authentication, you can conveniently use an authentication mechanism that's already implemented as a Graphcool [module](https://github.com/graphcool/modules/tree/master/authentication). 


### What is a root token? How is it different from a temporary authentication token?

There generally are two different kinds of tokens that you can use to authenticate requests:

- [Temporary authentication tokens](!alias-eip7ahqu5o#temporary-authentication-tokens): These are the tokens you are issuing to your users. They are generated using [graphcool-lib](https://github.com/graphcool/graphcool-lib) and each token is always associated with one particular _node_ (usually represting a _user_) in your database. Temporary authentication tokens will expire and your users have to acquire a new one.
- [Root tokens](!alias-eip7ahqu5o#root-tokens): Root tokens are tokens that are valid indefinitely. Each root token is associated with exactly one Graphcool project and can be used to perform any operation in the API of that project.
