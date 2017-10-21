---
alias: eip0theipo 
description: An overview of the Graphcool platform.
next: There is more to come
---

# Framework Overview

In this chapter, we provide an overview of the Graphcool platform. We're not yet talking about too much technicalities of the architecture but rather want to provide a high-level understanding of how to work with Graphcool in general.

## The Frontend Perspective

Frontend as well as mobile developers usually don't want to spend too much time on the server-side but rather focus on value-adding features they can bring to their users. Even more so, having to build a scalable web server with a database, authentication and data protection can be quite daunting for someone who is used to working with React, Angular, iOS, Android or any other frontend or mobile technology.

Frontend developers can choose to use the hosted version of Graphcool that will provide them with everything they need for a production-ready, scalable and secure backend.

All they need to do is define their data model in the GraphQL SDL and Graphcool will generated a ready-to-use GraphQL API with CRUD operations, filtering, pagination and ordering. 

## The Backend Perspective

For developers and teams that want to have complete control over their backend, Graphcool offers a set of tools that speed up the process of building of a GraphQL backend.

First and foremost, that's the GraphQL engine which makes use of four _infrastructure components_:

- Database
- Function Runtime
- Message Queue
- File Storage

Each of these components is swappable and you can choose your preferred product for each of these, like a MySQL database, OpenWhisk as a function runtime, RabbitMQ or S3 file storage.

By implementing the API proxy layer, you retain full control over what your API looks like. The GraphQL engine provides the foundation with an exhaustive CRUD API, you can then go and customize it according to your needs. The proxy layer is a simple web server (or potentially a serverless function) that provides the mapping from the GraphQL engine to your API.  


## Tools 

Graphcool provides two major tools for managing your projects and accessing the platform:

- Graphcool CLI: Allows to create, manage and deploy your Graphcool projects.
- Web Console: Provides a project dashboard with helpful metrics, monitoring and collaboration options as well as a straight view into the database.

### CLI & Project Configuration

The Graphcool CLI enables a fully local developer workflow. Here's a few of its core features:

- Creating a new Graphcool project
- Deploying a Graphcool project (and schema migration)
- Adding modules to a Graphcool project
- Local testing of functions and permissions

Every Graphcool project comes with a project configuration file (written in [YAML](https://en.wikipedia.org/wiki/YAML)) that defines the main project structure and its internal components, like your data model, serverless functions and permission rules.

The Graphcool CLI also has the concept of _environments_. The same project configuration can be deployed to multiple environments, enabling you to have a clear separation between, e.g. development, staging and production environments.

### Console

The Graphcool Console gives you magor insights into your project internals. You can use it for monitoring performance, accessing logs and managing collaboration. 









