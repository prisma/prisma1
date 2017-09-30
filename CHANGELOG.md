# 0.4 (29.09.2017)

## Features

* [The system fields createdAt and updatedAt are now optional](https://github.com/graphcool/graphcool/issues/452).

## Changes

* [Valid function names are now restricted](https://github.com/graphcool/graphcool/issues/538).
> Valid function names only use up to 64 alphanumeric letters, dashes and underscores. This is only checked when creating a new or updating an existing function and does *not* affect existing functions before updating them.
* Renamed Permanent Authentication Tokens (PATs) to Root Tokens.
* [Renaming relations requires usage of @rename directive with oldName parameter](https://github.com/graphcool/graphcool/issues/534).

## Bug Fixes

* [Fixed a bug that affected subscription queries using variables of type ID](https://github.com/graphcool/graphcool/issues/567).
* [Fixed a bug that prevented default values from being deleted](https://github.com/graphcool/graphcool/issues/418).
* [Fixed a bug when changing a unique Int field to a unique String field](https://github.com/graphcool/graphcool/issues/429).
* [Fixed several issues when migrating float fields](https://github.com/graphcool/graphcool/issues/574).


## Resolver Beta

Read more information about the [Resolver Beta in the Forum](https://www.graph.cool/forum/t/feedback-schema-extensions-beta/405?u=nilan).

### Features

* The payload type of a resolver is now taken into account when validating the payload. This applies to [required payload types](https://github.com/graphcool/graphcool/issues/558) as well as [list payload types](https://github.com/graphcool/graphcool/issues/435).

### Changes

* [Schema Extensions are renamed to Resolvers](https://github.com/graphcool/graphcool/issues/461).

### Bug Fixes

* [Fixed a bug where using an invalid type in a resolver schema resulted in an internal server error](https://github.com/graphcool/graphcool/issues/413).
* [Fixed a bug that prevented scalar list input fields for resolvers](https://github.com/graphcool/graphcool/issues/568).
* [Fixed a bug when returning null for a string in a resolver](https://github.com/graphcool/graphcool/issues/559).
* [Fixed a bug when creating two types of same name a resolver](https://github.com/graphcool/graphcool/issues/420).

## CLI Beta

Read more information about the [CLI Beta in the Forum](https://www.graph.cool/forum/t/feedback-new-cli-beta/949?u=nilan).

> **Note:** The latest CLI beta version is currently available in version 1.4. This will soon be corrected to version 0.4 instead.

### Features

* [Wildcard permissions have been introduced that can be used to match all operations](https://github.com/graphcool/graphcool/issues/521). The project configuration of a new project created with the CLI includes the wildcard permission by default.
* [Added support for Modules in the project configuration](https://github.com/graphcool/graphcool/issues/523).
* [Environment variables can now be used for Graphcool Functions using the CLI](https://github.com/graphcool/graphcool/issues/548).
* [Added Root Token support in project configuration](https://github.com/graphcool/graphcool/issues/536).

### Changes

* [Ejected projects will not receive default public permissions for new fields/models](https://github.com/graphcool/graphcool/issues/459).
* [When deploying, subscription queries are now validated first](https://github.com/graphcool/graphcool/issues/464). Also see [here](https://github.com/graphcool/graphcool/issues/465).
* [User and File system types not included by default in projects created with the CLI](https://github.com/graphcool/graphcool/issues/151).
* [The module command has been renamed to modules](https://github.com/graphcool/graphcool/issues/686).
* [The --version command is now available](https://github.com/graphcool/graphcool/issues/670).
* [Changes for diff and deploy are now better grouped](https://github.com/graphcool/graphcool/issues/526).

### Bug Fixes

* [Fixed a bug for displaying 504 and 502 error messages](https://github.com/graphcool/graphcool/issues/458) and [other errors](https://github.com/graphcool/graphcool/issues/520).
* [Fixed a bug that caused unnecessary updates to scalar list fields when deploying](https://github.com/graphcool/graphcool/issues/463).
* [Fixed a bug that caused deploys to not being aborted when errors occured](https://github.com/graphcool/graphcool/issues/540).
* [Fixed a bug with  the diff command for breaking changes](https://github.com/graphcool/graphcool/issues/557).
* [Fixed a bug when renaming a type with relations and deploying](https://github.com/graphcool/graphcool/issues/564).
