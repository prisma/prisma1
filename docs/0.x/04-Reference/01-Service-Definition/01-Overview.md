---
alias: opheidaix3
description: An overview of the different components that a Graphcool service consists of.
---

# Overview

Every Graphcool service consists of several components that developers can provide:

- **Type definitions (including database model)**: Defines types for the [GraphQL schema](http://graphql.org/learn/schema/). There are two kinds of types:
  - **Model types**: Determines the types that are to be persisted in the database. These types need to be annotated with the `@model`-directive and typically represent entities from the application domain. Read more in the [Database](!alias-viuf8uus7o) chapter.
  - **Transient types**: These types are not persisted in the database. They typically represent _input_ or _return_ types for certain API operations.
- **Permission rules**: Define which users are allowed to perform what operations in the API. Read more in the [Authorization](!alias-iegoo0heez) chapter.
- **Functions**: Used to implement data validation and transformation, GraphQL resolvers functions and other business logic. Read more in the [Functions](!alias-aiw4aimie9) chapter.
- **Root tokens**: The authentication tokens that provide access to _all_ API operations. Read more in the [Authentication](!alias-bee4oodood) chapter. 

To manage these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML). The file can be altered manually or through dedicated commands of the [CLI](!alias-zboghez5go).
