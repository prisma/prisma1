# passthrough

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
