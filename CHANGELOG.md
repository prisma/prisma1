# 1.4 (25.09.2017)

## Features

* [The system fields createdAt and updatedAt are now optional](https://github.com/graphcool/graphcool/issues/452).

## Changes

* [Permission queries are now validated upon creation](https://github.com/graphcool/graphcool/issues/456).
* [Valid function names are now restricted](https://github.com/graphcool/graphcool/issues/538).
> Valid function names only use up to 64 alphanumeric letters, dashes and underscores. This is only checked when creating a new or updating an existing function and does *not* affect existing functions before updating them.
* Renamed Permanent Authentication Tokens (PATs) to Root Tokens.
* [Schema Extensions are renamed to (Custom) Resolvers](https://github.com/graphcool/graphcool/issues/461).
* [Renaming relations requires usage of @rename directive with oldName parameter](https://github.com/graphcool/graphcool/issues/534).

## Bug Fixes

* [Fixed a bug that affected subscription queries using variables of type ID](https://github.com/graphcool/graphcool/issues/567).
* [Fixed a bug where using an invalid type in a resolver schema resulted in an internal server error](https://github.com/graphcool/graphcool/issues/413).
* [Fixed a bug when returning null for a string in a resolver](https://github.com/graphcool/graphcool/issues/559).

## CLI Beta

> Note: The CLI version 1.4 will be released at a later stage.

### Features

* [Wildcard permissions have been introduced that can be used to match all operations](https://github.com/graphcool/graphcool/issues/521). The project configuration of a new project created with the CLI includes the wildcard permission by default.
* [Added support for Modules in the project configuration](https://github.com/graphcool/graphcool/issues/523).
* [Environment variables can now be used for Graphcool Functions using the CLI](https://github.com/graphcool/graphcool/issues/548).
* [Added Root Token support in project configuration](https://github.com/graphcool/graphcool/issues/536).

### Changes

* [Ejected projects will not receive default public permissions for new fields/models](https://github.com/graphcool/graphcool/issues/459).
* [When deploying, subscription queries are now validated first](https://github.com/graphcool/graphcool/issues/464). Also see [here](https://github.com/graphcool/graphcool/issues/465).
* [User and File system types not included by default in projects created with the CLI](https://github.com/graphcool/graphcool/issues/151).

### Bug Fixes

* [504 and 502 error message](https://github.com/graphcool/graphcool/issues/458) and [other errors](https://github.com/graphcool/graphcool/issues/520) are now rendered correctly.
* [Scalar List Field are no longer updated unnecessarily during Deploy](https://github.com/graphcool/graphcool/issues/463).
* [Deploy is correctly aborted when errors occur](https://github.com/graphcool/graphcool/issues/540).
* [diff command works correctly for breaking changes](https://github.com/graphcool/graphcool/issues/557).



