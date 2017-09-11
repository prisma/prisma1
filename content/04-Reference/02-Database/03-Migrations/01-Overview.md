---
alias: paesahku9t
description: Schema migrations allow you to evolve a GraphQL schema. Sometimes, data migrations are required as well.
---

# Migrations

_Schema migrations_ need to be performed when you're updating your model schema:

- Adding, modifying or deleting a type in the model schema
- Adding, modifying or deleting a field of a concrete type in the model schema
- Adding, modifying or deleting a relation between two concrete types in the model schema

A schema migration includes two steps:

1. Update your type definitions in `types.graphql`
2. Run `graphcool deploy` in the CLI

<InfoBox type=warning>

In case the migration requires additional information from your side, e.g. when you're renaming a type or a field or you add a non-nullable field to an existing type, you'll have to setup an additional file where this information is provided. Notice that the CLI will detect these cases for you and launch a wizard that supports you in creating that file.

</InfoBox>

## Schema Migrations with the CLI

The [CLI](!alias-kie1quohli) leverages the [project file](!alias-uhieg2shio) and allows you to synchronize schema changes across your local environment and the Console.
