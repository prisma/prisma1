---
alias: paesahku9t
description: Schema migrations allow you to evolve a GraphQL schema. Sometimes, data migrations are required as well.
---

# Migrations

_Schema migrations_ need to be performed when you're updating your model schema with any of the following actions:

- Adding, modifying or deleting a _type_ in the model schema
- Adding, modifying or deleting a _field_ of a concrete type in the model schema
- Adding, modifying or deleting a _relation_ between two concrete types in the model schema

A schema migration includes two steps:

1. Update your type definitions in `types.graphql`
2. Run `graphcool deploy` in the CLI

<InfoBox type=warning>

In case the migration requires additional information from your side, e.g. when you're renaming a type or a field or you add a non-nullable field to an existing type, you'll have to provided a _migration file_ with the required information. Notice that the CLI will detect these cases for you and launch a wizard that supports you in creating the migration file.

</InfoBox>



## Migrating Types

GraphQL types in your GraphQL schema can be controlled using the `type` keyword.

> Read more about GraphQL [types in the schema chapter](!alias-ahwoh2fohj)

### Adding a new type

You can add new types to your GraphQL schema by adding a new `type` section in the schema file. This will automatically **add queries, mutations and subscriptions** to your [GraphQL API](!alias-heshoov3ai).

Consider this schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
}
```

To add a new `Story` type, this is the new schema file:

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

This will automatically add the [system fields](!alias-uhieg2shio) to the type as well, you don't need to specify them yourself. Using the [`@relation` directive](!alias-aeph6oyeez#relation-fields), you can also directly create new relations to existing types as well.

Have a look at the [naming conventions for types](!alias-oe3raifamo#types) to see what names are allowed and recommended.

### Removing an existing type

To remove an existing type **including all of its data and relations**, remove the corresponding section in the schema file. You also need to remove all relation tags for relations that include the type to be deleted as well.

> Removing a type potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to not rely on any operations on the type in your apps before deleting it.

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

To remove the `Story` type again, we need to remove the `type Story` section as well as the relation field `stories` on `User`:

```graphql
type User implements Node {
  id: ID!
  name: String!
}
```

### Renaming an existing type

Renaming a type can be done with the `@rename(oldName: String!)` directive.

> Renaming a type potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to adjust your app accordingly to the new name.

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

To rename the `Story` type, we use the [temporary directive](!alias-aeph6oyeez#temporary-directives) `@rename(oldName: String!)` on the type itself. We also need to update the type name for all relation fields that use the old type name. In this case, that's the `stories: [Story!]!` field.

This is how it looks like:


```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Post!]! @relation(name: "UserStories")
}

type Post implements Node @rename(oldName: "Story") {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

After the successful rename operation, we obtain this new schema file:

```graphql
type User implements Node {
  id: ID!
  name: String!
  stories: [Post!]! @relation(name: "UserStories")
}

type Post implements Node {
  id: ID!
  isPublished: Boolean!
  text: String!
  slug: String! @isUnique
  tags: [String!]
  user: User! @relation(name: "UserStories")
}
```

Note that the temporary directive `@rename` is not in the schema file anymore.



## Migrating Scalar Fields

Fields in your GraphQL schema are always part of a GraphQL type and consist of a name and a [scalar GraphQL type](!alias-teizeit5se#scalar-types).
Different modifiers exist to mark a fields as [unique](!alias-teizeit5se#unique) or [list fields](!alias-teizeit5se#list).

Apart from scalar fields, fields can also be used to work with [relations](!alias-goh5uthoc1).

> Read more about GraphQL [fields in the schema chapter](!alias-ahwoh2fohj)

### Adding a new scalar field

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

### Removing an existing field

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

### Renaming an existing field

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

### Changing the type of an existing field

If no data exists on a given model type, changing the type of an existing field can always be achieved.

If there is already data, some field type migrations require the `@migrationValue` directive, while others don't. The following examples can be summarized with these rules:

* When only the raw type changes, but the required flag for the field stays the same (for example from `Int!` to `String!` or from `Int` to `String`):
  * When updating a field type **to String, no migration value needs to be provided**, the raw value will simply be casted to a String. If you provide a migration value however, all nodes will be migrated to that value.
  * All **other field type migrations require a migration value**.
* When the required flag changes:
  * When updating a field type **to required, a migration value has to be provided**.
  * When updating a field type **to optional, no migration needs to be provided**. If you provide a migration value however, all nodes will be migrated to that value.

#### Changing a field to String

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

#### Changing a field to another type

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

#### Changing a field from optional to required

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


## Migrating Relations

The `@relation` directive can be attached to fields in your GraphQL schema to control relations.
Relations consist of two fields (or, in rare cases only one), have a name and both fields can either be singular or plural. Singular relation fields can be optional, but plural relation fields are always required.

> Read more about GraphQL [relations in the schema chapter](!alias-ahwoh2fohj)

### Adding a new relation

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

### Removing an existing relation

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

### Renaming an existing relation

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

### Changing the type of a relation field

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

### Changing the multiplicity of a relation field

Whether changing the multiplicity of a relation field is possible might change depending on if there are already connected nodes in the relation.

> Changing the multiplicity of a relation field potentially breaks existing queries, mutations and subscriptions in your [GraphQL API](!alias-heshoov3ai). Make sure to adjust your app accordingly to the changes.

#### Changing a relation field from to-many to to-one

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

#### Changing a relation field from to-one to to-many

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

