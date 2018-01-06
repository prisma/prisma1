---
alias: eiroozae8u
description: An overview of how to model application data in Graphcool.
---

# Data Modelling

TODO S: structure & content

- SDL
- how does embedded types/unidirection relations work

TODO N: polish

## Overview

Graphcool uses the GraphQL [Schema Definition Language](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51) (SDL) for data modelling. Your data model is written in one or more `.graphql`-files and is the foundation for the actual database schema that Graphcool generates under the hood. If you're using just a single file for your type definitions, this file is typically called `datamodel.graphql`.

> To learn more about the SDL, you can check out the [official GraphQL documentation](http://graphql.org/learn/schema/#type-language).

The `.graphql`-files containing the data model need to be specified in `graphcool.yml` under the `datamodel` property. For example:

```yml
datamodel:
  - types.graphql
  - enums.graphql
```

The data model that is deployed to a service defines the GraphQL API it exposes via the _database schema_.

### Example

This is an example for what `datamodel.graphql` with three simple types could look like:

```graphql
type Tweet {
  id: ID! @unique       # read-only system field (optional)
  createdAt: DateTime!  # read-only system field (optional)
  text: String!
  owner: User!
  location: Location! @relation
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

- Three types are created which are all mapped to the database (meaning each type is reflected by one dedicated table).
- `id`, `createdAt` and `updatedAt` are read-only system fields you can, but don't have to add to your types.
- There is a bidirectional relation between `User` and `Tweet` (the `@relation` directive is optional here, so it's left out).
- There is a unidirectional relation from `Tweet` to `Location`. The `onDelete` argument specifies the behaviour when a `Tweet` gets deleted: The related `Location` will also be deleted.
- Except for the `name` field on `User`, all fields are required in the data model (as indicated by the `!` following the type).

### Building blocks of the data model

There are several available building blocks to shape your data model.

* [Types](#graphql-types) consist of multiple [fields](#fields) and are used to group similar entities together. Each type in your data model is mapped to the database and CRUD operations for it are added to your GraphQL API.
* [Relations](#relations) describe _relationships_ between types.
* [Interfaces](http://graphql.org/learn/schema/#interfaces) are abstract types that include a certain set of fields which a type must include to _implement_ the interface. Currently, interfaces cannot be user-defined, but [there's a pending feature request](https://github.com/graphcool/framework/issues/83) for advanced interface support.
* Special [directives](#graphql-directives) covering different use cases are available.

### System fields

When writing type definitions for your Graphcool service, you need to be aware of three _system fields_ which are managed for you by Graphcool: `id`, `createdAt` and `updatedAt`.

> The values of these fields are currently read-only in the GraphQL API (unless in _import mode_) but will be made configurable in the future. See [this proposal](https://github.com/graphcool/framework/issues/1278) for more information.

In general, Graphcool will _always_ maintain these fields in the actual database. It's up to you to decide whether they will also be exposed in the GraphQL API by adding them explicitly to the SDL type definition.

<InfoBox type=warning>

Notice that you cannot have custom fields that are called `id`, `createdAt` and `updatedAt` since these field names are reserved for the system fields. Here are the only supported declarations for these three fields:

* `id: ID! @unique`
* `createdAt: DateTime!`
* `updatedAt: DateTime!`

</InfoBox>

#### System field: `id`

Every record in your Graphcool database (also called _node_) will get assigned a globally unique identifier when it's created.

Whenever you add the `id` field to a type definition to expose it in the GraphQL API, you must annotate it with the `@unique` directive.

The `id` has the following properties:

- Consists of 25 alphanumeric characters (letters are always lowercase)
- Always starts with a (lowercase) letter `c`
- Follows [cuid](https://github.com/ericelliott/cuid) (_collision resistant unique identifiers_) scheme

Notice that all your model types will implement the `Node` interface in the database schema. This is what the `Node` interface looks like:

```graphql
interface Node {
  id: ID! @unique
}
```


#### System fields: `createdAt` and `updatedAt`

Graphcool further has two special fields which you can add to your types:

- `createdAt: DateTime!`: Stores the exact date and time for when a node of this model type was _created_.
- `updatedAt: DateTime!`: Stores the exact date and time for when a node of this model type was _last updated_.

If you want your types to expose these fields, you can simply add them to the type definition and Graphcool will take care of actually managing them for you.

## Object types

An _object type_ (or short _type_) defines the structure for one concrete part of your data model. If you are familiar with SQL databases you can think of an object type as the schema for a table in your relational database. A type has a _name_ and one or multiple _[fields](#fields)_.

An instantiation of a type is called a _node_. The collection of all nodes is what you would refer to as your "application data". The term node refers to a node inside your "data graph".

Every type you define in your data model will be available as a type in the generated database schema.

### Defining an object type

A GraphQL object type is defined in the data model with the keyword `type`:

```graphql
type Article {
  id: ID! @unique
  text: String!
  isPublished: Boolean @default(value: "false")
}
```

The type defined above has the following properties:

- Name: `Story`
- Fields: `id`, `text` and `isPublished` (with the default value `false`)

### Generated operations based on types

The types that are included in your schema affect the available operations in the [GraphQL API](!alias-abogasd0go). For every type,

* [type queries](!alias-nia9nushae) allow you to fetch one or many nodes of that type
* [type mutations](!alias-ol0yuoz6go) allow you to create, update or delete nodes of that type
* [type subscriptions](!alias-aip7oojeiv) allow you to get notified of changes to nodes of that type

## Fields

_Fields_ are the building blocks of a [type](#object-types), giving a node its shape. Every field is referenced by its name and is either [scalar](#scalar-types) or a [relation](#relations) field.

### Scalar types

#### String

A `String` holds text. This is the type you would use for a username, the content of a blog post or anything else that is best represented as text.

Note: String values are currently limited to 256KB in size on the shared demo cluster. This limit can be increased on other clusters using [the cluster configuration](https://github.com/graphcool/framework/issues/748).

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

In queries or mutations, `DateTime` fields have to be specified in [ISO 8601 format](https://en.wikipedia.org/wiki/ISO_8601) with enclosing double quotes:

* `datatime: "2015"`
* `datatime: "2015-11"`
* `datatime: "2015-11-22"`
* `datetime: "2015-11-22T13:57:31.123Z"`.

#### Enum

Enums are defined on a service scope.

Like a Boolean an Enum can have one of a predefined set of values. The difference is that you can define the possible values. For example you could specify how an article should be formatted by creating an Enum with the possible values `COMPACT`, `WIDE` and `COVER`.

Note: Enum values can at most be 191 characters long.

In queries or mutations, Enum fields have to be specified without any enclosing characters. You can only use values that you defined for the enum: `enum: COMPACT`, `enum: WIDE`.

#### JSON

Sometimes you need to store arbitrary JSON values for loosely structured data. The JSON type makes sure that it is actually valid JSON and returns the value as a parsed JSON object/array instead of a string.

Note: JSON values are currently limited to 256KB in size on the shared demo cluster. This limit can be increased on other clusters using [the cluster configuration](https://github.com/graphcool/framework/issues/748).

In queries or mutations, JSON fields have to be specified with enclosing double quotes. Special characters have to be escaped: `json: "{\"int\": 1, \"string\": \"value\"}"`.

#### ID

An ID value is a generated unique 25-character string based on [cuid](https://github.com/graphcool/cuid-java). Fields with ID values are system fields and just used internally, therefore it is not possible to create new fields with the ID type.

### Type modifiers

#### List

Scalar fields can be marked with the list field type. A field of a relation that has the many multiplicity will also be marked as a list.

In queries or mutations, list fields have to be enclosed by square brackets, while the separate entries of the list adhere to the same formatting rules as lined out above: `listString: ["a string", "another string"]`, `listInt: [12, 24]`.

#### Required

Fields can be marked as required (sometimes also referred to as "non-null"). When creating a new node, you need to supply a value for fields which are required and don't have a [default value](#default-value).

Required fields are marked using a `!` after the field type: `name: String!`.

### Field constraints

Fields can be configured with certain field constraints to add further semantics to your data model.

#### Unique

Setting the _unique_ constraint makes sure that two nodes of the type in question cannot have the same value for a certain field. The only exception is the `null` value, meaning that multiple nodes can have the value `null` without violating the constraint.

> A typical example would be an `email` field on the `User` type where the assumption is that every `User` should have a unique email address.

Please note that only the first 191 characters in a String field are considered for uniqueness and the unique check is **case insensitive**. Storing two different strings is not possible if the first 191 characters are the same or if they only differ in casing.

To mark a field as unique, simply append the `@unique` directive to it:

```graphql
type User {
  email: String! @unique
}
```

#### More constraints

More database constraints will be added going forward according [to this feature request](https://github.com/graphcool/graphcool/issues/728).

### Default value

You can set a default value for scalar fields. The value will be taken for new nodes when no value was supplied during creation.

To specify a default value for a field, you can use the `@default` directive:

```graphql
type Story {
  isPublished: Boolean @default(value: "false")
}
```

### Generated operations based on fields

Fields in the data model affect the available [query arguments](!alias-nia9nushae#query-arguments). Unique fields in the data model add a new query argument to [queries for fetching one node](!alias-nia9nushae#fetching-a-single-node).

## Relations

A _relation_ defines the semantics of a connection between two [types](#object-types). Two types in a relation are connected via a [relation field](#scalar-and-relation-fields).

A relation can also connect a type with itself. It is then referred to as a _self-relation_.

### Required relations

For a `to-one` relation field, you can configure whether it is *required* or *optional*. The required flag acts as a contract in GraphQL that this field can never be `null`. A field for the address of a user would therefore be of type `Address` or `Address!`.

Nodes for a type that contains a required `to-one` relation field can only be created using a [nested mutation](!alias-ol0yuoz6go#nested-mutations) to ensure the according field will not be `null`.

> Note that a `to-many` relation field is always set to required. For example, a field that contains many user addresses always uses the type `[Address!]!` and can never be of type `[Address!]`. The reason is that in case the field doesn't contain any nodes, `[]` will be returned, which is not `null`.

### The `@relation` directive

When defining relations between types, there is the `@relation` directive which provides meta-information about the relation. It can take two arguments:

- `name`: An identifier for this relation (provided as a string). This argument is only required if relations are ambiguous.
- `onDelete` ([pending feature request](https://github.com/graphcool/framework/issues/1262)): Specifies the _deletion behaviour_. (In case a node with related nodes gets deleted, the deletion behaviour determines what should happen to the related node(s).) The input values for this argument are defined as an enum with the following possible values:
  - `NO_ACTION` (default): Keep the related node
  - `CASCADE`: Delete the related node
  - `SET_NULL`: Set the related node to `null`

#### Omitting the `@relation` directive

In the simplest case, where a relation between two types is unambiguous and the default deletion behaviour (`NO_ACTION`) should be applied, the corresponding relation fields do not have to be annotated with the `@relation` directive:

```graphql
type User {
  id: ID! @unique
  stories: [Story!]!
}

type Story {
  id: ID! @unique
  text: String!
  author: User!
}
```

Here we are defining a _one-to-many_ relation between the `User` and `Story` types. Since `onDelete` has not been provided, the default deletion behaviour is used: `NO_ACTION`. The semantics of this deletion behaviour are that stories and users can exists completely independently from another: When a `User` node is deleted, the nodes from its `stories` field will remain to exist. Likewise, when a `Story` node is deleted, the corresponding `author` node will remain to exist.

#### Using the `name` argument of the `@relation` directive

In certain cases, your data model may contain ambiguous relations. For example, consider you not only want a relation to express the "author-relationship" between `User` and `Story`, but you also want a relation to express which `Story` nodes have been _liked_ by a `User`.

In that case, you end up with two different relations between `User` and `Story`! In order to disambiguate these, you now need the `name` argument from the `@relation` directive:

```graphql
type User {
  id: ID! @unique
  writtenStories: [Story!]! @relation(name: "WrittenStories")
  likedStories: [Story!]! @relation(name: "LikedStories")
}

type Story {
  id: ID! @unique
  text: String!
  author: User! @relation(name: "WrittenStories")
  likedBy: [User!]! @relation(name: "LikedStories")
}
```

If the `name` wasn't provided in this case, there would be no way to decide whether `writtenStories` should relate to the `author` or the `likedBy` field.

#### Using the `onDelete` argument of the `@relation` directive

As mentioned above, when using relations in your system, you'll want to specify a dedicated deletion behaviour for the related nodes. That's what the `onDelete` argument of the `@relation` directive is being used for.

Consider the following example:

```graphql
type User {
  id: ID! @unique
  comments: [Comment!]! @relation(name: "CommentAuthor", onDelete: CASCADE)
  blog: Blog @relation(name: "BlogOwner", onDelete: CASCADE)
}

type Blog {
  id: ID! @unique
  comments: [Comment!]! @relation(name: "Comments", onDelete: CASCADE)
  owner: User! @relation(name: "BlogOwner", onDelete: SET_NULL)
}

type Comment {
  id: ID! @unique
  blog: Blog! @relation(name: "Comments", onDelete: NO_ACTION)
  author: User @relation(name: "CommentAuthor", onDelete: NO_ACTION)
}
```

Let's investigate the deletion behaviour for the three types:

- When a `User` node gets deleted:
  - all related `Comment` nodes will be deleted
  - the related `Blog` node will be deleted
- When a `Blog` node gets deleted:
  - all related `Comment` nodes will be deleted
  - the related `User` node will have its `blog` field set to `null`
- When a `Comment` node gets deleted:
  - the related `Blog` node continues to exist
  - the related `User` node continues to exist

### Generated operations based on relations

The relations that are included in your schema affect the available operations in the [GraphQL API](!alias-abogasd0go). For every relation,

* [relation queries](!alias-nia9nushae#relation-queries) allow you to query data across types or aggregated for a relation
* [nested mutations](!alias-ol0yuoz6go#nested-mutations) allow you to create, connect, update, upsert and delete nodes across types
* [relation subscriptions](!alias-aip7oojeiv#relation-subscriptions) allow you to get notified of changes to a relation

## GraphQL directives

A schema file follows the SDL syntax and can contain additional **static and temporary GraphQL directives**.

### Static directives

Static directives describe additional information about types or fields in the GraphQL schema.

#### Unique scalar fields

The static directive `@unique` denotes [a unique, scalar field](#unique). It does not take any arguments.

```graphql
# the `User` type has a unique `email` field
type User {
  email: String @unique
}
```

#### Relation fields

The static directive `@relation(name: String, onDelete: ON_DELETE! = NO_ACTION)` can be attached to a [relation field](#scalar-and-relation-fields).

[See above](#the-relation-directive) for more information.

#### Default value for scalar fields

The static directive `@default(value: String!)` denotes [the default value](#default-value) of a scalar field. Note that the `value` argument is of type String for all scalar fields (even if the fields themselves are not strings):

```graphql
# the `title`, `published` and `someNumber` fields have default values `New Post`, `false` and `42`
type Post {
  title: String! @default(value: "New Post")
  published: Boolean! @default(value: "false")
  someNumber: Int! @default(value: "42")
}
```

### Temporary directives

Temporary directives are used to perform run one-time migration operations. After a service whose type definitions contain a temporary directive was deployed, it **needs to be manually removed from the type definitions file**.

#### Renaming a type or field

The temporary directive `@rename(oldName: String!)` is used to rename a type or field.

```graphql
# renaming the `Post` type to `Story`, and its `text` field to `content`
type Story @model @rename(oldName: "Post") {
  content: String @rename(oldName: "text")
}
```

#### Migrating the value of a scalar field

The temporary directive `@migrationValue(value: String!)` is used to migrate the value of a scalar field. When changing an optional field to a requried field, it's necessary to also use this directive.

## Naming conventions

Different objects you encounter in a Graphcool service like types or relations follow separate naming conventions to help you distinguish them.

### Types

The type name determines the name of derived queries and mutations as well as the argument names for nested mutations. Type names can only contain **alphanumeric characters** and need to start with an uppercase letter. They can contain **at most 64 characters**.

*It's recommended to choose type names in the singular form.*
*Type names are unique on a service level.*

##### Examples

* `Post`
* `PostCategory`

### Scalar and relation fields

The name of a scalar field is used in queries and in query arguments of mutations. Field names can only contain **alphanumeric characters** and need to start with a lowercase letter. They can contain **at most 64 characters**.

The name of relation fields follows the same conventions and determines the argument names for relation mutations.

*It's recommended to only choose plural names for list fields*.
*Field names are unique on a type level.*

##### Examples

* `name`
* `email`
* `categoryTag`

### Relations

Relation names can only contain **alphanumeric characters** and need to start with an uppercase letter. They can contain **at most 64 characters**.

*Relation names are unique on a service level.*

##### Examples

* `UserOnPost`, `UserPosts` or `PostAuthor`, with field names `user` and `posts`
* `EmployeeAppointments`, `EmployeeOnAppointment` or `AppointmentEmployee`, with field names `employee` and `appointments`

### Enums

Enum values can only contain **alphanumeric characters and underscores** and need to start with an uppercase letter. The name of an enum value can be used in query filters and mutations. They can contain **at most 191 characters**.

*Enum names are unique on a service level.*
*Enum value names are unique on an enum level.*

##### Examples

* `A`
* `ROLE_TAG`
* `RoleTag`
