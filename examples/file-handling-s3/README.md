# File Handling with AWS S3

This example demonstrates how to implement a GraphQL server with a **file handling API** based on [AWS S3](https://aws.amazon.com/s3/), Prisma & [graphql-yoga](https://github.com/graphcool/graphql-yoga).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/file-handling-s3
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd auth
yarn install
```

### 2. Setup your AWS S3 file bucket & set environment variables

The S3 bucket will be the storage container for your files. Here is how you can create one:

1. Navigate to the [AWS S3 console](https://s3.console.aws.amazon.com/s3/) in your browser.
1. When you're there, click **Create bucket**.
1. Enter a DNS-compliant bucket name, e.g. `com.johndoe.mygraphqlbucket`. Then click through the remaining dialogs without any actions. On the last page, click **Create bucket**.
  ![](https://imgur.com/AAb7WnH.png)
1. Now, navigate to the [Identity and Access Management (IAM) Console](https://console.aws.amazon.com/iam).
1. Select **Users** in the left side-menu, then click the blue **Add user** button on top of the page.
1. Provide a name for the user in the **User name** text field and check the **Programmatic access** checkbox in the **Access type** section.
1. Click the **Next: Permissions** button to move on.
1. From the permission options, select **Attach existing policies directly**.
1. In the serch field that appeared, search for **AmazonS3FullAccess** and then check its checkbox.
1. Click the **Next: Review** button to move on.
1. Verify the user to be created looks similar to this (the **User name** might of course be different):
  ![](https://imgur.com/bpVq2cc.png)
1. Click **Create user** to finally create the user.
1. Don't close the page, you will need the **Access key ID** and the **Secret access key** in the next step.

Next, you need to copy the values for the **Access key ID** and **Secret access key** into your GraphQL server project. They're _used_ in [`index.ts`](./src/index.ts), but referenced as environment variables which are defined in [`.env`](./.env).

For each environment variable in `.env` that's prefixed with `S3`, set the corresponding value by replacing the placeholder.

1. Replace `__S3_BUCKET__` with the name of the bucket you created, e.g. `S3_BUCKET="com.johndoe.mygraphqlbucket"`
1. Replace `__S3_ACCESS_KEY_ID__` with the **Access key ID** of the user you created, e.g. `S3_ACCESS_KEY_ID="AKIAJNO2FGOQAWD5PA3Q"`
1. Replace `__S3_SECRET_ACCESS_KEY__` with the **Secret access key** of the user you created, e.g. `S3_SECRET_ACCESS_KEY="43udl0YjEd0NtGXBALKsSN3anOCptivd7KP8c6BP"`

### 3. Deploy the Prisma database service

You can now [deploy](https://www.prismagraphql.com/docs/reference/cli-command-reference/database-service/prisma-deploy-kee1iedaov) the Prisma service (note that this requires you to have [Docker](https://www.docker.com) installed on your machine - if that's not the case, follow the collapsed instructions below the code block):

```sh
yarn prisma deploy
```

<details>
 <summary><strong>I don't have <a href="https://www.docker.com">Docker</a> installed on my machine</strong></summary>

To deploy your service to a public cluster (rather than locally with Docker), you need to perform the following steps:

1. Remove the `cluster` property from `prisma.yml`.
1. Run `yarn prisma deploy`.
1. When prompted by the CLI, select a public cluster (e.g. `prisma-eu1` or `prisma-us1`).
1. Set the value of the `PRISMA_ENDPOINT` environment variable in [`.env`](./.env#L2) to the HTTP endpoint that was printed after the previous command.

</details>

### 3. Start the GraphQL server

The Prisma database service that's backing your GraphQL server is now available. This means you can now start the server:

```sh
yarn start
```

The server is now running on [http://localhost:4000](http://localhost:4000). Notice that you can upload files by sending a `multipart/form-data` HTTP POST request to `http://localhost:4000/upload` endpoint.

## Testing the API

### Uploading files

As mentioned above, you can upload files by sending a `multipart/form-data` HTTP POST request to `http://localhost:4000/upload` endpoint. It is important to use the form parameter `data` as seen in the example below.

#### Uploading workflow

To upload a file that's available on the path `/Users/johndoe/example.png` on your machine, you can execute the following `curl` command in the terminal:

```sh
curl -X POST 'http://localhost:5000/upload' -F "data=@/Users/johndoe/example.png; filename=coolimage.png"
```

This uploads the local file `example.png` and sets its `name` to `coolimage.png`. The response could look similar to this:

```json
[{
  "id": "cjbqvp4ii00390181b1q0dq6h",
  "name": "coolimage.png",
  "secret": "43de4b08-78b2-4b5c-a5b7-05ee350ee09a",
  "contentType": "image/png",
  "size": 36625,
  "url": "https://com.johndoe.mygraphqlbucket.s3-eu-west-1.amazonaws.com/43de4b08-78b2-4b5c-a5b7-05ee350ee09a"
}]
```

> If there's no filename provided, the original name of the file is used instead.

#### File properties

Everytime you upload a file to Prisma, a new [`File`](./database/datamodel.graphql) node is created that contains information about the uploaded file.

* `id`: A globally unique ID for the file in the Prisma database.
* `secret`: A unique, unguessable secret that allows access to the file (generated by the [`uuid`](./package.json#L21) library when the file was uploaded).
* `name`: The file name.
* `size`: The file size.
* `url`: The S3 URL of the file where it can be accessed. The URL contains the bucket name as well as the file `secret`.
* `contentType`: The content type of the file (e.g. `image/png` or `image/png`). It is determined based on the file name (extension in the name is required).

### Open a Playground

The easiest way to test the GraphQL API is by using a [GraphQL Playground](https://github.com/graphcool/graphql-playground).

You can either start the [desktop app](https://github.com/graphcool/graphql-playground) via

```sh
yarn playground
```

Or you can open a Playground by navigating to [http://localhost:4000](http://localhost:4000) in your browser.

### Retrieve file info

Once the Playground is open, you can send the following query to retrieve information about all files:

```graphql
{
  files {
    id
    secret
    name
    size
    url
    contentType
  }
}
```

You can also retrieve a single file by providing an `id` to the `file` query:

```graphql
{
  file(id: "cjbqvp4ii00390181b1q0dq6h") {
    id
    secret
    name
    size
    url
    contentType
}
```

### Renaming a file

You can rename a file with the `renameFile` mutation:

```graphql
mutation {
  renameFile(
    id: "cjbqvp4ii00390181b1q0dq6h"
    name: "awesomeimage.png"
  ) {
    id
  }
}
```

### Renaming a file

You can delete a file with the `deleteFile` mutation:

```graphql
mutation {
  deleteFile(
    id: "cjbqvp4ii00390181b1q0dq6h"
  ) {
    id
  }
}
```

## Troubleshooting

<details>
 <summary>I'm getting the error message <code>[Network error]: FetchError: request to http://localhost:4466/file-handling-s3-example/dev failed, reason: connect ECONNREFUSED</code> when trying to send a query or mutation</summary>

This is because the endpoint for the Prisma service is hardcoded in [`index.js`](index.js#L23). The service is assumed to be running on the default port for a local cluster: `http://localhost:4466`. Apparently, your local cluster is using a different port.

You now have two options:

1. Figure out the port of your local cluster and adjust it in `index.js`. You can look it up in `~/.prisma/config.yml`.
1. Deploy the service to a public cluster. Expand the `I don't have Docker installed on my machine`-section in step 2 for instructions.

Either way, you need to adjust the `endpoint` that's passed to the `Prisma` constructor in `index.js` so it reflects the actual cluster domain and service endpoint.

</details>

## License

MIT
