---
alias: ahv6rohnge
path: /docs/reference/migrations/fields
layout: REFERENCE
description: Fields are the smallest component in your data schema. You can add, remove or modify existing fields using schema migrations.
tags:
  - migrations
related:
  further:
    - teizeit5se
  more:
---

# Migrating Scalar Fields

Fields in your GraphQL schema are always part of a GraphQL type and consist of a name and a [scalar GraphQL type](!alias-teizeit5se#scalar-types).
Different modifiers exist to mark a fields as [unique](!alias-teizeit5se#unique) or [list fields](!alias-teizeit5se#list).

Apart from scalar fields, fields can also be used to work with [relations](!alias-goh5uthoc1).

> Read more about GraphQL [fields in the schema chapter](!alias-ahwoh2fohj)

## Adding a new scalar field

You can add new fields to your GraphQL schema by including them in an existing type. **This will modify existing queries, mutations and subscriptions** in your [GraphQL API](!alias-heshoov3ai).

Consider this schema file:

```graphql
type Story implements Node {
  id: ID!
  name: String!
}
```

Let's add a few fields to the `Story` type:

```graphql
type Story implements Node {
  id: ID!
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

Because the directive `@migrationValue(value: String!)` is temporary, we receive an updated schema file after the migration was successful:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  description: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

Have a look at the [naming conventions for fields](!alias-oe3raifamo#scalar-and-relation-fields) to see what names are allowed and recommended.

## Removing an existing field

To remove an existing field, you can delete the corresponding line in the schema file. This will remove **all data for this field**.

> Removing a field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to not rely on this field in your apps before deleting it.

Consider this schema file:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  isPublished: Boolean!
  description: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

Let's remove some of the fields of the `Story` type:

```graphql
type User implements Node {
  id: ID!
  name: String!
  isPublished: Boolean @defaultValue(value: "false")
}
```

## Renaming an existing field

Renaming a field can be done with the `@rename(oldName: String!)` directive.

> Renaming a field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to adjust your app accordingly to the new name.

Consider this schema file:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  isPublished: Boolean!
  description: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

To rename the `description` field to `information`, we use the [temporary directive](!alias-aeph6oyeez#temporary-directives) `@rename(oldName: String!)` on the field itself:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  isPublished: Boolean!
  information: String! @rename(oldName: "description")
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

After the successful rename operation, we obtain this new schema file:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  isPublished: Boolean!
  information: String!
  isPublished: Boolean @defaultValue(value: "false")
  length: Int
}
```

Note that the temporary directive `@rename` is not in the schema file anymore.

## Changing the type of an existing field

If no data exists on a given model type, changing the type of an existing field can always be achieved.

If there is already data, some field type migrations require the `@migrationValue` directive, while others don't. The following examples can be summarized with these rules:

* When only the raw type changes, but the required flag for the field stays the same (for example from `Int!` to `String!` or from `Int` to `String`):
  * When updating a field type **to String, no migration value needs to be provided**, the raw value will simply be casted to a String. If you provide a migration value however, all nodes will be migrated to that value.
  * All **other field type migrations require a migration value**.
* When the required flag changes:
  * When updating a field type **to required, a migration value has to be provided**.
  * When updating a field type **to optional, no migration needs to be provided**. If you provide a migration value however, all nodes will be migrated to that value.

### Changing a field to String

Consider this schema:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  length: Int
}
```

Whether or not there is already data, `length` can be updated to a `String` without providing a migration value:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  length: String
}
```

If a node formerly had `length: 3`, it is now `length: "3"`.

### Changing a field to another type

Consider this schema:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  length: Int
}
```

If there is already data, `name` can only be updated to an `Boolean!` when a migration value is provided:

```graphql
type Story implements Node {
  id: ID!
  name: Boolean! @migrationValue(value: "true")
  length: String!
}
```

All nodes will have `name: true`.

### Changing a field from optional to required

Consider this schema:

```graphql
type Story implements Node {
  id: ID!
  name: String!
  length: Int
}
```

Whether or not there is already data, `length` can only be updated to an `Int!` when providing a migration value:

```graphql
type Story implements Node {
  id: ID!
  name: Boolean! @migrationValue(value: "true")
  length: Int! @migrationValue(value: "0")
}
```

All nodes that formerly had `length: null` will now have `length: 0`. Nodes with a former non-null value for `length` will keep that value.

> Note: migrating optional list fields to be required, for example of type `[String!]` to `[String!]!` overwrites all values, not only the non-null values.
