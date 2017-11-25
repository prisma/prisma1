---
alias: aej3ne1eez
description: Graphcool features a simple yet powerful permission system that integrates seamlessly with the available authentication providers.
---

# Important Notes

* For create mutations, all scalar fields can be returned in the mutation payload without resulting in a permission error. This is independent of the permission setup.

> Note: this currently does not apply to the `createUser` mutation.

* Create or update mutations are only registered for a given permission, if one of the input arguments of the mutation is a  scalar field that the permission applies to. That's why a create or update mutation that only contains nested input arguments can never match any permission. So this will not work:

  ```graphql
  mutation {
    createComment(
      postId: "some-post-id"
    ) {
      id
    }
  }
  ```

  while this mutation will be registered by a permission that applies to the `dummy` field:

  ```graphql
  mutation {
    createComment(
      postId: "some-post-id", dummy: "dummy"
    ) {
      id
    }
  }
  ```
