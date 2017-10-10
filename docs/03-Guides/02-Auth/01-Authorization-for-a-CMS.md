---
alias: miesho4goo
description: Learn how to build secure authorization in GraphQL by defining role-based, owner-based and relation-based permissions powered by flexible filters.
---

# Authorization for a CMS with GraphQL Permission Queries

<InfoBox type=warning>

**Note**: This guide teaches the concept of [permission queries](!alias-iox3aqu0ee), but doesn't yet explain how to configure permissions in the new [Graphcool Framework](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333). The content however is still applicable if you just want to learn about permission queries!

An updated version of this guide is coming soon, stay tuned!

</InfoBox>


Security is one of the most critical parts of an application. Combining authentication methods with authorization rules empowers developers to build secure apps in a straight-forward way.

This article is a deep dive into the permission system used at Graphcool. The use case we want to explore is a *content management system* for documents.

Let's consider the following GraphQL schema in [IDL syntax](!alias-kr84dktnp0):

```graphql
type User {
  id: ID
  name: String!
  role: UserRole!
  accessGroups: [AccessGroup!]! @relation(name: "AccessGroupMembers")
  documents: [Document!]! @relation(name: "DocumentOwner")
}

type Document {
  id: ID
  content: String!
  published: Boolean!
  title: String!
  accessGroups: [AccessGroup!]! @relation(name: "AccessGroupDocuments")
  owner: [User!]! @relation(name: "DocumentOwner")
}

type AccessGroup {
  id: ID
  operation: AccessGroupOperation!
  members: [User!]! @relation(name: "AccessGroupMembers")
  documents: [Document!]! @relation(name: "AccessGroupDocuments")
}

enum UserRole {
  EDITOR,
  MODERATOR,
  ADMIN
}

enum AccessGroupOperation {
  READ,
  UPDATE,
  DELETE
}
```

Different kinds of users are interacting with this CMS. There are editors that can create and edit their own documents, moderators that have some elevated permissions and admins that have access to all operations.

Additionally to the different user roles, access groups can be defined to grant granular access control.

## Authorization Design Patterns

Before diving into the different **authorization design patterns**, let's start this off with a simple example to get used to the used terminology.

For more background information, the reference documentation offers a general [overview of the permission system](!alias-iegoo0heez) as well as a detailed list of available [permission queries, parameters and variables](!alias-iox3aqu0ee).

Let's have a look at a simple permission query:

### Everyone can see published documents

#### Permission Parameters

* **Operation:** `View Document`
* **Fields:** `id`, `content`, `published`, `title`
* **Audience:** `EVERYONE`

#### Permission Query

```graphql
query permitViewDocuments($node_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    published: true
  })
}
```

Here we use the `SomeDocumentExists` query to check if a given node of type `Document` (identified by `$node_id`) is published. Only then `SomeDocumentExists` returns `true`, and the operation is matched by this permission.

There are three broad categories of commonly used permission types that enable extremely powerful authorization rules when combined. Let's have a closer look!

## User Roles for Broad Authorization Rules

In our schema, we can assign different roles to users via the *enum field `role` on the `User` type* with the possible values `EDITOR`, `MODERATOR` and `ADMIN`. This paves the way for **role-based permissions**, which are very useful if different kinds of users should have different access levels.

Most role-based permission do not depend on the specific state of the node, or the relation between the node and the requesting user. Instead, mostly **the role of the user is the deciding factor whether an operation is allowed** or not.

### Admins can view all documents

#### Permission Parameters

* **Operation:** `View Document`
* **Fields:** `id`, `content`, `published`, `title`
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitViewDocuments($user_id: ID!) {
  SomeUserExists(filter: {
    id: $user_id
    role: ADMIN
  })
}
```

In this case, we use the `SomeUserExists` query to check if the session user (identified by the `$user_id` variable) has the `ADMIN` role. We don't use `SomeDocumentExists` because the document is irrelevant in this case.

### Editors can only assign themselves as the document owner

Whenever our schema contains relations that express ownership, we need to make sure that users don't maliciously assign a wrong owner. This works by defining a permission on the `DocumentOwner` relation.

#### Permission Parameters

* **Operation:** `Connect DocumentOwner`
* **Fields:** *relation permissions are not applicable to fields*
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitCreateDocuments($user_id: ID!, $ownerUser_id: ID!) {
  SomeUserExists(filter: {
    AND: [{
      id: $user_id
    }, {
      id: $ownerUser_id
    }, {
      role: EDITOR
    }]
  })
}
```

Because we want to express two conditions on the `id` variable, we need to use the logical operator `AND`. Then we check that the two variables `$user_id` (the logged-in user) and `$ownerUser_id` (the owner-to-be of the document) are the same. To only allow editors executing this operation, we add the `role: EDITOR` condition as well.

### Moderators and admins can assign anyone as the document owner

This is another permission on the `DocumentOwner` relation. But because moderators and admins can assign anyone as the owner of a document, we don't need the `$ownerUser_id` variable in this case.

