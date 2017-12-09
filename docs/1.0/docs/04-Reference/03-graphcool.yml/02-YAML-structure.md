---
alias: ufeshusai8
description: YAML Structure
---

# YAML Structure

## Overview

The service definition file `graphcool.yml` has the following root properties:

- `service` (required): Name of the Graphcool service
- `datamodel` (required): Type definitions for database models, relations, enums and other types
- `stages`: List of different deployment targets ("stages")
- `secret`: Secret for securing the API endpoint
- `disableAuth`: Disable authentication for the endpoint
- `schema`: Path to the full schema file (typically called `database.graphql`)
- `subscriptions`: Configuration of subscription functions
- `custom`: Use to provide variables which can be referenced from other fields

> The exact structure of `graphcool.yml` is defined with [JSON schema](http://json-schema.org/). You can find the corresponding schema definition [here](https://github.com/graphcool/graphcool-json-schema/blob/master/src/schema.json).

## `service` (required)

The service defines the service name which will also be reflected in the endpoint for your service once it's deployed. The service name must follow these requirements:

- must contain only alphanumeric characters, hyphens and undersroces
- must start with an uppercase or lowercase letter
- must be at most 64 characters long

### Type

The `service` property expects a **string**.

### Examples

```yml
service: hello-world
```

```yml
service: My-DEMO_APP123
```

## `datamodel` (required)

The `datamodel` points to one or more `.graphql`-files containing type definitions written in the [SDL](https://blog.graph.cool/graphql-sdl-schema-definition-language-6755bcb9ce51).

### Type

The `datamodel` property expects a **string** or a **list of strings**.

### Examples

```yml
datamodel: types.graphql
```

```yml
datamodel:
  - types.graphql
  - enums.graphql
```

## `stages` (required)

The `stages` property defines a list of named deployment targets to which you can deploy your service.

### Type

The `stages` property expects an **object**. The keys in that object represent the names of the available stages, the values are the identifiers of the corresponding cluster (loaded from the global `.graphcoolrc`).

Note that the object has one special key called `default` which points at the default deployment target.

### Examples

```yml
stages:
  default: dev
  dev: local
  prod: london-cluster
```

Note that this example expects two entries in the global `.graphcoolrc` file called `local` and `london-cluster`.

## `disableAuth` (optional, default: `false`)

The `disableAuth` property indicates whether the Graphcool service requires authentication. If set to `true`, anyone who has access to the service's endpoint has full read/write-access to the database!

### Type

The `disableAuth` property expects a **boolean**.

### Examples

```yml
disableAuth: true
```

```yml
disableAuth: false
```

## `secret`

A secret is used to generate (_sign_) authentication tokens ([JWT](https://jwt.io)). If your Graphcool service requires authentication, one of these authentication tokens need to be attached to the HTTP request (in the `Authorization` header field). Note that it's possible to specify multiple secrets. Each secret must follow these requirements:

- must be [utf8](https://en.wikipedia.org/wiki/UTF-8) encoded
- must not contain spaces
- must be at most 256 characters long

The JWT sent to the API needs to meet the following conditions:

- must be signed with a `secret` configured for the service
- must contain an `exp` claim with a value in the future
- must contain a `service` claim with service and stage matching the current request
- must contain a `roles` claim that provides access to the current operation

### Type

The `secret` property expects a **string** (no list of strings). If you want to specify multiple secrets, you need to provide them as a comma-separated list (spaces are ignored).

### Examples

```yml
secret: moo4ahn3ahb4phein1eingaep
```

```yml
secret: myFirstSecret,     SECRET_NUMBER_2,3rd-secret
```

> Note: In this example, you specify three secrets: `myFirstSecret`, `SECRET_NUMBER_2` and `3rd-secret`. The spaces before the second secret are ignored.

```yml
secret: ${env:MY_SECRET}
```