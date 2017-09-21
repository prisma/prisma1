---
alias: ikahjeex0x
description: Frequently asked questions about everything that relates to  Graphcool's database layer.
---

# Database

### What kind of database is Graphcool using?

Graphcool is based on SQL. The _hosted version_ of Graphcool uses an instance of [AWS Aurora](https://aws.amazon.com/rds/aurora/) under the hood. When running a _self-hosted version_ of Graphcool, you can also use [MySQL](https://www.mysql.com/).


### Can I use my own database with Graphcool?

Yes, in the self-hosted version you can "bring your own database". Right now the choices of databases is limited though as only [MySQL](https://www.mysql.com/) is supported. It is however planned to support more database systems in the future.


### Can I perform reads and writes directly against the database when using Graphcool?

No, Graphcool abstracts away the database layer and does not expose any direct interfaces towards the database.


### Do I need an ORM when using Graphcool?

No, Graphcool's [GraphQL engine](!alias-thei2kephu#graphql-engine) effectively takes the role of what would be considered an ORM in Graphcool's architecture (though technically that's not 100% accurate). It retrieves the data from the database and exposes it through the API.
