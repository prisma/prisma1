---
alias: goh5uthoc1
path: /docs/reference/schema/relations
layout: REFERENCE
description: A relation defines the interaction between two types. Related types are reflected in both the data model as well as the GraphQL schema.
tags:
  - platform
  - relations
related:
  further:
    - shiz6ov4ae
  more:
---

# Relations

A *relation* defines the interaction between two [types](!alias-ij2choozae). Two types in a relation are connected via a [relation field](!alias-teizeit5se) on each type.

A relation can also connect a type with itself. It is then referred to as a *self-relation*.

## Required Relations

For a `to-one` relation field, you can configure whether it is *required* or *optional*. The required flag acts as a contract in GraphQL that this field can never be `null`. A field for the address of a user would therefore be of type `Address` or `Address!`.

Nodes for a type that contains a required `to-one` relation field can only be created using a [nested mutation](!alias-ubohch8quo) to ensure the according field will not be `null`.

> Note that a `to-many` relation field is always set to required. For example, a field that contains many user addresses always uses the type `[Address!]!` and can never be of type `[Address!]`. The reason is that in case the field doesn't contain any nodes, `[]` will be returned, which is not `null`.

## Relations in the Data Schema

A relation is defined in the Data Schema using the [@relation directive](!alias-aeph6oyeez#relation-fields):

```graphql
type User {
  id: ID!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story {
  id: ID!
  text: String!
  author: User! @relation(name: "UserOnStory")
}
```

Here we are defining a *one-to-many* relation between the `User` and `Story` types. The relation fields are `stories: [Story!]!` and `author: User!`. Note how `[Story!]!` defines multiple stories and `User!` a single user.

## Generated Operations Based On Relations

The relations that are included in your schema effect the available operations in the [GraphQL API](!alias-heshoov3ai). For every relation,

* [relation queries](!alias-aihaeph5ip) allow you to query data across types or aggregated for a relation
* [relation mutations](!alias-kaozu8co6w) allow you to connect or disconnect nodes
* [nested mutations](!alias-ubohch8quo) allow you to create and connect nodes across types
* [relation subscriptions](!alias-riegh2oogh) allow you to get notified of changes to a relation
