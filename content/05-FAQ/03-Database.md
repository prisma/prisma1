---
alias: ikahjeex0x
description: Frequently asked questions about everything that relates to  Graphcool's database layer.
---

# Database

### What kind of database is Graphcool using?

* hosted: AWS Aurora
* self-hosted: MySQL

### Can I use my own database with Graphcool?

* yes, with the Docker setup (however, bound to MySQL as a DBMS)

### Can I perform reads and writes directly against the database when using Graphcool?

* no, Graphcool abstracts away the DB layer of traditional DB architecture and currently doesn't expose a direct interface to the DB
* maybe reads in the future?

### Do I need an ORM when using Graphcool?

* no, Graphcool effectively provides the ORM by connecting the DB with a GraphQL API

