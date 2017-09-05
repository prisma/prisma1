---
alias: eetai5meic
path: /docs/reference/simple-api/working-with-files
layout: REFERENCE
shorttitle: Working with files
description: You can interact with the file management system, by uploading or modifying  files through GraphQL queries and mutations exposed in the Simple API.
simple_relay_twin: aechiosh8u
tags:
  - simple-api
  - file-management
  - queries
  - mutations
related:
  further:
  more:
    - thaiph8ung
    - uo6uv0ecoh
---

# Working with files

To interact with the [file management](!alias-eer4wiang0) of the platform, you can create, rename or delete files through queries and mutations exposed in the Simple API.

## Uploading files

Uploading files with a GraphQL mutation is not supported yet. For now, use [the File API directly](!alias-eer4wiang0) to upload files.

## Reading meta information of files

To query the meta information stored for a file, use the `allFiles` or `File` queries.

To query a specific file use one of the unique fields `id`, `secret` or `url` fields to [specify the file node](!alias-ua6eer7shu):

```graphql
query {
  File(id: "my-file-id") {
    id
    name
  }
}
```

Similarly, the `allFiles` query can be used to query for [multiple file nodes](!alias-pa2aothaec).

## Renaming files

To rename a file, use the `updateFile` mutation and choose a new value for the `name` field:

```graphql
mutation {
  updateFile(
    id: "my-file-id"
    name: "new-comment-name.png"
  ) {
    file {
      id
      name
    }
  }
}
```

## Deleting files

To delete a file, use the `deleteFile` mutation as you would use any other delete mutation:

```graphql
mutation {
  deleteFile(id: "my-file-id") {
    file {
      id
    }
  }
}
```
