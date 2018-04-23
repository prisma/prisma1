---
alias: iquaecuj6b
description: Upgrade to 1.7
---

# Upgrade to 1.7

## Overview

The [1.7. release](https://github.com/graphcool/prisma/releases/tag/1.7.0) of Prisma introduces a few major changes for the deployment process of a Prisma API. These changes mainly concern the service configuration within [`prisma.yml`](!alias-ufeshusai8) and a few commands of the Prisma CLI.

All changes are **backwards-compatible**, meaning there is no necessity to incorporate the changes right away. In general, the CLI will help you perform the required changes automatically where possible.

There are two main cases for how the CLI is helping you with the migration:

- **Your API is deployed to a [Prisma Cloud](https://www.prisma.io/cloud) cluster**: The CLI _automatically_ adjusts `prisma.yml` and writes the new `endpoint` property into it (while removing `service`, `stage` and `cluster`).
- **Your API is _NOT_ deployed to a [Prisma Cloud](https://www.prisma.io/cloud) cluster**: The CLI prints a warning and provides hints how to perform the updates (see below for more info).

## `prisma.yml`

### New YAML structure

The service configuration inside `prisma.yml` is based on a new YAML structure (find the updated docs [here](https://www.prisma.io/docs/reference/service-configuration/prisma.yml/yaml-structure-ufeshusai8)):

- The `service`, `stage` and `cluster` properties have been removed.
- A new property called `endpoint` has been added. The new `endpoint` effectively encodes the information of the three removed properties.
- The `disableAuth` property has been removed. If you don't want your Prisma API to require authentication, simply omit the `secret` property.
- The `schema` property has been removed.

#### Example: Local deployment

As an example, consider this **outdated** version of `prisma.yml`:

```yml
service: myservice
stage: dev
cluster: local

datamodel: datamodel.graphql
```

After migrated to **Prisma 1.7.**, the file will have the following structure:

```yml
endpoint: http://localhost:4466/myservice/dev
datamodel: datamodel.graphql
```

### Introducing `default` service name and `default` stage

For convenience, two special values for the _service name_ and _stage_ parts of the Prisma `endpoint` have been introduced. Both values are called `default`. If not explicitly provided, the CLI will automatically infer them.

Concretely, this means that whenever the _service name_ and _stage_ are called `default`, you can omit them in the `endpoint` property of `prisma.yml`.

For example, `http://localhost:4466/default/default` can be written as `http://localhost:4466/` or `https://eu1.prisma.sh/public-helixgoose-752/default/default` can be written as `https://eu1.prisma.sh/public-helixgoose-752/`.

This is also relevant for the `import` and `export` endpoints of your API. Instead of `http://localhost:4466/default/default/import` you can now write `http://localhost:4466/import` or instead of `https://eu1.prisma.sh/public-helixgoose-752/default/default/export` you can write `https://eu1.prisma.sh/public-helixgoose-752/export`.

## Prisma CLI

### Deprecating `local` commands

The `prisma local` commands are being deprecated in favor of using Docker commands directly. `prisma local` provided a convenient abstraction for certain Docker workflows. In 1.7., everything related 

### Authenticating against Prisma servers running on Docker

### New 