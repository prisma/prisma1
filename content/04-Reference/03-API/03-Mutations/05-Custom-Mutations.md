---
alias: thiele0aop
description: Custom mutations can be added to your GraphQL API using Schema Extensions.
---

# Custom Mutations

Custom mutations can be added to your GraphQL API using [Schema Extensions](!alias-xohbu7uf2e).

You can define the **name, input arguments and payload of the mutation** and **resolve it with a Graphcool Function**.

## Example

> Return a random number in a specified range

Schema Extension SDL document:

```graphql
type RandomNumberPayload {
  number: Float!
}

extend type Mutation {
  randomNumber(min: Int!, max: Int!): RandomNumberPayload
}
```

Graphcool Function:

```js
module.exports = function randomNumber(event) {
  const min = event.data.min
  const max = event.data.max

  if (min > max) {
    return {
      error: "Invalid input"
    }
  }

  const number = Math.random() * (max - min) + min

  return {
    data: {
      number
    }
  }
}
```

Then the mutation can be called like this using the Simple API:

```graphql
mutation {
  isValidAge(age: 12) {
    isValid # false
    age # 12
  }
}
```

Note that the returned object contains a `data` key, which in turn contains the `number` field that was specified in the `RandomNumberPayload` in the SDL document. [Error handling](!alias-quawa7aed0) works similarly to other Graphcool Functions, if an object containing the `error` key is returned.
