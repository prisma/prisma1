---
alias: va4ga2phie
description: Learn the fundamentals of using Prisma.
---

# Changing the data model and updating the API

You now learned how to deploy a Prisma service, how to explore its API and how to interact with it by sending queries and mutations.

In this tutorial, you'll learn the following:

- Make changes to the data model
- Deploy the changes to update the service's API

> To ensure you're not accidentally skipping an instruction in the tutorial, all required actions on your end are highlighted with a little counter on the left.
>
> **Pro tip**: If you're only keen on trying the practical parts but don't care so much about the explanations of what's going on, you can simply jump from instruction to instruction.

## Changing the data model

The last thing we want to cover in this tutorial is how you can update the API by making changes to the data model.

We want to make the following changes to the data model:

- Add an `age` field to the `User` type.
- Track the exact time when a `User` was _initially created_ or _last updated_.
- Add a new `Post` type with a `title` field.
- Create a one-to-many relation between `User` and `Post` to express that one `User` can create many `Post` nodes.

<Instruction>

Start by adding the required fields to the `User` type:

```graphql
type User {
  id: ID! @unique
  createdAt: DateTime!
  updatedAt: DateTime!
  name: String!
  age: Int
}
```

</Instruction>

The `age` field is of type `Int` and not required on the `User` type. This means you can store `User` nodes where `age` will be null (in fact, this is the case for the `User` named `Sarah` you created before).

`createdAt` and `updatedAt` on the other hand are actually special fields that are managed by Prisma. Under the hood, Prisma always maintains these fields - but they're only exposed in your API once you add them to the type definition in the data model (the same is true for the `id` field by the way).

> **Note**: Right now, the values for these fields are read-only. In the future, it will be possible to set the values for these fields via regular mutations as well. To learn more about this feature and timeline, check out this [GitHub issue](https://github.com/graphcool/prisma/issues/1278).

So far, the changes you made are only local. So, you won't be able to access the new fields in a GraphQL Playground if you open it right now.

## Deploying your changes & updating the API

<Instruction>

To make your changes take effect, you need to to deploy the service again. In the `hello-world` directory, run the following command:

```sh
prisma1 deploy
```

</Instruction>

You can now either open up a new GraphQL Playground or _reload the schema_ in one that's already open (the button for reloading the schema is the **Refresh**-button right next to the URL of your GraphQL API).

Once you did that, you can access the new fields on the `User` type.

<Instruction>

Try this mutation to create a new `User` node and set its `age` field:

```graphql
mutation {
  createUser(data: {
    name: "John"
    age: 42
  }) {
    id
    createdAt
    updatedAt
  }
}
```

</Instruction>

Lastly in this tutorial, we want to add another type, called `Post`, to the data model and create a relation to the existing `User` type.

Creating a relation between types comes very natural: All you need to do is add a new field of the related type to represent one end of the relation. Relations can - but don't have to - go in both directions.

Go ahead and start by defining the new `Post` type with its end of the relation.

<Instruction>

Open `datamodel.graphql` and add the following type definition to it:

```graphql
type Post {
  id: ID! @unique
  title: String!
  author: User!
}
```

</Instruction>

<Instruction>

To apply these changes, you need to run `prisma1 deploy` inside the `hello-world` directory again.

</Instruction>

Every `Post` now requires a `User` node as its `author`. The way how this works is by using the `connect` argument for _nested_ mutations.

<Instruction>

You can for example send the following mutation to connect a new `Post` node with an existing `User` node (you'll of course have to replace the `__USER_ID__` placeholder with the actual `id` of a `User`):

```graphql
mutation {
  createPost(data: {
    title: "GraphQL is awesome"
    author: {
      connect: {
        id: "__USER_ID__"
      }
    }
  }) {
    id
  }
}
```

</Instruction>

Let's also add the other end of the relation, so we have a proper one-to-many relationship between the `User` and the `Post` types.

<Instruction>

Open `datamodel.graphql` and add a new field, called `posts`, to the `User` type so it looks as follows:

```graphql
type User {
  id: ID! @unique
  createdAt: DateTime!
  updatedAt: DateTime!
  name: String!
  age: Int
  posts: [Post!]!
}
```

</Instruction>

That's it! The new `posts` field represents a list of `Post` nodes which were created by that `User`.

<Instruction>

Of course, this now also allows you to send nested queries where you're asking for all `User` nodes, as well as all the `Post` nodes for these users as well:

```graphql
{
  users {
    name
    posts {
      title
    }
  }
}
```

</Instruction>

## Next steps

In this tutorial, we covered the very basics of using Prisma - but there's a lot more to explore!

Here's a few pointers for where you can go next:

- **Quickstart Tutorials (Backend & Frontend)**: The remaining quickstart tutorials explain how to use Prisma together with conrete languages and frameworks, like [React](!alias-tijghei9go), [Node.js](!alias-phe8vai1oo) or [TypeScript](!alias-rohd6ipoo4).
- [**Examples**](https://github.com/graphcool/Prisma/tree/master/examples): We're maintaing a list of practical examples showcasing certain use cases and scenarios with Prisma, such as authentication & permissions, file handling, wrapping REST APIs or using GraphQL subscriptions.
- [**Deployment Docs**](!alias-eu2ood0she): To learn more about different deployment options, you can check out the cluster documentation.
