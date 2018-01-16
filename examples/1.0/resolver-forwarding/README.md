# resolver-forwarding

## Examples

Run these examples against `https://localhost:4000`

```graphql
query a {
  posts {
    id
  }
}

mutation b {
  createPost(data: {
    isPublished: true
    text: "Nilan"
    title: "Nilan"
  }) {
    id
  }
}

mutation c {
  deletePost(id: "cjc0terkb00tw0179p3e2r7u3") {
    id
  }
}
```

### Questions

why is it called `db` on `forwardTo(`'db')`: because it is called like this in index.js
