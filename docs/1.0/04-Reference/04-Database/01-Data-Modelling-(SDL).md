---
alias: eiroozae8u
description: An overview of how to model application data in Graphcool.
---

# Data Modelling

## Overview

Graphcool uses (a subset of) the GraphQL [Schema Definition Language](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51) for data modelling. Your data model is written in your service's `.graphql`-file(s) and is the foundation for the actual database schema that Graphcool generates for you under the hood.

If you're using just a single file for your type definitions, this file is typically called `types.graphql`.

> To learn more about the SDL, you can check out the [official documentation](http://graphql.org/learn/schema/#type-language).

### Example

This is an example for what a simple `types.graphql` with two simple types could look like:

```graphql
type Tweet {
  id: ID! @unique       # read-only system field (optional)
  createdAt: DateTime!  # read-only system field (optional)
  text: String!
  owner: User!
  location: Location! @relation(onDelete: CASCADE)
}

type User {
  id: ID! @unique       # read-only system field (optional)
  createdAt: DateTime!  # read-only system field (optional)
  updatedAt: DateTime!  # read-only system field (optional)
  handle: String! @unique
  name: String
  tweets: [Tweet!]!
}

type Location {
  latitude: Float!
  longitude: Float!
}
```

Here are a few things to note about these type definitions:

- Three types are created which are all mapped to the database.
- `id`, `createdAt` and `updatedAt` are read-only system fields you can add to your types.
- There is a bidirectional relation between `User` and `Tweet` (the `@relation` directive is optional here).
- There is a unidirectional relation from `Tweet` to `Location`. The `onDelete` argument specifies the behaviour when a `Tweet` gets deleted: The related `Location` will also be deleted.
- Except for the `name` field on `User`, all fields are required in the data model (as indicated by the `!` following the type).

### Building blocks of the data model

There are several available building blocks to shape your data model.

* [Types](#graphql-types) consist of multiple [fields](#fields) and are used to group similar entities together. Each type in your data model is mapped to the database and CRUD operations for it are added to your GraphQL API.
* [Relations](#relations) describe _relationships_ between types.
* [Interfaces](http://graphql.org/learn/schema/#interfaces) are abstract types that include a certain set of fields which a type must include to _implement_ the interface.
* Special [directives](#graphql-directives) covering different use cases are available.

### System fields

When writing type definitions for your Graphcool database service, you need to be aware of three _system fields_ which are managed for you by Graphcool: `id`, `createdAt` and `updatedAt`. The values of these fields are read-only.

In general, Graphcool will always maintain these fields in the actual database. It's up to you to decide whether they will also be exposed in the GraphQL API by adding them explicitly to the SDL type definition.

<InfoBox type=warning>

Notice that you can not have custom fields that are called `id`, `createdAt` and `updatedAt` since these field names are reserved for the system fields.

</InfoBox>

#### System field: `id`

Every record in your Graphcool database (also called _node_) will get assigned a globally unique identifier when it's created.

Whenever you add the `id` field to a type definition to expose it in the GraphQL API, you must annotate it with the `@unique` directive.

The `id` has the following properties:

- Consists of 25 alphanumeric characters (letters are always lowercase)
- Always starts with a (lowercase) letter
- Follows [CUID](https://github.com/ericelliott/cuid) (_collision resistant unique identifiers_) scheme

>>>TODO<<<
do we still want to document the `Node` interface
>>>TODO<<<

Notice that all your model types will implement the `Node` interface in the actual GraphQL schema that defines all the capabilities of your API. This is what the `Node` interface looks like:

```graphql
interface Node {
  id: ID! @unique
}
```

#### System fields: `createdAt` and `updatedAt`

Graphcool further has two special fields which you can add to your types:

- `createdAt: DateTime!`: Stores the exact date and time for when a node of this model type was _created_.
- `updatedAt: DateTime!`: Stores the exact date and time for when a node of this model type was _last updated_.

If you want your types to expose these fields, you can simply add them to the type definition and Graphcool will take care of actually managing them for you. Similar to the `id` field, both fields will be maintained in the database anyways, and it's up to you whether they will be exposed.

## Object types

An _object type_ (or short just _type_) defines the structure for one concrete part of your data model. If you are familiar with SQL databases you can think of an object type as the schema for a table. A type has a _name_, an _optional description_ and one or multiple _[fields](#fields)_.

An instantiation of a type is called a _node_. The collection of all nodes is what you would refer to as your "application data". The term node refers to a node inside your data graph.

Every type you define will be available as a type in your generated GraphQL schema.

### Defining an object type

A GraphQL object type is defined in the data model with the keyword `type`:

```graphql
# This is the description for the `Article` type
type Article {
  id: ID! @unique
  text: String!
  isPublished: Boolean @defaultValue(value: "false")
}
```

The type defined above has the following properties:

- Name: `Story`
- Fields: `id`, `text` and `isPublished`
- Description: `This is the description for the `Article` type`

As you can see, a description can be added to a type by simply adding a comment right before its definition.

### Generated operations based on types

The types that are included in your schema effect the available operations in the [GraphQL API](!alias-abogasd0go). For every type,

* [type queries](!alias-nia9nushae) allow you to fetch one or many nodes of that type
* [type mutations](!alias-ol0yuoz6go) allow you to create, update or delete nodes of that type
* [type subscriptions](!alias-aip7oojeiv) allow you to get notified of changes to nodes of that type

## Fields

*Fields* are the building blocks of a [type](#object-types), giving a node its shape. Every field is referenced by its name and is either [scalar](#scalar-types) or a [relation](#relations) field.

### Scalar types

#### String

A `String` holds text. This is the type you would use for a username, the content of a blog post or anything else that is best represented as text.

Note: String values are currently limited to 256KB in size.

In queries or mutations, String fields have to be specified using enclosing double quotes: `string: "some-string"`.

#### Integer

An `Int` is a number that cannot have decimals. Use this to store values such as the weight of an ingredient required for a recipe or the minimum age for an event.

Note: `Int` values range from -2147483648 to 2147483647.

In queries or mutations, `Int` fields have to be specified without any enclosing characters: `int: 42`.

#### Float

A `Float` is a number that can have decimals. Use this to store values such as the price of an item in a store or the result of complex calculations.

In queries or mutations, `Float` fields have to be specified without any enclosing characters and an optional decimal point: `float: 42`, `float: 4.2`.

#### Boolean

A `Boolean` can have the value `true` or `false`. This is useful to keep track of settings such as whether the user wants to receive an email newsletter or if a recipe is appropriate for vegetarians.

In queries or mutations, `Boolean` fields have to be specified without any enclosing characters: `boolean: true`, `boolean: false`.

#### DateTime

The `DateTime` type can be used to store date or time values. A good example might be a person's date of birth.

In queries or mutations, `DateTime` fields have to be specified in [ISO 8601 format](https://en.wikipedia.org/wiki/ISO_8601) with enclosing double quotes: `datetime: "2015-11-22T13:57:31.123Z"`.

#### Enum

Enums are defined on a service scope.

Like a Boolean an Enum can have one of a predefined set of values. The difference is that you can define the possible values. For example you could specify how an article should be formatted by creating an Enum with the possible values `COMPACT`, `WIDE` and `COVER`.

Note: Enum values can at most be 191 characters long.

In queries or mutations, Enum fields have to be specified without any enclosing characters. You can only use values that you defined for the enum: `enum: COMPACT`, `enum: WIDE`.

#### JSON

Sometimes you need to store arbitrary JSON values for loosely structured data. The JSON type makes sure that it is actually valid JSON and returns the value as a parsed JSON object/array instead of a string.

Note: JSON values are currently limited to 64KB in size.

In queries or mutations, JSON fields have to be specified with enclosing double quotes. Special characters have to be escaped: `json: "{\"int\": 1, \"string\": \"value\"}"`.

#### ID

An ID value is a generated unique 25-character string based on [cuid](https://github.com/graphcool/cuid-java). Fields with ID values are system fields and just used internally, therefore it is not possible to create new fields with the ID type.

### Type modifiers

#### List

Scalar fields can be marked with the list field type. A field of a relation that has the many multiplicity will also be marked as a list.

In queries or mutations, list fields have to be enclosed by square brackets, while the separate entries of the list adhere to the same formatting rules as lined out above: `listString: ["a string", "another string"]`, `listInt: [12, 24]`.

#### Required

Fields can be marked as required (sometimes also referred to as "non-null"). When creating a new node, you need to supply a value for fields which are required and don't have a [default value](#default-value).

Required fields are marked using a `!` after the field type.

An example for a required field on the `User` type could look like this: `name: String!`.

### Field constraints

Fields can be configured with certain field constraints to add further semantics to your data model.

#### Unique

Setting the *unique* constraint makes sure that two nodes of the type in question cannot have the same value for a certain field. The only exception is the `null` value, meaning that multiple nodes can have the value `null` without violating the constraint.

> A typical example would be an `email` field on the `User` type where the assumption is that every `User` should have a unique email address.

>>>TODO<<<
needs update about specifics?
>>>TODO<<<

Please note that only the first 191 characters in a String field are considered for uniqueness and the unique check is **case insensitive**. Storing two different strings is not possible if the first 191 characters are the same or if they only differ in casing.

To mark a field as unique, simply append the `@unique` directive to it:

```graphql
type User {
  email: String! @unique
}
```

### Default value

You can set a default value for scalar fields. The value will be taken for new nodes when no value was supplied during creation.

To specify a default value for a field, you can use the `@defaultValue` directive:

```graphql
type Story {
  isPublished: Boolean @defaultValue(value: "false")
}
```

### Generated operations based on fields

Fields in the data schema affect the available [query arguments](!alias-nia9nushae#query-arguments). Unique fields in the data schema add a new query argument to [queries for fetching one node](!alias-nia9nushae#fetching-a-single-node).

## Relations

A *relation* defines the semantics of a connection between two [types](#object-types). Two types in a relation are connected via a [relation field](#scalar-and-relation-fields).

A relation can also connect a type with itself. It is then referred to as a *self-relation*.

### Required relations

For a `to-one` relation field, you can configure whether it is *required* or *optional*. The required flag acts as a contract in GraphQL that this field can never be `null`. A field for the address of a user would therefore be of type `Address` or `Address!`.

Nodes for a type that contains a required `to-one` relation field can only be created using a [nested mutation](!alias-ol0yuoz6go#nested-mutations) to ensure the according field will not be `null`.

> Note that a `to-many` relation field is always set to required. For example, a field that contains many user addresses always uses the type `[Address!]!` and can never be of type `[Address!]`. The reason is that in case the field doesn't contain any nodes, `[]` will be returned, which is not `null`.

### Relations in the data model

A relation is defined in the data model using the `@relation` directive:

```graphql
type User {
  id: ID! @unique
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story {
  id: ID! @unique
  text: String!
  author: User! @relation(name: "UserOnStory")
}
```

Here we are defining a *one-to-many* relation between the `User` and `Story` types. The relation fields are `stories: [Story!]!` and `author: User!`. Note how `[Story!]!` defines multiple stories and `User!` a single user.

### Generated operations based on relations

The relations that are included in your schema effect the available operations in the [GraphQL API](!alias-abogasd0go). For every relation,

* [relation queries](!alias-nia9nushae#relation-queries) allow you to query data across types or aggregated for a relation
* [relation mutations](!alias-ol0yuoz6go#relation-mutations) allow you to connect or disconnect nodes
* [nested mutations](!alias-ol0yuoz6go#nested-mutations) allow you to create and connect nodes across types
* [relation subscriptions](!alias-aip7oojeiv#relation-subscriptions) allow you to get notified of changes to a relation

## GraphQL Directives

A schema file follows the SDL syntax and can contain additional **static and temporary GraphQL directives**.

### Static Directives

Static directives describe additional information about types or fields in the GraphQL schema.

#### Unique Scalar Fields

The *static directive `@unique`* denotes [a unique, scalar field](#unique).

```graphql
## the `Post` type has a unique `slug` field
type Post {
  slug: String @unique
}
```

#### Relation Fields

The *static directive `@relation(name: String!)`* denotes a [relation field](#scalar-and-relation-fields). Most of the time, the same `@relation` directive appears twice in a type definitions file, to denote both sides of the relation:

```graphql
## the types `Post` and `User` are connected via the `PostAuthor` relation
type Post {
  user: User! @relation(name: "PostAuthor")
}

type User {
  posts: [Post!]! @relation(name: "PostAuthor")
}
```

#### Default value for scalar fields

The *static directive `@defaultValue(value: String!)`* denotes [the default value](#default-value) of a scalar field. Note that the `value` argument is of type String for all scalar fields:

```graphql
# the `title` and `published` fields have default values `New Post` and `false`
type Post {
  title: String! @defaultValue(value: "New Post")
  published: Boolean! @defaultValue(value: "false")
}
```

### Temporary directives

Temporary directives are used to run one-time migration operations. After a service whose type definitions contain a temporary directive was deployed, it **needs to be manually removed from the type definitions file**.

#### Renaming a Type or Field

The *temporary directive `@rename(oldName: String!)`* is used to rename a type or field.

```graphql
## Renaming the `Post` type to `Story`, and its `text` field to `content`
type Story @model @rename(oldName: "Post") {
  content: String @rename(oldName: "text")
}
```

#### Migrating the Value of a Scalar Field

The *temporary directive `@migrationValue(value: String!)`* is used to migrate the value of a scalar field. When changing an optional field to a requried field, it's necessary to also use this directive.


## Naming Conventions

Different objects you encounter in a Graphcool service like types or relations follow separate naming conventions to help you distinguish them.

### Types

The type name determines the name of derived queries and mutations as well as the argument names for nested mutations. Type names can only contain **alphanumeric characters** and need to start with an uppercase letter. They can contain **maximally 64 characters**.

*It's recommended to choose type names in the singular form.*
*Type names are unique on a service level.*

##### Examples

* `Post`
* `PostCategory`

### Scalar and Relation Fields

The name of a scalar field is used in queries and in query arguments of mutations. Field names can only contain **alphanumeric characters** and need to start with a lowercase letter. They can contain **maximally 64 characters**.

The name of relation fields follows the same conventions and determines the argument names for relation mutations.

*It's recommended to only choose plural names for list fields*.
*Field names are unique on a type level.*

##### Examples

* `name`
* `email`
* `categoryTag`

### Relations

The relation name determines the name of mutations to connect or disconnect nodes in the relation. Relation names can only contain **alphanumeric characters** and need to start with an uppercase letter. They can contain **maximally 64 characters**.

*Relation names are unique on a service level.*

##### Examples

* `UserOnPost`, `UserPosts` or `PostAuthor`, with field names `user` and `posts`
* `EmployeeAppointments`, `EmployeeOnAppointment` or `AppointmentEmployee`, with field names `employee` and `appointments`

### Enums

Enum values can only contain **alphanumeric characters and underscores** and need to start with an uppercase letter.
The name of an enum value can be used in query filters and mutations. They can contain **maximally 191 characters**.

*Enum names are unique on a service level.*
*Enum value names are unique on an enum level.*

##### Examples

* `A`
* `ROLE_TAG`
* `RoleTag`
