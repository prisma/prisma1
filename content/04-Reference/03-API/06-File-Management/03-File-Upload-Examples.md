---
alias: zaif4eing1
description: The file management system allows you to upload, modify and delete files with the GraphQL APIs. Files will be directly available in your backend.
---

# File Upload Examples

Uploading a file will result in a response containing the new file id and the url. Let's upload our first file now. Pick a funny picture or similar and upload it! You can then go to the url in the response to verify that the image file was correctly uploaded.

Remember to replace the file endpoint. You can copy your file endpoint from inside your project.

> Regardless of your http library, make sure to **set the header `Content-Type` to `multipart/form-data`** and **use the file key `data`**.

## fetch

Using fetch, this is how we can upload a file

```js
uploadFile = (files) => {
  let data = new FormData()
  data.append('data', files[0])

  // use the file endpoint
  fetch('https://api.graph.cool/file/v1/__PROJECT_ID__', {
    method: 'POST',
    body: data
  }).then(response => {
    return response.json()
  }).then(file => {
    const fileId = file.id
  })
}
```

## AJAX

With AJAX, you could have this file upload form:

```js
<form>
    <input type="file" id="file" name="file">
    <input type="submit">
</form>
```

Then you can upload the file like this:

```js
$( 'form' ).submit(function ( e ) {
  // prepare the file data
  var data = new FormData()
  data.append( 'data', $( '#file' )[0].files[0] )

  // do a post request
  var xhr = new XMLHttpRequest()
  xhr.open('POST', 'https://api.graph.cool/file/v1/__PROJECT_ID__', true)
  xhr.onreadystatechange = function ( response ) {
    // here you can obtain the new id of the uploaded file
  }
  xhr.send( data )
  e.preventDefault()
})
```

## Python with request

An example using the Python module `requests` would look like:

```Python
import requests

url = 'https://api.graph.cool/file/v1/__PROJECT_ID__'

file = {'data': open('example.png', 'rb')}

r = requests.post(url, files=file)
```

## 2.1.4 curl

With curl, first navigate to the folder containing the file:

```sh
cd ~/Downloads
```

You can then execute

```sh
curl -X POST 'https://api.graph.cool/file/v1/__PROJECT_ID__' -F "data=@example.png;filename=myname.png"
```

after replacing `__PROJECT_ID__` with your project id and `example.png` with the name of your file. The uploaded file will receive the new file name `myname.png`. You can copy your file endpoint from inside your project.

The response could look something like this:

```JSON
{
  "secret": "__SECRET__",
  "name": "example.png",
  "size": <omitted>,
  "url": "https://files.graph.cool/__PROJECT_ID__/__SECRET__",
  "id": <omitted>,
  "contentType": "image/png"
}
```


## Other Methods

* `react-native-image-picker` on React Native - [example](https://github.com/graphcool/content/issues/90)
* remote file upload - [version 1](https://github.com/graphcool/content/issues/30) and [version 2](https://github.com/graphcool/content/issues/59)
* `axios` - [example](https://github.com/graphcool/content/issues/95)
* `ng2-file-upload` on Angular - [example](https://github.com/graphcool/content/issues/112)
* `react-dropzone` on React - [example](https://github.com/graphcool-examples/react-graphql/tree/master/files-with-apollo)
* you can drag-and-drop files into the sidebar of the Console to upload a file
