---
alias: chew3ohd6u 
description: Overview of the Graphcool API.
---

# API Capabilities

## CRUD Operations

The foundation for the GraphQL API are CRUD operations for your model types. For each model type you specify, Graphcool will add the corresponding fields to the schema's root types and generate any additional API types such as filters or special input types.

![](https://imgur.com/MoInfZM.png)


## Schema Extensions

Graphcool further allows you to *extend *the root types in your project's GraphQL schema and thus write custom queries and mutations. Each field that you're adding to the `Query` or `Mutation` type needs to backed by a serverless function that implements the expected functionality for that field (i.e. it needs to perform the actual query or mutation). You can read more about schema extensions in the [functions](http://#) chapter.


## Realtime Subscriptions API

As mentioned before, subscriptions allow developers to implement event-based realtime functionality in their apps.

The Graphcool Subscription API can be used for the following three kinds of events:

* *created*-operations
* *updated*-operations
* *deleted*-operations

Depending on which of these events happened, the payload that the server pushes to the client differs.

Let's consider a simple example scenario where a client is generally interested in all operations on the `Post` type, meaning it wants to receive data whenever a `Post` node is created, updated or deleted. 


> When talking about a `Post` node, we're referring to a single instance, i.e. a database record of the `Post` type.


If that's the case, it needs to send a *subscription query* to the server that tells the server what events the client wants to get notified about and what data it wants to receive:

```graphql
subscription {
  Post {
    operation
    node {
      title
      content
    }
  }
}
```

Let's understand all parts of this subscription query:

* `Post` is the root field of the query, specifying the client wants to be informed when an operation happens on a `Post` 
* The `operation` field represents the kind of operation that was performed, it'll contain either of the enum values: `CREATED`, `UPDATED` or `DELETED`
* The `node` field represents the `Post` node on which the operation was performed
* `title` and `content` are simply carry the values of the `Post` node on which the operation was performed

For *created*- and *updated*-operations, that `node` will represent the `Post` node that was either just created or updated. However, these semantics don't apply for *deleted*-operations. In fact, when a `Post` node is deleted, `node` will always be `null`!

In the event of a *deleted*-operation, a client might still want to receive the values for `title` and `content` for the `Post` node that was just deleted. In that case, the `previousValues` can be added to the selection set:

```graphql
subscription {
  Post {
    operation
    node {
      title
      content
    }
    previousValues {
      title
      content
    }
  }
}
```

Let's make it more concrete a and consider the following three operations:

1. A new `Post` node is created with the mutation:
    
    ```graphql
    mutation {
      createPost(title: "GraphQL is awesome", content: "Lorem ipsum ...") {
        id
      }
    }
    ```

2. Let's assume we already have an existing `Post` node with the following values:

    - `id`: `"asd"`
    - `title`: `"GraphQL makes frontend development simple"`
    - `content`: `"Lorem ipsum ..."`

    Now we perform the following mutation:
    
    ```graphql
    mutation {
      updatePost(id: "asd", title: "GraphQL makes frontend AND backend development simple") {
        id
      }
    }
    ```

3. Let's assume we already have an existing `Post` node with the following values:

    - `id`: `"asd"`
    - `title`: `"GraphQL makes frontend development simple"`
    - `content`: `"Lorem ipsum ..."`

    Now we perform the following mutation:
    
    ```graphql
    mutation {
      deletePost(id: "asd") {
        id
      }
    }
    ```  

For each of these cases, let's investigate the payload that the server will push to the subscribed client. 

In the first scenario of the `createPost`-operation, the payload will look as follows:

```json
{
  "data": {
    "Post": {
      "operation": "CREATED",
      "node": {
        "title": "`GraphQL is awesome`",
        "content": "Lorem ipsum ..."
      },
      "previousValues": null
    }
  }
}
```

The `updatePost`-operation yields the following outcome:

```json
{
  "data": {
    "Post": {
      "operation": "UPDATED",
      "node": {
        "title": "`GraphQL `makes frontend development simple``",
        "content": "Lorem ipsum ..."
      },
      "previousValues": {
        "title": "`GraphQL `makes frontend AND backend development simple``",
        "content": "Lorem ipsum ..."
      }
    }
  }
}
```

And finally, for the `deletePost`-operation, this is the payload the server will send to the client:

```json
{
  "data": {
    "Post": {
      "operation": "UPDATED",
      "node": null,
      "previousValues": {
        "title": "`GraphQL `makes frontend development simple``",
        "content": "Lorem ipsum ..."
      }
    }
  }
}
```

Note that the Graphcool Subscriptions API also allows clients to specify that they're only interested in specific operations rather than getting informed about *created*-, *updated*- as well as *deleted*-operations. This can be done by specifying the `filter` for the `operation` field:

```graphql
subscription {
  Post(filter: {
    operation_in: [CREATED, UPDATED, DELETED]
  }) {
    node {
      title
      content
    }
    previousValues {
      title
      content
    }
  }
}
```


If the `filter` is specified, the client can add the operations it's interested in to the array that's provided for the `operation_in` field.


### API Endpoints: Simple vs Relay

In general, every Graphcool project will expose two different APIs:

* Simple API: Intuitive CRUD operations and data modelling
* Relay API: Corresponds to the schema requirements of [Relay](https://facebook.github.io/relay/)

Effectively, this means that there are *two* GraphQL Schemas that are backing one Graphcool project.

Relay is Facebook's homegrown GraphQL client that can be used in Javascript applications. The reason why Graphcool creates a dedicated API for it is that Relay makes a couple of assumptions about *how* the GraphQL schema is structured. The Relay API adheres to these assumptions and thus allows you to build a frontend application with Relay. For all other use cases, the Simple API will be the better choice. 
