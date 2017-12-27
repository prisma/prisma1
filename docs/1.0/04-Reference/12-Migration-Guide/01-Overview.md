---
alias: koo7shaino
description: Overview
---

Graphcool has evolved quite a bit since it first went live. Here is an overview of its three major phases:

- **Graphcool Backend-as-a-Service**: The initial product of Graphcool was a "BaaS for GraphQL". In that version, an entire backend is configured through the web-based GUI of the Graphcool Console, including the integration of serverless functions and features like authentication and 3rd party integrations.
- **Graphcool Framework**: The Graphcool Framework is the open source version of the BaaS. Everything that was previously done through the Graphcool Console, is now possible with a local developer workflow using the Graphcool CLI. This enables private hosting of Graphcool, better testing, debugging and CI workflows. A major change compared to the BaaS version is that authentication is now done with resolver functions rather than being configured through the UI. Permission queries are not configured through the UI any more but are also written in source files.
- **Graphcool 1.0 - "GraphQL Database"**: Graphcool 1.0 focusses on the core: Its GraphQL API. The biggest difference to previous versions is that Graphcool now requires a web server (such as Express.js) for which it then provides the persistence layer. This web server implements all functionality that was previously performed by serverless functions (except for subscriptions, they're still available in 1.0). In JavaScript, you can use `graphql-yoga` as your Express.js-based GraphQL server.

<InfoBox type=info>

To learn more about the migration from the BaaS to the Graphcool Framework, you can check out the previous [upgrade guide](!alias-aemieb1aev).

</InfoBox>
