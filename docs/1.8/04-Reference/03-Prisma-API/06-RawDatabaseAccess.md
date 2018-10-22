---
alias: alq1vmndfp
description: Raw Database Access
---

# Raw Database Access
You can access the native API of the underlying database and perform complex operations.

As an example, consider the following datamodel:

```graphql
type User {
  id: ID! @unique
  name: String!
  createdAt: DateTime!
  updatedAt: DateTime!
}

```

To enable raw database access, add `rawAccess` to your `docker-compose.yml` file.

```yml
version: '3'
services:
  prisma:
    image: prismagraphql/prisma:1.18.1
    restart: always
    ports:
    - "4466:4466"
    environment:
      PRISMA_CONFIG: |
        port: 4466
        databases:
          default:
            connector: postgres
            host: postgres
            port: 5432
            # port: 3306
            user: prisma
            password: prisma
            migrations: true
            rawAccess: true
  postgres:
    image: postgres:10.5
    restart: always
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: prisma
      POSTGRES_PASSWORD: prisma
    volumes:
      - postgres:/var/lib/postgresql/data
volumes:
  postgres:
```

## Example

To create a new User through the native API, you can use the `executeRaw` mutation.

```graphql
mutation {
  executeRaw(query: "INSERT INTO default$default.\"User\" (Id, Name, \"updatedAt\",\"createdAt\") VALUES ('cjnkpvm0b000d0b22j7csr04v', 'Abhi', '2018-10-22T19:54:47.606Z', '2018-10-22T19:54:47.606Z');")
}
```

To find all Users

```graphql
mutation {
  executeRaw(query: "SELECT * FROM default$default.\"User\"")
}
```
