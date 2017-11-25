---
alias: reob2ohph7
description: Frequently asked questions about Graphcool as a company, the platform itself and other topics.
---

# Other

### In what programming language is Graphcool implemented?

The core of Graphcool is implemented in [Scala](https://www.scala-lang.org/) and based on the [sangria-graphql](https://github.com/sangria-graphql/sangria) library.

The frontend tools (Graphcool [CLI](!alias-zboghez5go) & [Console](!alias-uh8shohxie)) are implemented in [Typescript](https://www.typescriptlang.org/).


### Where can I see performance metrics for my backend?

When using the _hosted version_ of Graphcool, performance metrics will be visible in the [Console](!alias-uh8shohxie).


### What's the difference between the Graphcool CLI and the `graphql-cli`?

The [Graphcool CLI](!alias-zboghez5go) and the [`graphql-cli`](https://github.com/graphcool/graphql-cli) are two independent projects. 

- The **[Graphcool CLI](!alias-zboghez5go)** is used to manage, configure and deploy Graphcool services. So, it's a tool that's _specific to the Graphcool Framework_.
- The **[`graphql-cli`](https://github.com/graphcool/graphql-cli)** on the other hand is a _general purpose_ tool that can be used with _any_ GraphQL project. It helps to manage different GraphQL endpoints, download schemas and is flexible to offer more features using the [plugin system](https://github.com/graphcool/graphql-cli#plugins). You can read more about the ideas behind the `graphql-cli` [here](https://blog.graph.cool/new-tooling-to-improve-your-graphql-workflows-7240c81e1ba3).

Note that you can definitely use both CLIs together as they offer complementary functionality!


<!--

### What if Graphcool gets acquired or shuts down for some reason?

Graphcool is an open-source framework and can always be used as a self-hosted version.

-->
