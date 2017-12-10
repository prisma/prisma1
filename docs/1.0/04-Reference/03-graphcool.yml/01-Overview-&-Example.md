---
alias: foatho8aip
description: Overview
---

# Overview & Example

## Overview

Every Graphcool service consists of several components that developers can provide:

- **Type definitions (including database model)**: Defines types for the [GraphQL schema](http://graphql.org/learn/schema/). There are two kinds of types:
  - **Model types**: Determines the types that are to be persisted in the database. These types need to be annotated with the `@model`-directive and typically represent entities from the application domain. Read more in the [Database](!alias-viuf8uus7o) chapter.
  - **Transient types**: These types are not persisted in the database. They typically represent _input_ or _return_ types for certain API operations.
- **Event Subscriptions**: Used to implement asynchronous business logic reacting to events in the Graphcool database. Read more in the [Event Subscriptions](!alias-aiw4aimie9) chapter.

To manage these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML). The file can be altered manually or through dedicated commands of the [CLI](!alias-zboghez5go).

## Example

Here is a simple example of a service definition file:

```yml
# Type definitions
types: ./types.graphql


# Functions
functions:

  # Resolver for authentication
  authenticateCustomer:
    handler:
      # Specify a managed function as a handler
      code:
        src: ./src/authenticate.js
        # Define environment variables to be used in function
        environment:
          SERVICE_TOKEN: aequeitahqu0iu8fae5phoh1joquiegohc9rae3ejahreeciecooz7yoowuwaph7
          STAGE: prod
    type: resolver

  # Operation-before hook to validate an email address
  validateEmail:
    handler:
      # Specify a managed function as a handler; since no environment variables
      # are specified, we don't need `src`
      code: ./src/validateEmail.js
    type: operationBefore
    operation: Customer.create

  # Subscription to pipe a new message into Slack
  sendSlackMessage:
    handler:
      # Specify a webhook as a handler
      webhook:
        url: http://example.org/sendSlackMessage
        headers:
            Content-Type: application/json
            Authorization: Bearer cha2eiheiphesash3shoofo7eceexaequeebuyaequ1reishiujuu6weisao7ohc
    type: subscription
    query: ./src/sendSlackMessage/newMessage.graphql


# Permission rules
permissions:
# Everyone can read messages
- operation: Message.read

# Only authenticated users can create messages
- operation: Message.create
  authenticated: true

# To update a message, users need to be authenticated and the
# permission query in `./permissions/updateMessage.graphql` has
# to return `true`; note that this permission only applies to the
# `text` and `attachments` fields of the `Message` type, no other
# fields may be updated
- operation: Message.update 
  authenticated: true
  fields: 
    - text
    - attachments
  query: ./permissions/updateMessage.graphql

# To delete a message, users need to be authenticated and
# the permission query in `./permissions/deleteMessage.graphql`
# has to return `true`
- operation: Message.delete
  authenticated: true
  query: ./permissions/deleteMessage.graphql

# Everyone can perform all CRUD operations for customers
- operation: Customer.read
- operation: Customer.create
- operation: Customer.update
- operation: Customer.delete


# You can edit the fields a permission is applied to
- operation: Customer.Read
- fields: 
  - firstName
  - lastName

# Only authenticated users can connect a `Message`
# and `Customer` node via the `CustomerMessages`-relation
- operation: CustomerMessages.connect
  authenticated: true

# To disconnect a `Message` from a `Customer` node in the 
# `CustomerMessages`-relation, users need to be authenticated and the 
# permission query in `./permissions/disconnectCustomerMessages.graphql`
# has to return `true`
- operation: CustomerMessages.disconnect
  authenticated: true
  query: ./permissions/disconnectCustomerMessages.graphql

# Root tokens
rootTokens:
  - rootToken1
  - RootToken2 # can also start with uppercase letters
```

This service definition expects the following file structure:

```
.
├── graphcool.yml
├── types.graphql
├── src
│   ├── authenticate.js
│   ├── validateEmail.js
│   └── sendSlackMessage
│       └── newMessage.graphql
└── permissions
    ├── updateMessage.graphql
    └── deleteMessage.graphql
```
