---
alias: eew4hahphe 
description: An overview of how authorization works in Graphcool.
---

# Authorization & Data Protection

## All CRUD Operations are Whitelisted by Default

Graphcool offers an extremely expressive authorization system that allows to define powerful permission rules for data protection. By default, all operations that can be performed in a Graphcool project are *whitelisted*, this means that no one is authorized to perform any operation unless explicitly being entitled to do so.


> Despite the whitelist approach, you'll find that it is possible to perform all CRUD operations on your model types after you created them. This is because the necessary permissions are created for you whenever you define a new Model Type. This is just convenience to help you get going with your project - you should definitely revisit your permission rules before a project goes into production!


When attaching a permission rule to an operation, there are three major options:

* making the operation available to *everyone*
* defining that only *authenticated* users are able to perform this operations
* specifying a *permission query* that defines which users are able to perform an operation

When choosing the second option, every request that attempts to perform the operation needs to be *authenticated* with a token, otherwise it will be rejected. The third option uses the concept of permission queries that will be covered in the next section.

## Understanding Permission Queries

With Graphcool, data access rules can be expressed by means of so-called *permission queries*. Permission queries follow the same syntax as regular GraphQL queries, however, they are notably different from conventional queries in that they only ever return `true` or `false`. 

This means the *selection set* of the query is irrelevant - in fact, when writing a permission query, you only specify the input arguments but completely omit the selection set.

All permission queries that can be sent in Graphcool are of the form `SomeXExists` for any model type `X`. The idea of this is that you express a requirement in terms of a GraphQL query where only if that requirement is true the operation can be performed. 

Every permission query can get passed a number of variables as input arguments:

* the `id` of the node on which the operation is to be performed (except for *created*-mutations where such an `id` logically does not exist yet)
* the `id` of the user (or other authenticated) node that is attempting to perform the operation
* any scalar values of the type on which the operation is to be performed

These variables will be replaced at runtime with the actual values.

 The permission query will be executed right before the actual database read (or write) of the operation that it is associated with. Only if the permission query returns `true`, the corresponding database operation will actually be performed.

As an example, let's consider the following database schema for a simple blogging application:

```graphql
type User {
  isAdmin: Boolean!
  email: String!
  posts: [Post!]!
}

type Post {
  title: String!
  author: User!
}
```

Considering what was mentioned above, this database schema allows to express permissions with the following two queries:

* `SomeUserExists(filter: SomeUserExistsFilter)`
* `SomePostExists(filter: SomePostExistsFilter`)`

Assume you had the following requirements for the blogging app:

1. Everyone can **read** a post
2. Only authenticated users can **create** posts
3. Only the author of a post can **update** it
4. Only admin users *or* the author of a post can **delete** that post

Looking at these requirements, only the third and fourth need to be expressed with permission queries. The first one simply requires a permission that entitles *everyone* to perform it, the second one requires a permission entitling only *authenticated* users - no further specification in the form of a permission query is needed. These permissions are simple one-liners in your project configuration file.

Now let's consider the last two requirements and think about how they can be expressed with a permission query. 

First, we need to specify a permission query for the `updatePost`-mutation on the `Post` type. In this case, we need to get passed the `id` of the *node* (i.e. the `Post` that's about to be updated) but also the `id` of the *authenticated user* that sent the request (and who consequently is trying to update that node) as input arguments for the query.

Here is what the permission query looks like:

```
query OnlyAuthorsCanUpdatePosts($user_id: ID!, $node_id: ID!) {
  SomePostExists(
    filter: {
      id: $node_id,
      author: {
        id: $user_id
      }
    }
  )
}
```

We're using the `SomePostExists` query to find out if the node on which the operation is to be performed fulfils our requirement.

The `filter` expresses our data requirements. `$node_id` represents the `Post` that's about to be updated, `$user_id` represents the user that sent the request and attempts to perform the *update*-mutation. 

Interpreting what the `filter` describes, the `updatePost`-mutation can only be performed if there is a `Post` node in the database that has the following characteristics:

* the `id` of that `Post` node is identical to `$node_id`
* the `id` of the `author` of that `Post` node is identical to `$user_id`

Looking at the last requirement, we need to express two requirements and combine them with a *logical or*. There are two ways of doing this:

1. Create two separate permissions for the `deletePost`-mutation. The semantics of two permissions on the same operation are such that if one permission allows the operation, the operation will be performed (which is equivalent to a logical or).
2. Create one permission and use the `OR` operator to combine them.

With the first approach, we would make use of the `SomeUserExists` *and* the `SomePostExists` queries and create the following two permissions:

```graphql
# Permission Query #1
query OnlyAdminsCanDeletePosts($user_id: ID!, $node_id: ID!) {
  SomeUserExists(
    filter: {
      id: $user_id,
      isAdmin: true
    }
  )
}
```

```graphql
# Permission Query #2
query OnlyAuthorsCanDeletePosts ($user_id: ID!, $node_id: ID!) {
  SomePostExists(
    filter: {
      id: $node_id,
      author: {
        id: $user_id
      }
    }
  )
}
```

The second approach, would only use the `SomeUserExists` query and requires you to attach the following permission query the the `deletePost`-mutation:

```graphql
query ($user_id: ID!, $node_id: ID!) {
  SomeUserExists(
    filter:  { 
      OR: [
        {
          id: $user_id,
          posts_some: {
            id: $node_id
          }
        }, 
        {
          id: $user_id,
          isAdmin: true
        }
      ]
    }
  )
}
```

This time, we express the requirements only from the “user perspective” and require that the `User` either needs to be an admin *or* needs to be associated with (at least) one `Post` whose `id` is identical to the given `$node_id`.


