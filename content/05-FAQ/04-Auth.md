---
alias: fae2ooth2u
description: Frequently asked questions everything about Graphcool's authentication and authorization systems.
---

# Auth

### How can I define permission rules with Graphcool? Is there something similar to ACL?

* Graphcool uses a similar approach to authorization as ACL but it's a lot more powerful
* it's similar in that you have to specify permissions for certain operations on given objects
* it's different since these permissions are expressed in the form of GraphQL queries that have access to the whole data graph

### How to get information about the currently logged in user?

* with the user query
* you can parse the GC token on the client side to get the user id

### Does Graphcool support 3rd party logins (e.g. Facebook login, Github login or firebase phone authentication)?

* yes, they can be implemented with schema extensions & serverless functions
* functions for most popular logins predefined?

### What is a Permanent Access Token (PAT)? How is it different from a normal token?

* there are two kinds of tokens:
    * temporary authentication token
    * permanent access token (PAT)
* tmp token can be used for authenticating the users of your app
* PATs are used for admin functionality (shouldn't give them to users) 




