---
alias: eer4wiang0
description: The file management system allows you to upload, modify and delete files with the GraphQL APIs. Files will be directly available in your backend.
---

# File Management

<InfoBox type=warning>

The File API only works with [legacy Console projects](!alias-aemieb1aev) and is **disabled for [Graphcool services](!alias-opheidaix3)**.

</InfoBox>


## Overview

As part of our file management system you are able to

* [upload files](#uploading-files) that are up to 200 MB as shown in [the examples](#file-upload-examples)
* [download files](#file-download)
* [download and transform image files](#image-api)

Each project contains a [file type](!alias-eiroozae8u#file-type) by default that provides the possibility to add and modify files.

### File management with the client APIs

Currently, files cannot be uploaded from the [Simple API](!alias-abogasd0go) or the [Relay API](!alias-aizoong9ah) directly. Read [this issue on GitHub](https://github.com/apollographql/apollo/issues/65) for more information.


## Uploading files

You can upload files with a size of up to 200MB to a project by doing a `multipart/form-data` HTTP request to the File API `https://api.graph.cool/file/v1/__PROJECT_ID__`.

It's important to use the form parameter `data` as seen in the examples below.

### Uploading workflow

Everytime you upload a file to Graphcool, a new `File` node is created that contains information about that file:

* `id`: the [familiar system field](!alias-eiroozae8u#id-field)
* `secret`: a unique, unguessable secret that allows access to the file
* `name`: the file name
* `url`: the url of the file where it can be accessed. The url contains of the project id and the file `secret`, so is unguessable as well.
* `contentType`: the contentType of the file. It is determined based on the file name.

If you want to connect the `File` node to another node in a relation, you can use the `id` in the response.

### Using plain HTTP

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

### File upload and permissions

File uploads using the File API are not governed by the permissions on the `File` type. As such, everyone can upload files to your project. Please reach out in the [Forum](https://graph.cool/forum) or [Slack](https://slack.graph.cool) if you have any questions about this.

### Current limitations

You can't upload files when you defined a required field on the `File` type.


## File upload examples

Uploading a file will result in a response containing the new file id and the url. Let's upload our first file now. Pick a funny picture or similar and upload it! You can then go to the URL in the response to verify that the image file was correctly uploaded.

Remember to replace the file endpoint. You can copy your file endpoint from inside your project.

> Regardless of your HTTP library, make sure to **set the header `Content-Type` to `multipart/form-data`** and **use the file key `data`**.

### Javascript `fetch`

Using `fetch`, this is how we can upload a file

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

### AJAX

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

### Python with `requests`

An example using the Python module `requests` would look like:

```Python
import requests

url = 'https://api.graph.cool/file/v1/__PROJECT_ID__'

file = {'data': open('example.png', 'rb')}

r = requests.post(url, files=file)
```

### 2.1.4 `curl`

With `curl`, first navigate to the folder containing the file:

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


### Other methods

* `react-native-image-picker` on React Native - [example](https://github.com/graphcool/content/issues/90)
* remote file upload - [version 1](https://github.com/graphcool/content/issues/30) and [version 2](https://github.com/graphcool/content/issues/59)
* `axios` - [example](https://github.com/graphcool/content/issues/95)
* `ng2-file-upload` on Angular - [example](https://github.com/graphcool/content/issues/112)
* `react-dropzone` on React - [example](https://github.com/graphcool-examples/react-graphql/tree/master/files-with-apollo)
* you can drag-and-drop files into the sidebar of the Console to upload a file



## File download

Downloading a file can be done by querying the `url` of the `File` node that needs to be accessed.

### Using plain HTTP

To download a file, all you need to know is its url. If you have the id of the file, you can run the `File` query to fetch it. Then you could use curl to download it using

```sh
curl -O -J <your-file-url>
```

### File download and permissions

Files are secured using a unique and unguessable `secret`. You can protect this secret by using the read permissions on the `File` type as with any other type. For example, you can restrict access to a file's `secret` to authenticated users.

File downloads however are currently not governed by permissions on the `File` type. As such, everyone with a file's secret and the project id can download a file. Please reach out in the [Forum](https://graph.cool/forum) or [Slack](https://slack.graph.cool) if you have any questions about this.


## Image API

The Image API provides a very thin layer on top of the File API for image transformations.

Once an image has been calculated, it is persisted at the respective URL. Calling it again will not initiate another calculation. Only images up to 25MB can be transformed, and final results up to 6MB can be downloaded.

The base URL of a supported image uploaded through the File API is:

```
https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/
```

#### Resizing

* `https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/500x300`: Fit into 500px x 300px rectangle
* `https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/500x300!`: Forced resize
* `https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/500x`: Resize to 500px width maintaining aspect ratio
* `https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/x300`: Resize to 300px height maintaining aspect ratio

#### Cropping

* `https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/0x0:400x400`: Crops the image taking the first 400x400 square

> Cropping starts from left-top corner

#### Name

`https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/__IMAGE_NAME__.__EXTENSION__`: Name of image in URL to improve SEO
`https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/Graphcool.jpg`: Get image with name Graphcool.jpg 

Supported extensions: 

* png
* jpg
* jpeg
* svg
* gif
* bmp
* webp

#### Resizing, cropping and naming

`https://images.graph.cool/v1/__PROJECT_ID__/__FILE_SECRET__/0x0:400x400/250x250/Graphcool.jpg`: Crops the image taking the first 400x400 square and fit into 250px x 250px rectangle and rename it to Graphcool.jpg

> Images are cropped before being resized.

#### Contributions

Contributions and improvements to the Image API are welcome! For more information, see [serverless-image-proxy](https://github.com/graphcool/serverless-image-proxy).

