---
alias: aeph6oyeez
path: /docs/reference/schema/directives
layout: REFERENCE
description: Different GraphQL directives are used in your data schema to express special situations like data constraints or relations.
tags:
  - platform
  - schema
related:
  further:
    - paesahku9t
  more:
---

# GraphQL Directives

A schema file follows the [SDL syntax](!alias-kr84dktnp0) and can contain additional **static and temporary GraphQL directives**.

## Static Directives

Static directives describe additional information about types or fields in the GraphQL schema.

### Unique Scalar Fields

The *static directive `@isUnique`* denotes [a unique, scalar field](!alias-teizeit5se#unique).

```graphql
# the `Post` type has a unique `slug` field
type Post {
  slug: String @isUnique
}
```

### Relation Fields

The *static directive `@relation(name: String!)`* denotes a [relation field](!alias-goh5uthoc1). Most of the time, the same `@relation` directive appears twice in a schema file, to denote both sides of the relation

```graphql
# the types `Post` and `User` are connected via the `PostAuthor` relation
type Post {
  user: User! @relation(name: "PostAuthor")
}

type User {
  posts: [Post!]! @relation(name: "PostAuthor")
}
```
### Default Value for Scalar Fields

The *static directive `@defaultValue(value: String!)`* denotes [the default value](!alias-teizeit5se#default-value) of a scalar field. Note that the `value` argument is of type String for all scalar fields

```graphql
# the `title` and `published` fields have default values `New Post` and `false`
type Post {
  title: String! @defaultValue(value: "New Post")
  published: Boolean! @defaultValue(value: "false")
}
```

## Temporary Directives

Temporary directives are used to run one-time migration operations. After a temporary directive has been pushed, it is not part of the schema anymore.

### Renaming a Type or Field

The *temporary directive `@rename(oldName: String!)`* is used to rename a type or field.

```graphql
# Renaming the `Post` type to `Story`, and its `text` field to `content`
type Story @rename(oldName: "Post") {
  content: String @rename(oldName: "text")
}
```

### Migrating the Value of a Scalar Field

The *temporary directive `@migrationValue(value: String!)`* is used to migrate the value of a scalar field. When changing an optional field to a requried field, it's necessary to also use this directive.
