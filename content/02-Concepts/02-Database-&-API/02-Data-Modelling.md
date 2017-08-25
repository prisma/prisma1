# Data Modelling

## Using GraphQL SDL to Define Your Data Model

When using Graphcool, you're leveraging the [GraphQL type system](http://graphql.org/learn/schema/#type-system) to define your model objects. For example in a blogging application, you might define two simple types that look as follows:

```
type Person {
  id: ID!
  name: String!
  posts: [Post!]!
}

type Post {
  id: ID!
  title: String!
  content: String
  author: User!
}
```

The `type` keyword denotes the definition of a new data type in the GraphQL schema. Each type has a number of *fields* that represent the properties and relationships of the type. The `Person` type in the example above has the `name` field as well as `posts` field that represents a one-to-many relation to the `Post` type. Notice that the exclamation point following the type of a field means that it cannot be `null`.

Once you added a new type to your model schema, the Graphcool framework will add the appropriate definitions to the relational schema that backs the SQL database and expose CRUD operations through the GraphQL API. This essentially replaces the work you previously needed to do when implementing the ORM.

## The Schema Definition Language

GraphQL uses the [Schema Definition Language](https://www.graph.cool/docs/faq/graphql-sdl-schema-definition-language-kr84dktnp0/) (SDL) to let you define the types that are relevant in your application. SDL is very expressive despite its lightweight syntax. Besides types, it offers constructs like enums, interfaces, union types as well as custom directives so you have a powerful foundation to build your data model from the ground up. You can learn more about all the language constructs of the SDL on the official [GraphQL website](http://graphql.org/learn/schema/#scalar-types).
