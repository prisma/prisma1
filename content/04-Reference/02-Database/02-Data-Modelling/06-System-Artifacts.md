---
alias: uhieg2shio
path: /docs/reference/schema/system-artifacts
layout: REFERENCE
description: In order to make the platform as seamless and integrated as possible, certain predefined artifacts are defined for each project.
---

# System Artifacts

In order to make the platform as seamless and integrated as possible, we introduced some predefined artifacts in each project. These artifacts are designed to be as minimal as possible and cannot be deleted. At the moment there are two type of artifacts: *system types* and *system fields*.

## `User` Type

Every project has a system type called `User`. As the `User` type is the foundation for our [built-in authentication system](!alias-wejileech9) you cannot delete it. But of course you can still extend the `User` type to suit your needs and it behaves like every other type.

Apart from the predefined system fields, the `User` type can have additional system fields depending on the configured [custom authentication](!alias-seimeish6e).

You can add additional [fields](!alias-teizeit5se) as with any other type.

## `File` Type

The `File` type is part of our [file management](!alias-eer4wiang0). Every time you upload a file, a new `File` node is created. Aside from the predefined system fields, the `File` type contains several other fields that contain meta information:
* `contentType: `: our best guess as to what file type the file has. For example `image/png`. Can be `null`
* `name: String`: the complete file name including the file type extension. For example `example.png`.
* `secret: String`: the file secret. Can be combined with your project id to get the file url. Everyone with the secret has access to the file!
* `size: Integer`: the file size in bytes.
* `url: String`: the file url. Looks something like `https://files.graph.cool/__PROJECT_ID__/__SECRET__`, that is the generic location for files combined with your project id endpoint and the file secret.

You can add additional [fields](!alias-teizeit5se) as with any other type, but they need to be optional.

## `id` Field

Every type has a [required](!alias-teizeit5se#required) system field with the name `id` of type [ID](!alias-teizeit5se#id). The `id` value of every node (regardless of the type) is globally unique and unambiguously identifies a node ([as required by Relay](https://facebook.github.io/relay/docs/graphql-object-identification.html)). You cannot change the value for this field.

## `createdAt` and `updatedAt` Fields

Every type has the [DateTime](!alias-teizeit5se#datetime) fields `createdAt` and `updatedAt` that will be set automatically when a node is created or updated. You cannot change the values for these fields.
