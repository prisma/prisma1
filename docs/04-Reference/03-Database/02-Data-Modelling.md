---
alias: eiroozae8u
description: An overview of how to model application data in Graphcool.
---

# Data Modelling

## Overview

Graphcool uses (a subset of) the GraphQL [Schema Definition Language](https://www.graph.cool/docs/faq/graphql-sdl-schema-definition-language-kr84dktnp0/) for data modelling. Your data model is written in your service's `.graphql`-file, typically called `types.graphql`, which is the foundation for the actual database schema that Graphcool generates for you.

To learn more about the SDL, you can check out the [official documentation](http://graphql.org/learn/schema/#type-language).

### Example

This is an example for what a `types.graphql` could look like:

```graphql
type Tweet @model {  
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  text: String!
  owner: User! @relation(name: "UserOnTweet")
}

type User @model {
  id: ID! @isUnique
  name: String!
  tweets: [Tweet!]! @relation(name: "UserOnTweet")
}
```

### Building blocks of the data model

There are several available building blocks to shape your data model.

* [Types](#graphql-types) consist of multiple [fields](#fields) and are used to group similar entities together.
* [Relations](#relations) describe interactions between types.
* Special [directives](#graphql-directives) that cover different use cases are available.

Additionally, a project can contain prepopulated types and fields, referred to as [system artifacts](#system-artifacts). Different [naming conventions](#naming-conventions) define valid names.

### Writing a data model

There are a few things that are special about the way a data model is written.

The data model generally defines the [model types](#model-types) that represent the entities from your application domain. The nodes of these types will be persisted in the database and further determine the operations of your CRUD [API](!alias-abogasd0go).

Each type that you want to be part of this data model needs to be defined with the `@model` directive following the type name and the required `id` field.

There are three system fields, all of which are managed by the Graphcool runtime and read-only for you.

#### Required system field: `id` 

Every type that you define with the `@model` directive needs to have an `id: ID! @isUnique` field, otherwise `graphcool deploy` is going to fail. This `id` however is managed by Graphcool: Every new node that is created in your project will get assigned a globally unique ID automatically.

Notice that all your model types will implement the `Node` interface in the actual GraphQL schema that defines all the capabilities of your API. This is what the `Node` interface looks like:

```graphql
interface Node {
  id: ID! @isUnique
}
```

> You don't have to implement the `Node` interface yourself, this is implicitly handled by Graphcool when you're using the `@model` directive.

#### Optional system fields: `createdAt` and `updatedAt`

Graphcool offers two special fields that you can add to `@model` types:

- `createdAt: DateTime!`: Stores the exact date and time for when a node of this model type was created.
- `updatedAt: DateTime!`: Stores the exact date and time for when a node of this model type was last updated.

If you want your model types to expose these fields, you can simply add them to the type definition and Graphcool will take care of actually managing them for you.


```graphql
type Article @model {
  id: ID! @isUnique    # required system field
  createdAt: DateTime! # optional system field
  updatedAt: DateTime! # optional system field
}
``` 

<InfoBox type=warning>

Notice that you can not have custom fields that are called `createdAt` and `updatedAt` and of type `DateTime!`. When adding these to a model type, they will automatically be managed by Graphcool and are read-only for your application.

</InfoBox>

## Model types

A *model type* defines the structure for a certain type of your data. If you are familiar with SQL databases you can think of a type as the schema for a table. A type has a name, an optional description and one or multiple [fields](!alias-teizeit5se).

An instantiation of a type is called a *node*. The collection of all nodes is what you would refer to as your "application data". The term node refers to a node inside your data graph.

Every type you define will be available as a type in your GraphQL schema. 


### Defining a model type

A GraphQL type is defined in the data model with the keyword `type`:

```graphql
type Story @model {
  id: ID! @isUnique
  text: String!
  isPublished: Boolean @defaultValue(value: "false")
  author: Author! @relation(name: "AuthorStories")
}

type Author @model {
  id: ID! @isUnique
  age: Int
  name: String!
  stories: [Story!]! @relation(name: "AuthorStories")
}
```

### Generated operations based on types

The types that are included in your schema effect the available operations in the [GraphQL API](!alias-abogasd0go). For every type,

* [type queries](!alias-nia9nushae) allow you to fetch one or many nodes of that type
* [type mutations](!alias-ol0yuoz6go) allow you to create, update or delete nodes of that type
* [type subscriptions](!alias-aip7oojeiv) allow you to get notified of changes to nodes of that type


## Fields

*Fields* are the building blocks of a [types](#model-types) giving a node its shape. Every field is referenced by its name and is either [scalar](#scalar-types) or a [relation](#relations) field.

> The `Post` type might have a `title` and a `text` field both of type String and an `id` field of type `ID`.

### Scalar Types

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

<!--
#### GeoPoint

*Coming soon...*
-->

#### ID

An ID value is a generated unique 25-character string based on [cuid](https://github.com/graphcool/cuid-java). Fields with ID values are system fields and just used internally, therefore it is not possible to create new fields with the ID type.

### Type modifiers

#### List

Scalar fields can be marked with the list field type. A field of a relation that has the many multiplicity will also be marked as a list.

Note: List values are currently limited to 256KB in size, independently of the [scalar type](#scalar-types) of the field.

In queries or mutations, list fields have to be enclosed by square brackets, while the separate entries of the list adhere to the same formatting rules as lined out above: `listString: ["a string", "another string"]`, `listInt: [12, 24]`.

#### Required

Scalar fields can be marked as required (sometimes also referred to as "non-null"). When creating a new node, you need to supply a value for fields which are required and don't have a [default value](#default-value).

Required fields are usually marked using a `!` after the field type.

> An example for a required field on the `User` type could look like this: `name: String!`.

### Field constraints

Fields can be configured with certain field constraints to add further semantics to your data model.

#### Unique

Setting the *unique* constraint makes sure that two nodes of the type in question cannot have the same value for a certain field. The only exception is the `null` value, meaning that multiple nodes can have the value `null` without violating the constraint.

> A typical example is the `email` field on the `User` type.

Please note that only the first 191 characters in a String field are considered for uniqueness and the unique check is **case insensitive**. Storing two different strings is not possible if the first 191 characters are the same or if they only differ in casing.

To mark a field as unique, simply append the `@isUnique` directive to it:

```graphql
type User @model {
  email: String! @isUnique
}

```

### Default value

You can set a default value for scalar fields. The value will be taken for new nodes when no value was supplied during creation.

To specify a default value for a field, you can use the `@defaultValue` directive:

```graphql
type Story @model {
  isPublished: Boolean @defaultValue(value: "false")
}
```

### Generated operations based on fields

Fields in the data schema affect the available [query arguments](!alias-nia9nushae#query-arguments). Unique fields in the data schema add a new query argument to [queries for fetching one node](!alias-nia9nushae#fetching-a-single-node).


## Relations

A *relation* defines the semantics of a connection between two [types](#model-types). Two types in a relation are connected via a [relation field](#scalar-and-relation-fields) on each type.

A relation can also connect a type with itself. It is then referred to as a *self-relation*.

### Required relations

For a `to-one` relation field, you can configure whether it is *required* or *optional*. The required flag acts as a contract in GraphQL that this field can never be `null`. A field for the address of a user would therefore be of type `Address` or `Address!`.

Nodes for a type that contains a required `to-one` relation field can only be created using a [nested mutation](!alias-ol0yuoz6go#nested-mutations) to ensure the according field will not be `null`.

> Note that a `to-many` relation field is always set to required. For example, a field that contains many user addresses always uses the type `[Address!]!` and can never be of type `[Address!]`. The reason is that in case the field doesn't contain any nodes, `[]` will be returned, which is not `null`.

### Relations in the data model

A relation is defined in the data model using the `@relation` directive:

```graphql
type User @model {
  id: ID! @isUnique
  stories: [Story!]! @relation(name: "UserOnStory")
}

type Story @model {
  id: ID! @isUnique
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

The *static directive `@isUnique`* denotes [a unique, scalar field](#unique).

```graphql
## the `Post` type has a unique `slug` field
type Post @model {
  slug: String @isUnique
}
```

#### Relation Fields

The *static directive `@relation(name: String!)`* denotes a [relation field](!alias-goh5uthoc1). Most of the time, the same `@relation` directive appears twice in a type definitions file, to denote both sides of the relation:

```graphql
## the types `Post` and `User` are connected via the `PostAuthor` relation
type Post @model {
  user: User! @relation(name: "PostAuthor")
}

type User @model {
  posts: [Post!]! @relation(name: "PostAuthor")
}
```

#### Default value for scalar fields

The *static directive `@defaultValue(value: String!)`* denotes [the default value](#default-value) of a scalar field. Note that the `value` argument is of type String for all scalar fields:

```graphql
# the `title` and `published` fields have default values `New Post` and `false`
type Post @model {
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

Different objects you encounter in a Graphcool project like types or relations follow separate naming conventions to help you distinguish them.

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



## System Artifacts (only for [legacy Console projects](!alias-aemieb1aev))

In order to make the platform as seamless and integrated as possible, we introduced some predefined artifacts in each project. These artifacts are designed to be as minimal as possible and cannot be deleted. At the moment there are two type of artifacts: *system types* and *system fields*.

### `User` Type

Every project has a system type called `User`. As the `User` type is the foundation for our integration-based authentication system you cannot delete it. But of course you can still extend the `User` type to suit your needs and it behaves like every other type.

Apart from the predefined system fields, the `User` type can have additional system fields depending on the configured custom authentication.

You can add additional [fields](#fields) as with any other type.

### `File` Type

The `File` type is part of our [file management](!alias-eer4wiang0). Every time you upload a file, a new `File` node is created. Aside from the predefined system fields, the `File` type contains several other fields that contain meta information:
* `contentType: `: our best guess as to what file type the file has. For example `image/png`. Can be `null`
* `name: String`: the complete file name including the file type extension. For example `example.png`.
* `secret: String`: the file secret. Can be combined with your project id to get the file url. Everyone with the secret has access to the file!
* `size: Integer`: the file size in bytes.
* `url: String`: the file url. Looks something like `https://files.graph.cool/__PROJECT_ID__/__SECRET__`, that is the generic location for files combined with your project id endpoint and the file secret.

You can add additional [fields](#fields) as with any other type, but they need to be optional.

### `id` Field

Every type has a [required](#required) system field with the name `id` of type [ID](#id). The `id` value of every node (regardless of the type) is globally unique and unambiguously identifies a node ([as required by Relay](https://facebook.github.io/relay/docs/graphql-object-identification.html)). You cannot change the value for this field.

### `createdAt` and `updatedAt` Fields

Every type has the [DateTime](#datetime) fields `createdAt` and `updatedAt` that will be set automatically when a node is created or updated. You cannot change the values for these fields.

