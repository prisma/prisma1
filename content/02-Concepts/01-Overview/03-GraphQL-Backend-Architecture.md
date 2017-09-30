---
alias: thei2kephu 
description: An overview of the Graphcool platform.
---

# Graphcool Backend Architecture

## GraphQL Engine

### Features of the GraphQL Engine

The core of the Graphcool backend is the _GraphQL engine_. It implements the mapping between the database and the automatically generated GraphQL API. 

> Though not 100% technically accurate, you can somewhat think of the GraphQL engine as a "GraphQL ORM".

Another major part of the GraphQL engine's functionality is the _event gateway_. The event gateway enables clients to _subscribe_ to events that are happening inside the Graphcool system and pushes out the corresponding data to them when the event occurs. 

The GraphQL engine also implements Graphcool's authorization system to ensure proper data protection and access rights.

### GraphQL Engine Infrastructure Components

The GraphQL engine is powered by a set of (exchangeable) infrastructure components:

- Database
- Serverless Function Runtime
- Message Queue
- File Storage

When choosing the hosted version of Graphcool, each of these components will come with a concrete implementation:

- Database: [AWS Aurora](https://aws.amazon.com/rds/aurora)
- Serverless Function Runtime: [AWS Lambda](https://aws.amazon.com/lambda)
- Message Queue: [RabbitMQ](https://www.rabbitmq.com/)
- File Storage: [S3](https://aws.amazon.com/s3/)

In the self-hosted version, the same products are used by default but can be swapped by the developer as needed.


## API Proxy Layer

The API proxy layer is a proper HTTP server that sits in front of the automatically generated CRUD GraphQL API provided by the GraphQL engine. It tightly integrates with the API and easily lets you customize it. This allows to hide certain operations or add more functionality to the API.

The proxy layer is not a required component in the Graphcool architecture, but it's an extremely powerful construct. If you're not making use of a proxy layer, your client applications can simply access the GraphQL engine's CRUD API with all its default operations.


## Integrating with external systems

One major use case of GraphQL in general is to act as an integration layer (API gateway) for external systems like microservices, 3rd-party APIs and legacy infrastructures.

Graphcool enables this use case and makes it easy to integrate your Graphcool backend with external systems. There are two main ways of doing this:

- Using the _GraphQL proxy layer_
- Implementing _resolvers_ (as serverless functions) directly in the GraphQL engine

If your use case for integrating an external system is rather simple, going for a resolver is a reasonable choice. For mode advanced use cases, consider using the API proxy layer.


## Modules

Graphcool also has the concept of _modules_ that allow for easily pulling it additional functionality into a Graphcool project. A module is nothing but a regular Graphcool project that implements a specific piece functionality, for example a concrete authentication approach like a [Facebook login](https://github.com/graphcool-examples/functions/tree/master/authentication/facebook-authentication).

### Specifying modules in the project configuration

The Graphcool project configuration has the top-level property `modules`. Each item that's listed under this property needs to point to the configuration file of another Graphcool project that's available locally on your machine (commonly inside a `modules` folder inside your project).

### Adding new modules through the CLI

When adding a new module to a Graphcool project, you can either download all required project files, including the project configuration (YAML), and manually put it into the `modules` directory or use the CLI for that process.

When using the CLI, you can simply execute the appropriate command and point it to a GitHub repository where the module is stored.



 




 



