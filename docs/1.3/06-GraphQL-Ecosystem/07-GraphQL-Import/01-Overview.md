---
alias: quaidah9pn
description: GraphQL Import is a package that allows importing &amp; exporting schema definitions in GraphQL SDL (also refered to as GraphQL modules).
---

# graphql-import

[`graphql-import`](https://github.com/graphcool/graphql-import) is a package that allows importing &amp; exporting schema definitions in GraphQL SDL (also refered to as GraphQL modules).

## Install

```sh
yarn add graphql-import
```

## Usage

```ts
import { importSchema } from 'graphql-import'
import { makeExecutableSchema } from 'graphql-tools'

const typeDefs = importSchema('schema.graphql')
const resolvers = {}

const schema = makeExecutableSchema({ typeDefs, resolvers })
```

## Example

Assume the following directory structure:

```
.
├── a.graphql
├── b.graphql
└── c.graphql
```

`a.graphql`

```graphql
# import B from "b.graphql"

type A {
  # test 1
  first: String
  second: Float
  b: B
}
```

`b.graphql`

```graphql
# import C from 'c.graphql'

type B {
  c: C
  hello: String!
}
```

`c.graphql`

```graphql
type C {
  id: ID!
}
```

Running `console.log(importSchema('a.graphql'))` procudes the following output:

```graphql
type A {
  first: String
  second: Float
  b: B
}

type B {
  c: C
  hello: String!
}

type C {
  id: ID!
}
```
