---
alias: opheidaix3
description: Graphcool uses a dedicated YAML format for configuration.
---

# Project Configuration (YAML)

Every Graphcool project consists of several different pieces of information that developers can provide:

- Database model: Determines the types that are to be persisted in the database. These types typically represent the entities from the application domain.
- Permission rules: Define which users are allowed to perform what operations in the API. 
- Serverless functions: Used to implement custom business logic

To manage each of these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML).

![](http://imgur.com/tMQKBfg.png)

![](http://imgur.com/hgABTZY.png)