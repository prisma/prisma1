# File Handling with Microsoft Aure Storage

This example demonstrates how to implement a GraphQL server with a **file handling API** based on [Azure Storage](https://azure.microsoft.com/en-us/services/storage/), Prisma & [graphql-yoga](https://github.com/graphcool/graphql-yoga).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). Th# File Handling with Microsoft Aure Storage

This example demonstrates how to implement a GraphQL server with a **file handling API** based on [Azure Storage](https://azure.microsoft.com/en-us/services/storage/), Prisma & [graphql-yoga](https://github.com/graphcool/graphql-yoga).

## Get started

> **Note**: `prisma` is listed as a _development dependency_ and _script_ in this project's [`package.json`](./package.json). This means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/file-handling-azure-storage
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd file-handling-azure-storage
yarn install
```

### 2. Setup your Azure storagee container & set environment variables

The S3 bucket will be the storage container for your files. Here is how you can create one:

1. Navigate to the [Azure Portal](https://portal.azure.com) in your browser.
1. When you're there, click **Create a new resource**.
    ![image_1] (https://www.dropbox.com/s/e6yf9wyyepd0wzb/first.PNG?dl=0)

2. Then select **storage** to load storage category and click **Storage account - blob, file, table, queue**.
    ![image_2](https://www.dropbox.com/s/w6e71u4in5k4xo6/second.PNG)

3. Fill the storage form and progress to seting up your azure storage
4. After creating a storage account click on **Access Keys** to get all your azure access keys needed.
    ![image_3](https://www.dropbox.com/s/3okrlm8oga3qimm/5.PNG?dl=0)
5. After getting the keys click on **overview**
    ![image_3](https://www.dropbox.com/s/7tnwrpj7b738cd2/4.PNG?dl=0)
5. Then click on **blob** to create a new container
6. Create a container and keep the name of your container
1. go to `src/modules/fileApi.ts` and change the container variable to the name of your container e.g. `const container = 'containerName';`

Next, you need to copy the values for the **Access key ID** and **Secret access key** into your GraphQL server project. They're _used_ in [`index.ts`](./src/index.ts), but referenced as environment variables which are defined in [`.env`](./.env).

For each environment variable in `.env` that's prefixed with `S3`, set the corresponding value by replacing the placeholder.

1. Replace `__AZURE_STORAGE_ACCOUNT__` with the storage account name, e.g. `AZURE_STORAGE_ACCOUNT="my-storage"`
1. Replace `__AZURE_STORAGE_ACCESS_KEY__` with the **key** of the storage account, e.g. `AZURE_STORAGE_ACCESS_KEY="AKIAJNO2FGOQAWD5PA3Qgg677ggh"`
1. Replace `__AZURE_STORAGE_CONNECTION_STRING__` optional!, with the **connection string** of the storage account, e.g. `AZURE_STORAGE_CONNECTION_STRING="43udl0YjEd0NtGXBALKsSN3anOCptivd7KP8c6BPgfdgff65vfhyf6vyvvgjvvyf6uyfvu65ydf6f5ytvjhftykjk=/lkjbuygh78g"`

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
curl -X POST 'http://localhost:3000/upload' -F "data=@/example.png; filename=coolimage.png"
```

This uploads the local file `example.png` and sets its `name` to `coolimage.png`. The response could look similar to this:

```json
[{
  "id": "cje29q6sgcand0141shsg72o3",
  "name": "coolimages.jpg",
  "secret": "07f736ee-bcea-42e6-8539-74c1a585213c",
  "contentType": "image/png",
  "size": 3805,
  "url": "https://gs3.blob.core.windows.net/unizonn/07f736ee-bcea-42e6-8539-74c1a585213c_coolimaggh.jpg?st=2018-02-26T12%3A07%3A22Z&se=2018-02-26T15%3A27%3A22Z&sp=r&sv=2015-12-11&sr=b&sig=CfRF3Mqwme3CFdVyq4lNMhp8jTj6ey2YvfriMvI2Hes%3D"
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

### Deleting a file

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
is means you can invoke the Prisma CLI without having it globally installed on your machine (by prefixing it with `yarn`), e.g. `yarn prisma deploy` or `yarn prisma playground`. If you have the Prisma CLI installed globally (which you can do with `npm install -g prisma`), you can omit the `yarn` prefix.

### 1. Download the example & install dependencies

Clone the Prisma monorepo and navigate to this directory or download _only_ this example with the following command:

```sh
curl https://codeload.github.com/graphcool/prisma/tar.gz/master | tar -xz --strip=2 prisma-master/examples/file-handling-azure-storage
```

Next, navigate into the downloaded folder and install the NPM dependencies:

```sh
cd auth
yarn install
```

### 2. Setup your Azure storagee container & set environment variables

The S3 bucket will be the storage container for your files. Here is how you can create one:

1. Navigate to the [Azure Portal](https://portal.azure.com) in your browser.
1. When you're there, click **Create a new resource**.
    ![image_1] (https://www.dropbox.com/s/e6yf9wyyepd0wzb/first.PNG?dl=0)

2. Then select **storage** to load storage category and click **Storage account - blob, file, table, queue**.
    ![image_2](https://www.dropbox.com/s/w6e71u4in5k4xo6/second.PNG)

3. Fill the storage form and progress to seting up your azure storage
4. After creating a storage account click on **Access Keys** to get all your azure access keys needed.
    ![image_3](https://www.dropbox.com/s/3okrlm8oga3qimm/5.PNG?dl=0)
5. After getting the keys click on **overview**
    ![image_3](https://www.dropbox.com/s/7tnwrpj7b738cd2/4.PNG?dl=0)
5. Then click on **blob** to create a new container
6. Create a container and keep the name of your container
1. Don't close the page, you will need the **Access key ID** and the **Secret access key** in the next step.

Next, you need to copy the values for the **Access key ID** and **Secret access key** into your GraphQL server project. They're _used_ in [`index.ts`](./src/index.ts), but referenced as environment variables which are defined in [`.env`](./.env).

For each environment variable in `.env` that's prefixed with `S3`, set the corresponding value by replacing the placeholder.

1. Replace `__AZURE_STORAGE_ACCOUNT__` with the storage account name, e.g. `AZURE_STORAGE_ACCOUNT="my-storage"`
1. Replace `__AZURE_STORAGE_ACCESS_KEY__` with the **key** of the storage account, e.g. `AZURE_STORAGE_ACCESS_KEY="AKIAJNO2FGOQAWD5PA3Qgg677ggh"`
1. Replace `__AZURE_STORAGE_CONNECTION_STRING__` optional!, with the **connection string** of the storage account, e.g. `AZURE_STORAGE_CONNECTION_STRING="43udl0YjEd0NtGXBALKsSN3anOCptivd7KP8c6BPgfdgff65vfhyf6vyvvgjvvyf6uyfvu65ydf6f5ytvjhftykjk=/lkjbuygh78g"`

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
curl -X POST 'http://localhost:3000/upload' -F "data=@/example.png; filename=coolimage.png"
```

This uploads the local file `example.png` and sets its `name` to `coolimage.png`. The response could look similar to this:

```json
[{
  "id": "cje29q6sgcand0141shsg72o3",
  "name": "coolimages.jpg",
  "secret": "07f736ee-bcea-42e6-8539-74c1a585213c",
  "contentType": "image/png",
  "size": 3805,
  "url": "https://gs3.blob.core.windows.net/unizonn/07f736ee-bcea-42e6-8539-74c1a585213c_coolimaggh.jpg?st=2018-02-26T12%3A07%3A22Z&se=2018-02-26T15%3A27%3A22Z&sp=r&sv=2015-12-11&sr=b&sig=CfRF3Mqwme3CFdVyq4lNMhp8jTj6ey2YvfriMvI2Hes%3D"
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

### Deleting a file

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
