---
alias: xai9quoo0i
description: The file management system allows you to upload, modify and delete files with the GraphQL APIs. Files will be directly available in your backend.
---

# Uploading Files

You can upload files with a size of up to 200MB to a project by doing a `multipart/form-data` HTTP request to the File API `https://api.graph.cool/file/v1/__PROJECT_ID__`.

It's important to use the form parameter `data` as seen in the examples below.

## Uploading Workflow

Everytime you upload a file to Graphcool, a new `File` node is created that contains information about that file:

* `id`: the [familiar system field](!alias-uhieg2shio#id-field)
* `secret`: a unique, unguessable secret that allows access to the file
* `name`: the file name
* `url`: the url of the file where it can be accessed. The url contains of the project id and the file `secret`, so is unguessable as well.
* `contentType`: the contentType of the file. It is determined based on the file name.

If you want to connect the `File` node to another node in a relation, you can use the `id` in the response.

## Using plain HTTP

<!-- GITHUB_EXAMPLE('File upload with fetch', 'https://github.com/graphcool-examples/react-graphql/tree/master/files-with-apollo') -->

With`curl` you could execute:

`curl -X POST 'https://api.graph.cool/file/v1/__PROJECT_ID__' -F "data=@example.png;filename=myname.png"`

This updates the local file `example.png` with the new name `myname.png`. The response could look something like this:

```JSON
{
  "secret": "__SECRET__",
  "name": "myname.png",
  "size": <omitted>,
  "url": "https://files.graph.cool/__PROJECT_ID__/__SECRET__",
  "id": <omitted>,
  "contentType": "image/png"
}
```

## File Upload and Permissions

File uploads using the File API are not governed by the permissions on the `File` type. As such, everyone can upload files to your project. Please reach out in the [Forum](https://graph.cool/forum) or [Slack](https://slack.graph.cool) if you have any questions about this.

## Current Limitations

* You can't upload files when you defined a required field on the `File` type.
