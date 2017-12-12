---
alias: paesahku9t
description: Schema migrations allow you to evolve a GraphQL schema. Sometimes, data migrations are required as well.
---

# Migrations

## Overview

_Schema migrations_ need to be performed when you're updating your model type definitions with any of the following actions:

- Adding, modifying or deleting a _type_ in your model type definitions
- Adding, modifying or deleting a _field_ of a concrete type in the model type definitions
- Adding, modifying or deleting a _relation_ between two concrete types in the model type definitions

A schema migration includes two steps:

1. Update your type definitions in `types.graphql`
2. Run [`graphcool deploy`](!alias-aiteerae6l#graphcool-deploy) in the CLI

Whenever the migration contains potentially destructive changes (e.g. when removing a type from your data model, which will delete all existing nodes of that type), you need to pass the `--force` (short: `-f`) to `graphcool deploy`.

<InfoBox type=warning>

In case the migration requires additional information from your side, e.g. when you're renaming a type or a field or you add a non-nullable field to an existing type, you'll need to temporarily add special directives to `types.graphql` that you can remove after successful [deployment](!alias-aiteerae6l#graphcool-deploy).

</InfoBox>


## Migrating types

GraphQL types in your GraphQL schema are defined using the `type` keyword.

> Read more about GraphQL types in the [Data Modelling](!alias-eiroozae8u) chapter.

### Adding a new type

You can add new types to your GraphQL schema by adding a new `type` section to your type definitions file. This will automatically **add queries, mutations and subscriptions** to your [GraphQL API](!alias-abogasd0go).

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
}
```

To add a new `Story` type (including a [relation](!alias-eiroozae8u#relations) to the `User` type), this is the new type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

Using the `@relation` directive, you can also directly create new relations to existing types as well.

Have a look at the [naming conventions for types](!alias-eiroozae8u#naming-conventions) to see what names are allowed and recommended.

### Removing an existing type

To remove an existing type **including all of its data and relations**, simply remove it from the type definitions file. You also need to remove all relation tags for relations that include that type as well.

> Removing a type potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to not rely on any operations on the type in your apps before deleting it.

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To remove the `Story` type again, we need to remove the `type Story` section as well as the relation field `stories` on `User`:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
}
```

### Renaming an existing type

Renaming a type can be done with the `@rename(oldName: String!)` directive.

> Renaming a type potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to adjust your app accordingly to the new name.

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To rename the `Story` type, you need to use the [temporary directive](!alias-eiroozae8u#temporary-directives) `@rename(oldName: String!)` on the type itself. We also need to update the type name for all relation fields that use the old type name. In this case, that's the `stories: [Story!]!` field.

This is how it looks like:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Post!]! @relation(name: "UserStories")
}

type Post @model @rename(oldName: "Story") {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

**After you successfully [deployed](!alias-aiteerae6l#graphcool-deploy) the service, you need to remove the temporary directive `@rename` from the file:**

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Post!]! @relation(name: "UserStories")
}

type Post @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```


## Migrating scalar fields

Fields in your GraphQL schema are always part of a GraphQL type and consist of a name and a [scalar GraphQL type](!alias-eiroozae8u#scalar-types).
Different modifiers exist to mark a fields as [unique](!alias-eiroozae8u#unique) or [list fields](!alias-eiroozae8u#list).

Apart from scalar fields, fields can also be used to work with [relations](!alias-goh5uthoc1).

> Read more about GraphQL [fields in the schema chapter](!alias-eiroozae8u#fields)

### Adding a new scalar field

You can add new fields to your GraphQL schema by including them in an existing type. **This will modify existing queries, mutations and subscriptions** in your [GraphQL API](!alias-abogasd0go).

Consider this type definitions file:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
}
```

Let's add a few fields to the `Story` type:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  description: String! @migrationValue(value: "No description yet")
  isPublished: Boolean @migrationValue(value: "true") @defaultValue(value: "false")
  length: Int
}
```

We added three fields `description: String!`, `isPublished: Boolean` and `length: Int`:

* as `description` is a required String, we need to supply the `@migrationValue(value: String)` directive to migrate existing `Story` nodes (assuming there are existing stories already).
* for the optional `isPublished` field of type Boolean, we don't have to supply a migration value, but we do so nonetheless. Additionally we set the default value to `false` using the `@defaultValue(value: String!)` directive.
* for the optional field `length`, we did not supply a migration or default value.

**After you successfully [deployed](!alias-aiteerae6l#graphcool-deploy) the service, you need to remove the temporary `@migrationValue` directives from the file:**

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  description: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

Have a look at the [naming conventions for fields](!alias-eiroozae8u#naming-conventions) to see what names are allowed and recommended.

### Removing an existing field

To remove an existing field, you can delete the corresponding line in the type definitions file. This will remove **all data for this field**.

> Removing a field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to not rely on this field in your apps before deleting it.

Consider this type definitions file:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  isPublished: Boolean!
  description: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

Let's remove some of the fields of the `Story` type:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  isPublished: Boolean @defaultValue(value: "false")
}
```

### Renaming an existing field

Renaming a field can be done with the `@rename(oldName: String!)` directive.

> Renaming a field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to adjust your app accordingly to the new name.

Consider this type definitions file:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  isPublished: Boolean!
  description: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

