---
alias: quaidah9ph
description: GraphQL Bindings are modular building blocks that allow to embed existing GraphQL APIs into your own GraphQL server.
---

# graphql-binding

🔗 [`graphql-binding`](https://github.com/graphcool/graphql-binding/) is a package that simplifies the process of creating your own GraphQL bindings. GraphQL bindings are **modular building blocks** that allow to embed existing GraphQL APIs into your own GraphQL server. Think about it as turning (parts of) GraphQL APIs into reusable LEGO building blocks.

> The idea of GraphQL bindings is introduced in detail in this blog post: [Reusing & Composing GraphQL APIs with GraphQL Bindings](https://blog.graph.cool/80a4aa37cff5)

## Install

```sh
yarn add graphql-binding
```

## API

### Binding

#### constructor

```ts
constructor(options: BindingOptions): Binding
```

[`BindingOptions`](./src/types.ts#L27) has the following properties:

| Key | Required |  Type | Default | Note |
| ---  | --- | --- | --- | --- |
| `schema` | Yes | `GraphQLSchema` |  - | The executable [GraphQL schema](https://blog.graph.cool/ac5e2950214e) for binding  |
| `fragmentReplacements` | No | `FragmentReplacements` |  `{}` | A list of GraphQL fragment definitions, specifying fields that are required for the resolver to function correctly |
| `before` | No | `() => void` |  `(() => undefined)` | A function that will be executed before a query/mutation is sent to the GraphQL API |
| `handler` | No | `any` |  `null` | The [`handler`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Proxy/handler) object from [JS Proxy](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Proxy) |
| `subscriptionHandler` | No | `any` |  `null` | ... |

#### query & mutation

```ts
binding.query.<rootField>: QueryMap<any> // where <rootField> is the name of a field on the Query type in the mapped GraphQL schema
binding.mutation.<rootField>: QueryMap<any> // where <rootField> is the name of a field on the Mutation type in the mapped GraphQL schema
```

A `binding` object exposes two properties which can be used to send queries and mutations to the API: `binding.query` and `binding.mutation`.

Both are of type [`QueryMap`](src/types.ts#L11) and will expose methods that are named after the schema's root fields on the `Query` and `Mutation` types.

These methods take three arguments:

| Name | Required |  Type | Note |
| ---  | --- | --- | --- |
| `args` | No | `[key: string]: any` |  An object that contains the arguments of the root field |
| `context` | No | `[key: string]: any` |  The `context` object that's passed down the GraphQL resolver chain; every resolver can read from and write to that object |
| `info` | No | `GraphQLResolveInfo` &#124; `string` |  The `info` object (which contains an AST of the incoming query/mutation) that's passed down the GraphQL resolver chain or a string containing a [selection set](https://medium.com/front-end-developers/graphql-selection-sets-d588f6782e90) |

##### Example

Assume the following schema:

```graphql
type Query {
  user(id: ID!): User
}

type Mutation {
  createUser(): User!
}
```

If there is a `binding` for a GraphQL API that implements this schema, you can invoke the following methods on it:

```js
binding.query.user({ id: 'abc' })
binding.mutation.createUser()
```

When using the binding in a resolver implementation, it can be used as follows:

```js
findUser(parent, args, context, info) {
  return binding.user({ id: args.id }, context, info)
}

newUser(parent, args, context, info) {
  return binding.createUser({}, context, info)
}
```

#### subscription

```ts
binding.subscription.<rootField>(...):  AsyncIterator<any> | Promise<AsyncIterator<any>> // where <rootField> is the name of a field on the Subscription type in the mapped GraphQL schema
```

The `binding.subscription` property follows the same idea as `query` and `mutation`, but rather than returning a single value using a `Promise`, it returns a _stream_ of values using `AsyncIterator`.

 It is of type [`SubscriptionMap`](./src/types.ts#L19) and will expose methods that are named after the schema's root fields on the `Subscription` type.

 These methods take same three arguments as the generated methods on `query` and `mutation`, see above for the details.

## Examples

### Minimal example

```js
const { makeExecutableSchema } = require('graphql-tools')
const { Binding } = require('graphql-binding')

const users = [
  {
    name: 'Alice',
  },
  {
    name: 'Bob',
  },
]

const typeDefs = `
  type Query {
    findUser(name: String!): User
  }
  type User {
    name: String!
  }
`

const resolvers = {
  Query: {
    findUser: (parent, { name }) => users.find(u => u.name === name),
  },
}

const schema = makeExecutableSchema({ typeDefs, resolvers })

const findUserBinding = new Binding({
  schema,
})

findUserBinding.findUser({ name: 'Bob' })
  .then(result => console.log(result))
```

## Public GraphQL bindings

You can find practical, production-ready examples here:

- [`graphql-binding-github`](https://github.com/graphcool/graphql-binding-github)
- [`graphcool-binding`](https://github.com/graphcool/graphcool-binding)

> Note: If you've created your own GraphQL binding based on this package, please add it to this list via a PR 🙌

If you have any questions, share some ideas or just want to chat about GraphQL bindings, join the [`#graphql-bindings`](https://graphcool.slack.com/messages/graphql-bindings) channel in our [Slack](https://slack.graph.cool/).
