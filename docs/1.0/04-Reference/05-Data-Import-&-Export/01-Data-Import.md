---
alias: ol2eoh8xie
description: Data Import
---

# Data Import

Data to be imported needs to adhere to the Normalized Data Format (NDF). As of today, the conversion from any concrete data source (like MySQL, MongoDB or Firebase) to NDF must be performed manually. In the [future](https://github.com/graphcool/framework/issues/1410), the Prisma CLI will support importing from these data sources directly.

Here is a general overview of the data import process:

```
+--------------+                    +----------------+                       +------------+
| +--------------+                  |                |                       |            |
| |            | |                  |                |                       |            |
| | SQL        | |  (1) transform   |      NDF       |  (2) chunked upload   |   Prisma   |
| | MongoDB    | | +--------------> |                | +-------------------> |            |
| | JSON       | |                  |                |                       |            |
| |            | |                  |                |                       |            |
+--------------+ |                  +----------------+                       +------------+
  +--------------+
```

As mentioned above, step 1 has to be performed manually. Step 2 can then be done by either using the raw import API or the `prisma import` command from the CLI.

> To view the current state of supported transformations in the CLI and submit a vote for the one you need, you can check out [this](https://github.com/graphcool/framework/issues/1410) GitHub issue.

When uploading files in NDF, you need to provide the import data split across three different _kinds_ of files:

- `nodes`: Data for individual nodes (i.e. databases records)
- `lists`: Data for a list of nodes
- `relations`: Data for related nodes

You can upload an unlimited number of files for each of these types, but each file should be at most 10 MB large.

## Data import with the CLI

The Prisma CLI offers the `prisma import` command. It accepts one option:

- `--data`  (short: `-d`): A file path to a .zip-directory containing the data to be imported

Under the hood, the CLI uses the import API that's described in the next section. However, using the CLI provides some major benefits:

- uploading **multiple files** at once (rather than having to upload each file individually)
- **leveraging the CLI's authentication mechanism** (i.e. you don't need to manually send your authentication token)
- ability to **pause and resume** an ongoing import
- **import from various data sources** like MySQL, MongoDB or Firebase (_not available yet_)

### Input format

When importing data using the CLI, the files containing the data in NDF need to be located in directories called after their type: `nodes`, `lists` and `relations`.

NDF files are JSON files following a specific structure, so each file containing import data needs to end on `.json`. When placed in their respective directories (`nodes`, `lists` or `relations`), the `.json`-files need to be numbered incrementally, starting with 1, e.g. `1.json`. The file name can be prepended with any number of zeros, e.g. `01.json` or `0000001.json`.

### Example

Consider the following file structure defining a Prisma service:

```
.
├── data
│   ├── lists
│   │   ├── 0001.json
│   │   ├── 0002.json
│   │   └── 0003.json
│   ├── nodes
│   │   ├── 0001.json
│   │   └── 0002.json
│   └── relations
│       └── 0001.json
├── data.zip
├── datamodel.graphql
└── prisma.yml
```

`data.zip` is the _compressed_ version of the `data` directory. Further, all files ending on `.json` are adhering to NDF. To import the data from these files, you can simply run the following command in the terminal:

```sh
prisma import --source data.zip
```

## Data import using the raw import API

The raw import API is exposed under the `/import` path of your service's HTTP endpoint. For example:

- `http://localhost:60000/my-app/dev/import`
- `https://database.prisma.sh/my-app/prod/import`

One request can upload JSON data (in NDF) of at most 10 MB in size. Note that you need to provide your authentication token in the HTTP `Authorization` header of the request!

Here is an example `curl` command for uploading some JSON data (of NDF type `nodes`):

```sh
curl 'http://localhost:60000/my-app/dev/import' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MTM1OTQzMTEsImV4cCI6MTU0NTEzMDMxMSwiYXVkIjasd3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.L7DwH7vIfTSmuwfxBI82D64DlgoLBLXOwR5iMjZ_7nI' \
-d '{"valueType":"nodes","values":[{"_typeName":"Model0","id":"0","a":"test","b":0,"createdAt":"2017-11-29 14:35:13"},{"_typeName":"Model1","id":"1","a":"test","b":1},{"_typeName":"Model2","id":"2","a":"test","b":2,"createdAt":"2017-11-29 14:35:13"},{"_typeName":"Model0","id":"3","a":"test","b":3},{"_typeName":"Model3","id":"4","a":"test","b":4,"createdAt":"2017-11-29 14:35:13","updatedAt":"2017-11-29 14:35:13"},{"_typeName":"Model3","id":"5","a":"test","b":5},{"_typeName":"Model3","id":"6","a":"test","b":6},{"_typeName":"Model4","id":"7"},{"_typeName":"Model4","id":"8","string":"test","int":4,"boolean":true,"dateTime":"1015-11-29 14:35:13","float":13.333,"createdAt":"2017-11-29 14:35:13","updatedAt":"2017-11-29 14:35:13"},{"_typeName":"Model5","id":"9","string":"test","int":4,"boolean":true,"dateTime":"1015-11-29 14:35:13","float":13.333,"createdAt":"2017-11-29 14:35:13","updatedAt":"2017-11-29 14:35:13"}]}' \
-sSv
```

The generic version for `curl` (using placeholders) would look as follows:

```sh
curl '__SERVICE_ENDPOINT__/import' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer __JWT_AUTH_TOKEN__' \
-d '{"valueType":"__NDF_TYPE__","values": __DATA__ }' \
-sSv
```
