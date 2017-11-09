Download the latest CLI version with `npm install -g graphcool`.

# 0.9 (2017-11-08)

## Server

* Fixed a bug when removing a field that was referenced by a permission https://github.com/graphcool/framework/issues/993

* Improved logging for local and shared cluster
  * https://github.com/graphcool/framework/issues/1108 https://github.com/graphcool/framework/issues/881 https://github.com/graphcool/framework/issues/947 https://github.com/graphcool/framework/issues/982

* General wording improvements, and better error messages
  * https://github.com/graphcool/framework/issues/952 https://github.com/graphcool/framework/issues/846 https://github.com/graphcool/framework/issues/922 https://github.com/graphcool/framework/issues/926 https://github.com/graphcool/framework/issues/929 https://github.com/graphcool/framework/issues/778 https://github.com/graphcool/framework/issues/951

* Implemented Webhook shorthand notation  https://github.com/graphcool/framework/issues/978

* Changing a service alias now invalidates the old one https://github.com/graphcool/framework/issues/741

* Local docker and dependency injection binding clean up
  * https://github.com/graphcool/framework/pull/1098
  * Removed RabbitMQ dependency for local clusters


* *Experimental* support for Docker Toolbox
  * https://github.com/graphcool/framework/issues/1074  https://github.com/graphcool/framework/issues/864
  * Please let us know [in the Forum](https://www.graph.cool/forum/c/feedback) if you still experience any issues with Docker Toolbox, so we can fully support it in a coming release.

## CLI

* General wording improvements
  * https://github.com/graphcool/framework/issues/936 https://github.com/graphcool/framework/issues/972 https://github.com/graphcool/framework/issues/924

* Unified usage when adding templates https://github.com/graphcool/framework/issues/754
  * Individual templates will receive better explanations shortly.

* Fixed endless deploy loop when environment variables are missing https://github.com/graphcool/framework/issues/966

## Playground

Version `1.1.1` has been [released](https://github.com/graphcool/graphql-playground/releases/tag/v1.1.1)

* Added middleware support
* Expose component for React 16
* Added serverless handler

# 0.8.1 (2017-10-22)

## CLI

* Improved importing npm modules:
  * https://github.com/graphcool/framework/issues/962
  * https://github.com/graphcool/framework/issues/997
* Improved error message and cosmetics:
  * https://github.com/graphcool/framework/issues/931
  * https://github.com/graphcool/framework/issues/921
  * https://github.com/graphcool/framework/issues/941
  * https://github.com/graphcool/framework/issues/917
  * https://github.com/graphcool/framework/issues/980
* The service definition for upgraded projects has been improved: https://github.com/graphcool/framework/issues/873

* `graphcool init`
  * Temporarily removed `--template` parameter https://github.com/graphcool/framework/issues/950

## Lib

* Version 0.1.4 of `graphcool-lib` has been released. Read the release notes [here](https://github.com/graphcool/graphcool-lib/releases).

# 0.8 (2017-10-20)

## Core

* [The function context has been streamlined for all function types](https://github.com/graphcool/framework/issues/219).
* [Fixed deploying a renamed function file](https://github.com/graphcool/framework/issues/896).
* [Fixed deploying operationBefore and operationAfter functions](https://github.com/graphcool/framework/issues/818).
* [A problem with subscription functions has been fixed](https://github.com/graphcool/framework/issues/835).
* [Functions are now updated instead of deleted and recreated](https://github.com/graphcool/framework/issues/451).
* [Permissions are only updated if necessary](https://github.com/graphcool/framework/issues/773).
* [The @model directive replaces 'implements Node'](https://github.com/graphcool/framework/issues/817).

* General error message improvements:

  * https://github.com/graphcool/framework/issues/854
  * https://github.com/graphcool/framework/issues/829
  * https://github.com/graphcool/framework/issues/592

## CLI

* [The CLI exists with code 1 on failure](https://github.com/graphcool/framework/issues/662) and [0 otherwise](https://github.com/graphcool/framework/issues/663).
* [You can now refer to single GraphQL operations in .graphql files](https://github.com/graphcool/framework/issues/669).
* [The CLI now supports a `GRAPHCOOL_TARGET` environment variable to set the target independently from `.graphcoolrc`](https://github.com/graphcool/framework/issues/626).
* [Docker running on port 60001 works as expected](https://github.com/graphcool/framework/issues/825).
* [Referring to modules in `graphcool.yml` is now detected, and you're asked to use templates instead](https://github.com/graphcool/framework/issues/841).
* All dependencies are explicitely specified now, as described [here](https://github.com/graphcool/framework/issues/875), [here](https://github.com/graphcool/framework/issues/895) and [here](https://github.com/graphcool/framework/issues/889).

* `graphcool init`
  * Improved output as described [here](https://github.com/graphcool/framework/issues/654) and [here](https://github.com/graphcool/framework/issues/869).
  * [Initial `package.json` does not refer to `index.js` anymore](https://github.com/graphcool/framework/issues/814).
  * [It's now possible to initialze a service in a non-empty folder](https://github.com/graphcool/framework/issues/857).

* `graphcool deploy`
  * [Errors are caught before showing the deploy wizard](https://github.com/graphcool/framework/issues/856).
  * [Deploying to not running local cluster is handled gracefully](https://github.com/graphcool/framework/issues/853).
  * [Introduced an error that detects missing node_modules when modules are required or imported in any function](https://github.com/graphcool/framework/issues/824). [Built-in modules are ignored](https://github.com/graphcool/framework/issues/831).
  * [Deploying also works when using symlinks](https://github.com/graphcool/framework/issues/891).
  * [Prevent accidental deploy](https://github.com/graphcool/framework/issues/660).
  * [Improved error message for missing schema file of resolver functions](https://github.com/graphcool/framework/issues/840).
  * [Updated environment variables are correctly deployed](https://github.com/graphcool/framework/issues/799).
  * [Fixed a problem with deploying when the parent directory contains a .graphcoolrc file](https://github.com/graphcool/framework/issues/855).

* `graphcool diff`
  * [Has been renamed to `graphcool deploy --dry-run`](https://github.com/graphcool/framework/issues/883).


* `graphcool add-template`
  * Error handling has been improved as described [here](https://github.com/graphcool/framework/issues/813) and [here](https://github.com/graphcool/framework/issues/819).

* `graphcool root-token`
  * [Improved instructions for obtaining the root token](https://github.com/graphcool/framework/issues/844).
  * [Fixed usage of help parameter](https://github.com/graphcool/framework/issues/935).
  * [Now _only_ prints the root token](https://github.com/graphcool/framework/issues/933).

* `graphcool invoke-local`
  * [Now works with non-promise functions](https://github.com/graphcool/framework/issues/708).
* `graphcool local`
  * [An issue that lead to graphcool local up freezing has been fixed](https://github.com/graphcool/framework/issues/766).

* `graphcool logs`
  * [Added a `target` parameter to obtain logs from specific target](https://github.com/graphcool/framework/issues/938).
  * [You can now obtain logs for all functions with one command](https://github.com/graphcool/framework/issues/645).

* `graphcool console`
  * [Added `target` parameter](https://github.com/graphcool/framework/issues/642).

* [`graphcool pull` has been permanently removed](https://github.com/graphcool/framework/issues/932).

* [`graphcool export` is temporarily removed](https://github.com/graphcool/framework/issues/957).

## Lib

* Version 0.1.3 of `graphcool-lib` has been released. Read the release notes [here](https://github.com/graphcool/graphcool-lib/releases).

# 0.7 (2017-10-13)

## Features 🎉

* [A new local development workflow for functions is available](https://github.com/graphcool/framework/issues/714). Additionally, there is a new & improved function runtime [when deploying a service to a remote cluster](https://github.com/graphcool/framework/issues/800), as well as in [the local `graphcool-dev` environment](https://github.com/graphcool/framework/issues/797).
* [You can now add templates to the service definition with a new add-template command](https://github.com/graphcool/framework/issues/720).

## Changes 💡

* [The initial project structure after upgrading a project to the Framework has been changed](https://github.com/graphcool/framework/issues/602).
* [Added GRAPHCOOL_PLATFORM_TOKEN env var](https://github.com/graphcool/framework/issues/753).
* [Removed the env command](https://github.com/graphcool/framework/issues/732). From now on `.graphcoolrc` is used to control deploy targets.
* [The CLI now can be run with node 6+](https://github.com/graphcool/framework/issues/777).

## Bug Fixes 🐛

* [The CLI now reauthenticates if an invalid session is found](https://github.com/graphcool/framework/issues/731).
* [Service aliases with dashes are now supported in the CLI](https://github.com/graphcool/framework/issues/616).
* [Fixed a problem with the confirmation when deleting projects](https://github.com/graphcool/framework/issues/735).


# 0.6 (2017-10-06)

## Features

* [Introducing Resolver Functions to extend your Graphcool API](https://github.com/graphcool/framework/issues/40) 🎉

## Bug Fixes

* [Fixed a bug that prevented relation queries for specific schemas](https://github.com/graphcool/framework/issues/718).

## Framework Preview

* [graphcool init does not deploy the service anymore](https://github.com/graphcool/framework/issues/706).
* Improved usage texts [in general](https://github.com/graphcool/framework/issues/639) and [for delete](https://github.com/graphcool/framework/issues/697).
* [Added divider for project list](https://github.com/graphcool/framework/issues/653).
* [Improved output for written files](https://github.com/graphcool/framework/issues/664).
* [Allow .graphcool file in current folder](https://github.com/graphcool/framework/issues/622).
* [Search project configuration in parent folders](https://github.com/graphcool/framework/issues/646).
* [Deleting projects now asks for configuration in all cases](https://github.com/graphcool/framework/issues/631).
* [Introduced a shorthand notation for function code handlers](https://github.com/graphcool/framework/issues/529).
* [Renamed get-root-tokens to root-tokens](https://github.com/graphcool/framework/issues/634).
* [Added basic account command](https://github.com/graphcool/framework/commit/d7c9074659889bf751c79657cde32b78d205137a).


Currently, permission queries can't contain a header section, which will be changed soon. More information [here](https://github.com/graphcool/framework/issues/703).

# 0.4 (2017-09-29)

## Changes

* [Valid function names are now restricted](https://github.com/graphcool/framework/issues/538).
> Valid function names only use up to 64 alphanumeric letters, dashes and underscores. This is only checked when creating a new or updating an existing function and does *not* affect existing functions before updating them.
* Renamed Permanent Authentication Tokens (PATs) to Root Tokens.
* [Renaming relations requires usage of @rename directive with oldName parameter](https://github.com/graphcool/framework/issues/534).
* [Schema Extensions are renamed to Resolvers](https://github.com/graphcool/framework/issues/461).

## Features

* [The system fields createdAt and updatedAt are now optional](https://github.com/graphcool/framework/issues/452).
* The payload type of a resolver is now taken into account when validating the payload. This applies to [required payload types](https://github.com/graphcool/framework/issues/558) as well as [list payload types](https://github.com/graphcool/framework/issues/435).

## Bug Fixes

* [Fixed a bug that affected subscription queries using variables of type ID](https://github.com/graphcool/framework/issues/567).
* [Fixed a bug that prevented default values from being deleted](https://github.com/graphcool/framework/issues/418).
* [Fixed a bug when changing a unique Int field to a unique String field](https://github.com/graphcool/framework/issues/429).
* [Fixed several issues when migrating float fields](https://github.com/graphcool/framework/issues/574).
* [Fixed a bug where using an invalid type in a resolver schema resulted in an internal server error](https://github.com/graphcool/framework/issues/413).
* [Fixed a bug that prevented scalar list input fields for resolvers](https://github.com/graphcool/framework/issues/568).
* [Fixed a bug when returning null for a string in a resolver](https://github.com/graphcool/framework/issues/559).
* [Fixed a bug when creating two types of same name a resolver](https://github.com/graphcool/framework/issues/420).

## Framework Preview

See the Forum for more information on the [Framework Preview](https://www.graph.cool/forum/t/feedback-new-cli-beta/949/1?u=nilan).

> **Note:** The latest CLI version of the Framework Preview is currently available in version 1.4. This will soon be corrected to version 0.4 instead.

* [Wildcard permissions have been introduced that can be used to match all operations](https://github.com/graphcool/framework/issues/521). The project configuration of a new project created with the CLI includes the wildcard permission by default.
* [Added support for Modules in the project configuration](https://github.com/graphcool/framework/issues/523).
* [Environment variables can now be used for Graphcool Functions using the CLI](https://github.com/graphcool/framework/issues/548).
* [Added Root Token support in project configuration](https://github.com/graphcool/framework/issues/536).
* [Projects will not receive default public permissions for new fields/models](https://github.com/graphcool/framework/issues/459).
* [When deploying, subscription queries are now validated first](https://github.com/graphcool/framework/issues/464). Also see [here](https://github.com/graphcool/framework/issues/465).
* [User and File system types not included by default in projects created with the CLI](https://github.com/graphcool/framework/issues/151).
* [The module command has been renamed to modules](https://github.com/graphcool/framework/issues/686).
* [The --version command is now available](https://github.com/graphcool/framework/issues/670).
* [Changes for diff and deploy are now better grouped](https://github.com/graphcool/framework/issues/526).
* [Fixed a bug for displaying 504 and 502 error messages](https://github.com/graphcool/framework/issues/458) and [other errors](https://github.com/graphcool/framework/issues/520).
* [Fixed a bug that caused unnecessary updates to scalar list fields when deploying](https://github.com/graphcool/framework/issues/463).
* [Fixed a bug that caused deploys to not being aborted when errors occured](https://github.com/graphcool/framework/issues/540).
* [Fixed a bug with  the diff command for breaking changes](https://github.com/graphcool/framework/issues/557).
* [Fixed a bug when renaming a type with relations and deploying](https://github.com/graphcool/framework/issues/564).
