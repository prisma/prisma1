---
alias: eiroozae8u
description: An overview of how to model application data in Graphcool.
---


# Defining a Data Model

Graphcool uses (a subset of) the GraphQL [Schema Definition Language] (SDL) for data modelling. Your model types are written in the `.graphql`-file, typically called `types.graphql`, which is the foundation for the actual database schema that Graphcool generates for you.

To learn more about the SDL, you can check out the [official documentation](http://graphql.org/learn/schema/#type-language).

## Example

This is an example for what a `types.graphql` could look like:

```graphql
type Tweet {  
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  owner: User! @relation(name: "UserOnTweet")
  text: String!
}

type User {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  name: String!
  tweets: [Tweet!]! @relation(name: "UserOnTweet")
}
```

## Building blocks of the Model Schema

There are several available building blocks to shape your model schema.

* [Types](!alias-ij2choozae) consist of multiple [fields](!alias-teizeit5se) and are used to group similar entities together.
* [Relations](!alias-goh5uthoc1) describe interactions between types.
* Special [Directives](!alias-aeph6oyeez) that cover different use cases are available.

Additionally, a project contains prepopulated types and fields, referred to as [system artifacts](!alias-uhieg2shio). Different [naming conventions](!alias-oe3raifamo) define valid names.

## Obtaining a schema file

You can obtain the schema file for a Graphcool project in the Schema view of the Console or by using the [get-graphql-schema tool](!alias-maiv5eekan).

To create a new schema file from scratch, simply use your favorite text editor.
