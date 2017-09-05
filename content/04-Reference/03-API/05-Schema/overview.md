---
alias: ahwoh2fohj
path: /docs/reference/schema/overview
layout: REFERENCE
description: The data schema of a project is defined with types, fields and relations that will define the GraphQL schema of your GraphQL backend.
tags:
  - platform
  - data-schema
related:
  further:
    - kie1quohli
  more:
    - ga2ahnee2a
    - he6jaicha8
---

# Data Schema

The data schema of a project can be described in [SDL syntax](!alias-kr84dktnp0) and is usually managed using [schema files](!alias-ow2yei7mew). **The schema directly influences the available operations in the client APIs**.

Data schemas are typically saved as `.graphql` files. This is an example schema file, `schema.graphql`:

```graphql
type Tweet {
  createdAt: DateTime!
  id: ID! @isUnique
  owner: User! @relation(name: "UserOnTweet")
  text: String!
  updatedAt: DateTime!
}

type User {
  createdAt: DateTime!
  id: ID! @isUnique
  updatedAt: DateTime!
  name: String!
  tweets: [Tweet!]! @relation(name: "UserOnTweet")
}
```

The schema file follows the [SDL syntax](!alias-kr84dktnp0) and consists of different elements.

## Building blocks of the Data Schema

There are several available building blocks to shape your data schema.

* [Types](!alias-ij2choozae) consist of multiple [fields](!alias-teizeit5se) and are used to group similar entities together.
* [Relations](!alias-goh5uthoc1) describe interactions between types.
* Special [Directives](!alias-aeph6oyeez) that cover different use cases are available.

Additionally, a project contains prepopulated types and fields, referred to as [system artifacts](!alias-uhieg2shio). Different [naming conventions](!alias-oe3raifamo) define valid names.

## Obtaining a schema file

You can obtain the schema file for a Graphcool project in the Schema view of the Console or by using the [get-graphql-schema tool](!alias-maiv5eekan).

To create a new schema file from scratch, simply use your favorite text editor.
