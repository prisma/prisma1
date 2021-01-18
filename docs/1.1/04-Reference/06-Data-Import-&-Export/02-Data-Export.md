---
alias: pa0aip3loh
description: Data Export
---

# Data Export

Exporting data can be done either using the CLI or the raw export API. In both cases, the downloaded data is formatted in JSON and adheres to the Normalized Data Format (NDF). As the exported data is in NDF, it can directly be imported into a service with an identical schema. This can be useful when test data is needed for a service, e.g. in a `dev` stage.

## Data export with the CLI

The Prisma CLI offers the `prisma1 export` command. It accepts one option:

- `--export-path` (short: `-e`): A file path to a .zip-directory which will be created by the CLI and where the exported data is stored

Under the hood, the CLI uses the export API that's described in the next section. However, using the CLI provides some major benefits:

- **leveraging the CLI's authentication mechanism** (i.e. you don't need to manually send your authentication token)
- **writing downloaded data directly to file system**
- **cursor management** in case multiple requests are needed to export all application data (when doing this manually you need to send multiple requests and adjust the cursor upon each)

### Output format

The data is exported in NDF and will be placed in three directories that are named after the different NDF types: `nodes`, `lists` and `relations`.

## Data export using the raw export API

The raw export API is exposed under the `/export` path of your service's HTTP endpoint. For example:

- `http://localhost:60000/my-app/dev/export`
- `https://database.prisma.sh/my-app/prod/export`

One request can download JSON data (in NDF) of at most 10 MB in size. Note that you need to provide your authentication token in the HTTP `Authorization` header of the request!

The endpoint expects a POST request where the body contains JSON with the following contents:

```json
{
  "fileType": "nodes",
  "cursor": {
    "table": 0,
    "row": 0,
    "field": 0,
    "array": 0
  }
}
```

The values in `cursor` describe the offsets in the database from where on data should be exported. Note that each response for an export request will return a new cursor with either of two states:

- Terminated (_not full_): If all the values for `table`, `row`, `field` and `array` are returned as `-1` it means the export has completed.
- Non-terminated (__full_): If any of the values for `table`, `row`, `field` or `array` is different from `-1`, it means the maximum size of 10 MB for this response has been reached. If this happens, you can use the returned `cursor` values as the input for your next export request.

### Example

Here is an example `curl` command for uploading some JSON data (of NDF type `nodes`):

```sh
curl 'http://localhost:60000/my-app/dev/export' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1MTM1OTQzMTEsImV4cCI6MTU0NTEzMDMxMSwiYXVkIjasd3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSIsIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjpbIk1hbmFnZXIiLCJQcm9qZWN0IEFkbWluaXN0cmF0b3IiXX0.L7DwH7vIfTSmuwfxBI82D64DlgoLBLXOwR5iMjZ_7nI' \
-d '{"fileType":"nodes","cursor":{"table":0,"row":0,"field":0,"array":0}}' \
-sSv
```

The generic version for `curl` (using placeholders) would look as follows:

```sh
curl '__SERVICE_ENDPOINT__/export' \
-H 'Content-Type: application/json' \
-H 'Authorization: Bearer __JWT_AUTH_TOKEN__' \
-d '{"fileType":"__NDF_TYPE__","cursor": {"table":__TABLE__,"row":__ROW__,"field":__FIELD__,"array":__ARRAY__}} }' \
-sSv
```
