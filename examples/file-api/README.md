# GraphQL Server File Upload Example

This example illustrates the implementation of File API with GraphQL Server pattern. 

## Getting Started

### Initializing the Prisma Database Service
```sh
prisma deploy # choose local cluster
# copy API endpoint into the `PRISMA_ENPOINT` env var in .env
```

### Setting up the S3 bucket
1. Head over to the [AWS console](http://console.aws.amazon.com/) and navigate to the `S3` section.
2. Click create bucket and follow the instructions on screen.
3. Once you have created a `bucket`, add bucket name that you've picked to .env `S3_BUCKET` property.
4. Head back to the AWS Console and open `Identity and Access Management (IAM)` [console](https://console.aws.amazon.com/iam). Navigate to `Users` and click `Add user`.
5. Under _Access type_ check **Programmatic access** and press `Next`. From options, select _Attach existing policies directly_ and a table below will open. Search for **AmazonS3FullAccess** and check it. Press `Next` to review everything and submit by pressing `Create user`.
6. Once done, copy the _Access key ID_ to .env `S3_KEY` property and _Secret access key_ to .env `S3_SECRET` property.
7. You are all set to start the server!

### Starting the Server

```sh
yarn install
yarn start
# Open http://localhost:4000/
```

## Uploading files

You can upload files to a project by doing a `multipart/form-data` HTTP request to the File API `http://localhost:5000/upload`.

It's important to use the form parameter `data` as seen in the example below.

### Uploading workflow

Everytime you upload a file to Prisma, a new `File` node is created that contains information about that file.

* `id`: the [familiar system field](!alias-eiroozae8u#id-field)
* `secret`: a unique, unguessable secret that allows access to the file
* `name`: the file name
* `size`: the file size
* `url`: the url of the file where it can be accessed. The url contains of the project id and the file `secret`, so is unguessable as well.
* `contentType`: the contentType of the file. It is determined based on the file name (extension in the name is required!).

If you want to connect the `File` node to another node in a relation, you can use the `id` in the response.

With `curl` you could execute:

`curl -X POST 'http://localhost:5000/upload' -F "data=@example.png; filename=coolimage.png"`

This uploads the local file `example.png` under `coolimage.png` name. The response could look something like this:

```JSON
[{
  "id": "cjbqvp4ii00390181b1q0dq6h",
  "name": "coolimage.png",
  "secret": "43de4b08-78b2-4b5c-a5b7-05ee350ee09a",
  "contentType": "image/png",
  "size": 36625,
  "url": "https://__S3_BUCKET__.s3-eu-west-1.amazonaws.com/43de4b08-78b2-4b5c-a5b7-05ee350ee09a"
}]
```

> If there's no filename provided, the original name of the file is used instead.

## License
MIT
