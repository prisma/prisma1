---
alias: opheidaix3
description: An overview of the different components that a Graphcool project consists of.
---

# Overview

Every Graphcool project consists of several components that developers can provide:

- **Database model**: Determines the types that are to be persisted in the database. These types typically represent the entities from the application domain. Read more in the [Database](!alias-viuf8uus7o) chapter.
- **Permission rules**: Define which users are allowed to perform what operations in the API. Read more in the [Authorization](!alias-iegoo0heez) chapter.
- **Serverless functions**: Used to implement data validation and transformation, GraphQL resolvers functions and other business logic. Read more in the [Functions](!alias-aiw4aimie9) chapter.
- **Root tokens**: The authentication tokens that provide access to all API operations. Read more in the [Authentication](!alias-bee4oodood) chapter. 

To manage each of these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML). The file can be altered manually or through dedicated commands of the [CLI](!alias-zboghez5go).
