---
alias: aiw4aimie9
description: An overview of how to integrate serverless functions with Graphcool. 
---

# Overview

Graphcool lets you seamlessly integrate serverless functions inside your project to react to events, execute business logic and hook into the GraphQL engine.

There generally are three kinds of functions that you can implement

- **Hooks**: Hooks allow you to synchronously invoke a function right before or after a database operation (commonly used for data transformation and validation)
- **Subscriptions**: Subscriptions allow you to asynchronously invoke a function when an specific event occurs in the GraphQL engine
- **Resolvers**: Resolvers allow you to extend your GraphQL schema with more functionality that goes beyond the auto-generated CRUD capabilities. Resolvers are also commonly used to implement authentication mechanisms.

## Adding a function to your Graphcool project

To add a function to your Graphcool project, you need to add it to the [project configuration file](!alias-asd) under the `functions` section and deploy the project after you made the changes.

When being added, a function needs the following information:

- `handler`: Specifies _how_ the function should be invoked (either as a webhook or a managed function that will be deployed by Graphcool directly)
- `type`: Specifies which of the four available function types (`operationBefore`, `operationAfter`, `subscription`, `resolver`) this function is.
- `query` **[only applies if `type` is `subscription`]**: Points to a file containing the subscription query (which determines the input type of the function).
- `operation ` **[only applies if `type` is `operationBefore` or `operationAfter`]**: Specifies for which operation on which model type this function should be invoked, e.g. `User.update` or `Article.delete`.
- `schema` **[only applies if `type` is `subscription`]**: Defines the necessary extensions on the `Query` or `Mutation` type (and potentially additional types that represent the input or return types of the new field).


