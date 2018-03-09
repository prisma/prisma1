---
alias: apohpae9ju 
description: An overview of Prisma.
---

# What is Prisma

<InfoBox>

To understand what Prisma is and how it works, it is crucial that you have a solid understanding of GraphQL and how it is implemented on the server-side. If you're not familiar with the concept of a **GraphQL schema**, its **root types** and the role of **resolver functions**, please make sure to read the following articles:

- [GraphQL Server Basics: The Schema](https://blog.graph.cool/ac5e2950214e)
- [How to build a GraphQL Server](https://blog.graph.cool/6da86f346e68)

Note that if you have used Graphcool to manage your GraphQL server in the past, you will greatly benefit from reading through above articles to get a clearer understanding of Prisma and the value it provides.

</InfoBox>

Prisma is a _database abstraction layer_ that turns your databases into GraphQL APIs with CRUD operations and realtime capabilities. It is the glue between your database and GraphQL server.

[GraphQL](http://graphql.org/) is a simple yet incredibly powerful abstraction for working with data. Prisma is the first step towards making GraphQL a universal query language by abstracting away the complexity of SQL and other database APIs.

![](https://imgur.com/g41vZah.png)

## Prisma makes it easy to build GraphQL servers

With modern tools like GraphQL bindings and schema stitching, it becomes possible to implement a GraphQL server by simply composing already existing GraphQL APIs. When implementing the resolvers for your GraphQL schema, you're not writing complicated SQL any more but instead delegate the execution of incoming queries to the underlying Prisma engine.

With Prisma, building GraphQL servers becomes as easy as putting together Lego bricks.

## Databases are complex - Prisma makes them simple

Databases are one of the most difficult components to get right in backend development. Ensuring performance and scaling the backend infrastructure requires notable technical expertise and is prone to becoming a major time sink in the development process. Prisma drastically simplifies that part by offering a simple abstraction for your database while retaining query performance.

## A new level of developer experience for backend engineers

Building GraphQL servers with Prisma is a new level of developer experience. Thanks to GraphQL's strongly-typed API layer, you can benefit from auto-completion in your editor as well as build-time validation and error checks for API requests and response payloads.