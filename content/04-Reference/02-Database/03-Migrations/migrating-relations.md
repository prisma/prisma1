---
alias: shiz6ov4ae
path: /docs/reference/migrations/relations
layout: REFERENCE
description: Relations describe the interaction of two types in your data schema. You can add, remove or modify existing relations using schema migrations.
tags:
  - migrations
related:
  further:
    - goh5uthoc1
  more:
---

# Migrating Relations

The `@relation` directive can be attached to fields in your GraphQL schema to control relations.
Relations consist of two fields (or, in rare cases only one), have a name and both fields can either be singular or plural. Singular relation fields can be optional, but plural relation fields are always required.

> Read more about GraphQL [relations in the schema chapter](!alias-ahwoh2fohj)

## Adding a new relation

You can add new relations to your GraphQL schema using the `@relation(name: String!)` tag. This will **add new mutations and modify existing queries, mutations and subscriptions** in your [GraphQL API](!alias-heshoov3ai).

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
}
```

Let's add a new relation `UserStories` to the schema:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

Note that we added two fields:

* `stories: [Story!]! @relation(name: "UserStories")` on the `User` type signifies many Stories. List relation fields are always required.
* `user: User! @relation(name: "UserStories")` on the `Story` type signifies a [required relation field](!alias-teizeit5se#required). Singular relation fields can be optional or required.

By changing the multiplicities of the separate fields, we can create **one-to-one, one-to-many and many-to-many** relations.

Have a look at the [naming conventions for relations](!alias-oe3raifamo#scalar-and-relation-fields) to see what names are allowed and recommended.

## Removing an existing relation

To remove an existing relation, you can delete the corresponding relation fields in the schema file. This will remove **all data for this relation** as well.

> Removing a relation potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to not rely on this relation in your apps before deleting it.

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

Let's remove the `UserStories` relation again:

```graphql
type User implements Node {
  id: ID!
  name: String!
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
}
```

## Renaming an existing relation

Renaming a relation can be done by updating the existing `@relation(name: String!)` directives.

> Renaming a relation potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to adjust your app accordingly to the new name.

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To rename the `UserStories` relation to `UserOnStory`, we adjust the corresponding `@relation` tags:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserOnStory")
}
```

## Changing the type of a relation field

Whether changing the type of a relation field is possible might change depending on if there are already connected nodes in the relation.

> Changing the type of a relation field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to adjust your app accordingly to the changes.

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To change the type of the `user` field from `User!` to `Author`, modify the according type in the schema (here, we're also adding a new `Author` type) and transfer the `stories` field from the `User` to the `Author` type:

```graphql
type User implements Node {
  id: ID!
  name: String!
}

type Author implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: [Author!]! @relation(name: "UserOnStory")
}
```

Changing the type of a relation field is only possible when no nodes are connected in the relation.

## Changing the multiplicity of a relation field

Whether changing the multiplicity of a relation field is possible might change depending on if there are already connected nodes in the relation.

> Changing the multiplicity of a relation field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to adjust your app accordingly to the changes.

### Changing a relation field from to-many to to-one

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To change the multiplicity of the `stories` field from `to-many` to `to-one`, simply change the type from `[Story!]!` to `Story`.

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: Story @relation(name: "UserOnStory")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserOnStory")
}
```

Changing a `to-many` to a `to-one` field is only possible when no nodes are connected in the relation.

### Changing a relation field from to-one to to-many

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To change the multiplicity of the `user` field from `to-one` to `to-many`, simply change the type from `User!` to `[User!]!]`.

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: [User!]! @relation(name: "UserOnStory")
}
```

This is always possible, whether or not there are already connectd nodes in the relation.