To rename the `description` field to `information`, we use the [temporary directive](!alias-eiroozae8u#temporary-directives-%28only-for-non-ejected-projects%29) `@rename(oldName: String!)` on the field itself:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  isPublished: Boolean!
  information: String! @rename(oldName: "description")
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

**After you successfully [deployed](!alias-aiteerae6l#graphcool-deploy) the service, you need to remove the temporary directive `@rename` from the file:**

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  isPublished: Boolean!
  information: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

### Changing the type of an existing field

If no data exists on a given model type, changing the type of an existing field is possible.

If there already is data, some field type migrations require the `@migrationValue` directive, while others don't. The following examples can be summarized with these rules:

* When only the raw type changes, but the required flag for the field stays the same (for example from `Int!` to `String!` or from `Int` to `String`):
  * When updating a field type **to String, no migration value needs to be provided**, the raw value will simply be casted to a String. If you provide a migration value however, all nodes will be migrated to that value.
  * All **other field type migrations require a migration value**.
* When the required flag changes:
  * When updating a field type **to required, a migration value has to be provided**.
  * When updating a field type **to optional, no migration needs to be provided**. If you provide a migration value however, all nodes will be migrated to that value.

#### Changing a field to String

Consider this schema:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  length: Int
}
```

Whether or not there is already data, `length` can be updated to a `String` without providing a migration value:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  length: String
}
```

If a node formerly had `length: 3`, it is now `length: "3"`.

#### Changing a field to another type

Consider this model type:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  length: Int
}
```

If there is already data, `name` can only be updated to an `Boolean!` when a migration value is provided:

```graphql
type Story @model {
  id: ID! @isUnique
  name: Boolean! @migrationValue(value: "true")
  length: String!
}
```

All nodes will have `name: true`.

#### Changing a field from optional to required

Consider this model type:

```graphql
type Story @model {
  id: ID! @isUnique
  name: String!
  length: Int
}
```

Whether or not there is already data, `length` can only be updated to an `Int!` when providing a migration value:

```graphql
type Story @model {
  id: ID! @isUnique
  name: Boolean! @migrationValue(value: "true")
  length: Int! @migrationValue(value: "0")
}
```

All nodes that formerly had `length: null` will now have `length: 0`. Nodes with a former non-null value for `length` will keep that value.

> Note: migrating optional list fields to be required, for example of type `[String!]` to `[String!]!` overwrites all values, not only the non-null values.


## Migrating relations

The `@relation` directive can be attached to fields in your GraphQL schema to control relations. 

Relations consist of two fields (or, in rare cases only one), have a name and both fields can either be singular or plural. Singular relation fields can be optional, but plural relation fields are always required.

> Read more about relations in the [Data Modelling](!alias-eiroozae8u) chapter.

### Adding a new relation

You can add new relations to your GraphQL schema using the `@relation(name: String!)` tag. This will **add new mutations and modify existing queries, mutations and subscriptions** in your [GraphQL API](!alias-abogasd0go).

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
}
```

Let's add a new relation `UserStories` to the schema:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

Note that we added two fields:

* `stories: [Story!]! @relation(name: "UserStories")` on the `User` type signifies many Stories. List relation fields are always required.
* `user: User! @relation(name: "UserStories")` on the `Story` type signifies a [required relation field](!alias-eiroozae8u#required-relations). Singular relation fields can be optional or required.

By changing the cardinalities of the separate fields, we can create **one-to-one, one-to-many and many-to-many** relations.

Have a look at the [naming conventions](!alias-eiroozae8u#naming-conventions) for relations to see what names are allowed and recommended.

### Removing an existing relation

To remove an existing relation, you can delete the corresponding relation fields in the type definitions file. This will remove **all data for this relation** as well.

> Removing a relation potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to not rely on this relation in your apps before deleting it.

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

Let's remove the `UserStories` relation again:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
}
```

### Renaming an existing relation

Renaming a relation can be done by updating the existing `@relation` directives and adding a second argument `oldName` to them: `@relation(name: String!, oldName: String!)`.

> Renaming a relation potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to adjust your app accordingly to the new name.

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To rename the `UserStories` relation to `UserOnStory`, we adjust the corresponding `@relation` tags:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory", oldName: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserOnStory", oldName: "UserStories")
}
```

**After you successfully [deployed](!alias-aiteerae6l#graphcool-deploy) the service, you need to remove the temporary directive argument `oldName` from the `@relation` directive in the file:**

```
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserOnStory")
}
```

### Changing the type of a relation field

Whether changing the type of a relation field is actually possible might change depending on if there are already connected nodes in the relation.

> Changing the type of a relation field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to adjust your app accordingly to the changes.

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To change the type of the `user` field from `User!` to `Author`, modify the according type in the schema (here, we're also adding a new `Author` type) and transfer the `stories` field from the `User` to the `Author` type:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
}

type Author @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: [Author!]! @relation(name: "UserOnStory")
}
```

Changing the type of a relation field is only possible when no nodes are connected in the relation.

### Changing the cardinality of a relation field

Whether changing the cardinality of a relation field is possible might change depending on if there are already connected nodes in the relation.

> Changing the cardinality of a relation field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-abogasd0go). Make sure to adjust your app accordingly to the changes.

#### Changing a relation field from to-many to to-one

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To change the cardinality of the `stories` field from `to-many` to `to-one`, simply change the type from `[Story!]!` to `Story`.

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: Story @relation(name: "UserOnStory")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserOnStory")
}
```

Changing a `to-many` to a `to-one` field is only possible when no nodes are connected in the relation.

#### Changing a relation field from to-one to to-many

Consider this type definitions file:

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserStories")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

To change the cardinality of the `user` field from `to-one` to `to-many`, simply change the type from `User!` to `[User!]!]`.

```graphql
type User @model {
  id: ID! @isUnique
  name: String!
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story @model {
  id: ID! @isUnique
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: [User!]! @relation(name: "UserOnStory")
}
```

This is always possible, whether or not there are already connectd nodes in the relation.

