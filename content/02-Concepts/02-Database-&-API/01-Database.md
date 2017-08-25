# Database

## Graphcool Abstracts Away the Database Layer

In traditional backend development, you'd usually start by setting up a database. If you're using a SQL database, this also includes defining the[ relational schema](https://en.wikipedia.org/wiki/Database_schema) and writing the SQL queries for the CRUD operations on your model objects. Typically, the next step is to integrate an [ORM](https://en.wikipedia.org/wiki/Object-relational_mapping) that's responsible to map the data from the database to the API layer.

When using Graphcool, the whole database layer is abstracted away and you don't have to worry about the implementation details and writing SQL queries any more. All database operations are automatically generated based on the GraphQL types that you specify for your data model.

## Powerful SQL Database

Graphcool comes with a baked-in SQL database. For the **hosted version**, there'll be an [AWS Aurora](https://aws.amazon.com/rds/aurora/) instance to power your backend. The** local / self-hosted version** of Graphcool on the other hand is based on [MySQL](https://www.mysql.com/) which you can configure in your Docker setup.