#### Permission Parameters

* **Operation:** `Connect DocumentOwner`
* **Fields:** *relation permissions are not applicable to fields*
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitCreateDocuments($user_id: ID!) {
  SomeUserExists(filter: {
    id: $user_id
    role_in: [ADMIN, MODERATOR]
  })
}
```

### Moderators and admins can publish or unpublish any document

This is a `role-based` permission that only acts on a subset of the available fields, in this case the `published` field.

#### Permission Parameters

* **Operation:** `Create Document`
* **Fields:** `published`
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitUpdateDocuments($user_id: ID!) {
  SomeUserExists(filter: {
    id: $user_id
    role_in: [ADMIN, MODERATOR]
  })
}
```

### Admins can delete documents

Another *role-based* permission.

#### Permission Parameters

* **Operation:** `Delete Document`
* **Fields:** *no fields need to be selected for delete permissions*
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitDeleteDocuments($user_id: ID!) {
  SomeUserExists(filter: {
    id: $user_id
    role: ADMIN
  })
}
```

## Relation-based permissions for complete control

**Relation-based** permissions offer a lot of power and flexibility when defining permissions. In general, the existence of **a special path from a node to the session user across multiple relations** determines whether an operation is allowed.

A typical example is that a document can only be accessed if the session user is in the collaborators relation of the document owner. In this article however, we're implementing an **access control list or ACL** using the `AccessGroup` type. The `accessLevel` enum field with possible values `READ`, `UPDATE` and `DELETE` is used to control the access level on documents for specific users.

Access control lists are a common concept when defining authorization because they allow extreme granularity.

### Users with read access for a specific document can see it

#### Permission Parameters

* **Operation:** `View Document`
* **Fields:** `id`, `content`, `published`, `title`
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitViewDocuments($node_id: ID!, $user_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    accessGroups_some: {
      accessLevel: READ
      members_some: {
        id: $user_id
      }
    }
  })
}
```

Here we use relational filters, starting with the `SomeDocumentExists` query, to check if the document to be viewed (identified by `$node_id`) is connected to an access group with `READ` access that the session user (identified by `$user_id`) is connected to as well. Note that we can also turn the query around, starting with `SomeUserExists`:

```graphql
query permitViewDocuments($node_id: ID!, $user_id: ID!) {
  SomeUserExists(filter: {
    id: $user_id
    accessGroups_some: {
      accessLevel: READ
      documents_some: {
        id: $node_id
      }
    }
  })
}
```

Here we follow the relation from the other side of the path, starting at the user, passing the access group and finally reach the document.

### Users with the update access level can edit a specific document

#### Permission Parameters

* **Operation:** `Update Document`
* **Fields:** `content`, `published`, `title`
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitUpdateDocuments($node_id: ID!, $user_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    accessGroups_some: {
      accessLevel: UPDATE
      members_some: {
        id: $user_id
      }
    }
  })
}
```

### Users with the delete access level can delete a specific document

#### Permission Parameters

* **Operation:** `Delete Document`
* **Fields:** *no fields need to be selected for delete permissions*
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitDeleteDocuments($node_id: ID!, $user_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    accessGroups_some: {
      accessLevel: DELETE
      members_some: {
        id: $user_id
      }
    }
  })
}
```

## Elevated Access for Owners

Finally, a special case for relation-based permissions are **owner-based permissions**. It's a very common use case and useful to most applications, because usually, we want to allow the owner of a node access to special operations. In our case, the relation `DocumentOwner` determines the ownership of a document.

### Owners can view the documents they own

#### Permission Parameters

* **Operation:** `View Document`
* **Fields:** `id`, `content`, `published`, `title`
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitViewDocuments($node_id: ID!, $user_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    owner: {
      id: $user_id
    }
  })
}
```

Here, we use the `SomeDocumentExists` query and combine it with the `$node_id` and `$user_id` variables to ensure that the current node to be queried is owned by the logged-in user.

### The owner of a document can edit it

#### Permission Parameters

* **Operation:** `Update Document`
* **Fields:** `content`, `published`, `title`
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitUpdateDocuments($node_id: ID!, $user_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    owner: {
      id: $user_id
    }
  })
}
```

### The owner of a document can delete it

#### Permission Parameters

* **Operation:** `Delete Document`
* **Fields:** *no fields need to be selected for delete permissions*
* **Audience:** `AUTHENTICATED`

#### Permission Query

```graphql
query permitDeleteDocuments($node_id: ID!, $user_id: ID!) {
  SomeDocumentExists(filter: {
    id: $node_id
    owner: {
      id: $user_id
    }
  })
}
```

## Conclusion

Permission queries combine the simplicity and expressiveness of GraphQL queries with common authorization design patterns, allowing developers to specify complex permission setups in a straight-forward way.

Thinking in terms of the concepts of role-based, relation-based and owner-based permissions helps when designing the permissions for your application. If you need help with your authorization configuration, feel free to [reach out in the forum](https://graph.cool/forum/c/platform).
