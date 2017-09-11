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

