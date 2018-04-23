---
alias: iquaecuj6b
description: Update to 1.7
---

# Update to 1.7

## Overview

The [1.7. release](https://github.com/graphcool/prisma/releases/tag/1.7.0) of Prisma introduces a few major changes for the deployment process of a Prisma API. These changes mainly concern the service configuration within [`prisma.yml`](!alias-ufeshusai8) and a few commands of the Prisma CLI.

All changes are **backwards-compatible**, meaning there is no necessity to incorporate the changes right away. In general, the CLI will help you perform the required changes.

There are two cases for how the CLI is helping you with the migration:

- **Your API is deployed to a [Prisma Cloud](https://www.prisma.io/cloud) cluster**: The CLI _automatically_ adjusts `prisma.yml` and writes the new `endpoint` property into it (while removing `service`, `stage` and `cluster`).
- **Your API is _NOT_ deployed to a [Prisma Cloud](https://www.prisma.io/cloud) cluster**: The CLI prints a warning and provides hints how to perform the updates (see below for more info).

## `prisma.yml`

### New YAML structure

The service configuration inside `prisma.yml` is based on a new [YAML structure](https://www.prisma.io/docs/reference/service-configuration/prisma.yml/yaml-structure-ufeshusai8):

- The `service`, `stage` and `cluster` properties have been removed.
- A new property called `endpoint` has been added. The new `endpoint` effectively encodes the information of the three removed properties.
- The `disableAuth` property has been removed. If you don't want your Prisma API to require authentication, simply omit the `secret` property.

### Example: Local deployment

As an example, consider this **outdated** version of `prisma.yml`:

```yml
service: myservice
stage: dev
cluster: local

datamodel: datamodel.graphql
```

After migrated to Prisma **1.7.**, the file will have the following structure:

```yml
endpoint: http://localhost:4466/myservice/dev
datamodel: datamodel.graphql
```

### Introducing `default` service name and `default` stage


## Prisma CLI
