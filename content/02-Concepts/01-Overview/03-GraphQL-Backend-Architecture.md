---
alias: thei2kephu 
description: An overview of the Graphcool platform.
---

# Graphcool Backend Architecture

## The GraphQL Engine

### Features of the GraphQL Engine

The core of the Graphcool backend is the _GraphQL engine_. It implements the mapping between the database and the automatically generated GraphQL API. 

> Though not 100% technically accurate, you can somewhat think of the GraphQL engine as a "GraphQL ORM".

Another major part of the GraphQL engine's functionality is the _event gateway_. The event gateway enables clients to _subscribe_ to events that are happening inside the Graphcool system and pushes out the corresponding data to them. 

The GraphQL engine also implements Graphcool's authorization system to ensure proper access rights and data protection.

### GraphQL Engine Infrastructure Components

The GraphQL engine is powered by a set of (exchangeable) infrastructure components:

- Database
- Serverless Function Runtime
- Message Queue
- File Storage

When choosing the hosted solution, each of these components will come with a concrete implementation:

- Database: AWS Aurora
- Serverless Function Runtime: AWS Lambda
- Message Queue: RabbitMQ
- File Storage: S3


