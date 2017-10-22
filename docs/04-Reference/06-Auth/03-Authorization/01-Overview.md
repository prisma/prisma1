---
alias: iegoo0heez
description: Graphcool features a simple yet powerful permission system that integrates seamlessly with the available authentication solutions.
---

# Authorization

Graphcool features a simple yet powerful permission system that integrates seamlessly with the available [authentication concept](!alias-geekae9gah).

<InfoBox>

To get a practical introduction to the Graphcool authorization system, check out the [permissions](https://github.com/graphcool/graphcool/tree/master/examples/permissions) example.

</InfoBox>


## Whitelist permissions for modular authorization

In general, permissions follow a **whitelist approach**:

* *no operation is permitted unless explicitely allowed*
* *a permission cannot be nullified by other permissions*

Essentially this means that a request is only executed if and only if it *matches* at least one specified permission. This allows us to specify permissions in a modular way and to focus on a specific use case in a single permission which leads to many simple permissions instead of fewer complex ones.

## Specifying permissions in `graphcool.yml`

To grant permission right for everyone or a specify more fine-grained permission rules, you need to configure the `permissions` in your service definition file [`graphcool.yml`](!alias-foatho8aip).

> Read [this](!alias-foatho8aip#definition-permissions) to learn what the structure of each permission `graphcool.yml` looks like.

Consider this data model for the following examples:

```graphql
type User @model {
  id: ID! @isUnique
  messages: [Message!]! @relation(name: "UserMessages")
}

type Message @model {
  id: ID! @isUnique
  sentBy: User! @relation(name: "UserMessages")
  text: String!
  attachments: [String!]!
  location: String!
}
```

### Granting access to everyone for performing a specific API operation

To grant access to everyone for _reading_ nodes of type `Message`, you need to add the following operation to the `permission` root property in your `graphcool.yml`:

```yml
permissions:
  - operation: Message.read # Allow everyone to read `Message` nodes
```

### Granting access to only authenticated user for performing a specific operation

To grant access to only authenticated users for _creating_ nodes of type `Message`, you need to add the following operation to the `permission` root property in your `graphcool.yml`:

```yml
permissions:
  - operation: Message.create
    authenticated: true
```

### Granting access to a specific user group and for specific fields

The following permission only allows for _updating_ the `text` and `attachments` fields of the nodes of type `Message`. `location` must not be updated at all with this setup. Further, for a user to update `text` or `attachments`, the [permission query](!alias-iox3aqu0ee) in `updateMessage.graphql` must return true: 

```yml
permissions:
  - operation: Message.update
    authenticated: true
    fields: [text, attachments]
    query: updateMessage.graphql
```

The permission query defined in `updateMessage.graphql` could look like this, only allowing the `sender` of a `Message` to update it:

```graphql
query UpdateMessageQuery($node_id: ID!, $user_id: ID!) {
  SomeMessageExists(filter: {
    id: $node_id,
    sender: {
      id: $user_id
    }  
  })
}
```

## Request matching for permissions

When does a permission match a request? This is determined by the [permission parameters](!alias-soh5hu6xah). Additional conditions can be defined using [permission queries](!alias-iox3aqu0ee).



