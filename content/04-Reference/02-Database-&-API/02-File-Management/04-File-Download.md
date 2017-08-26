---
alias: beim3teevi
description: The file management system allows you to upload, modify and delete files with the GraphQL APIs. Files will be directly available in your backend.
---

# File Download

Downloading a file can be done by querying the `url` of the `File` node that needs to be accessed.

## Using plain HTTP

To download a file, all you need to know is its url. If you have the id of the file, you can run the `File` query to fetch it. Then you could use curl to download it using

```sh
curl -O -J <your-file-url>
```

## File Download and Permissions

Files are secured using a unique and unguessable `secret`. You can protect this secret by using the read permissions on the `File` type as with any other type. For example, you can restrict access to a file's `secret` to authenticated users.

File downloads however are currently not governed by permissions on the `File` type. As such, everyone with a file's secret and the project id can download a file. Please reach out in the [Forum](https://graph.cool/forum) or [Slack](https://slack.graph.cool) if you have any questions about this.
