---
alias: oe3raifamo
path: /docs/reference/schema/naming-conventions
layout: REFERENCE
description: The different objects you encounter in a Graphcool project like types or relations follow separate naming conventions to help you distinguish them.
tags:
  - platform
  - queries
  - mutations
  - relations
  - fields
  - data-schema
related:
  further:
    - teizeit5se
    - ij2choozae
    - goh5uthoc1
  more:
    - eicha8ooph
---

# Naming Conventions

Different objects you encounter in a Graphcool project like types or relations follow separate naming conventions to help you distinguish them.

## Types

The type name determines the name of derived queries and mutations as well as the argument names for nested mutations. Type names can only contain **alphanumeric characters** and need to start with an uppercase letter. They can contain **maximally 64 characters**.

*It's recommended to choose type names in the singular form.*
*Type names are unique on a project level.*

#### Examples

* `Post`
* `PostCategory`

## Scalar and Relation Fields

The name of a scalar field is used in queries and in query arguments of mutations. Field names can only contain **alphanumeric characters** and need to start with a lowercase letter. They can contain **maximally 64 characters**.

The name of relation fields follows the same conventions and determines the argument names for relation mutations.

*It's recommended to only choose plural names for list fields*.
*Field names are unique on a type level.*

#### Examples

* `name`
* `email`
* `categoryTag`

## Relations

The relation name determines the name of mutations to connect or disconnect nodes in the relation. Relation names can only contain **alphanumeric characters** and need to start with an uppercase letter. They can contain **maximally 64 characters**.

*Relation names are unique on a project level.*

#### Examples

* `UserOnPost`, `UserPosts` or `PostAuthor`, with field names `user` and `posts`
* `EmployeeAppointments`, `EmployeeOnAppointment` or `AppointmentEmployee`, with field names `employee` and `appointments`

## Enums

Enum values can only contain **alphanumeric characters and underscores** and need to start with an uppercase letter.
The name of an enum value can be used in query filters and mutations. They can contain **maximally 191 characters**.

*Enum names are unique on a project level.*
*Enum value names are unique on an enum level.*

#### Examples

* `A`
* `ROLE_TAG`
* `RoleTag`
