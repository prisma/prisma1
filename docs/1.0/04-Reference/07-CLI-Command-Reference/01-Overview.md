---
alias: eeb1oh24ec
description: Overview
---

# Overview

TODO N: final pass; finish commands #1330

## Using the Graphcool CLI

The Graphcool command line interface (CLI) is the primary tool to manage your Graphcool database services. Generally, the structure and behaviour of a Graphcool service are determined by the service definition file [`graphcool.yml`](!alias-foatho8aip).

The CLI offers tools to modify the local version of your service definition and file structure as well as syncing these changes with already deployed services.

## Using command line options

Most of the CLI commands accept specific arguments (also called _options_) you can provide when invoking the command.

For each option you provide, you can use either of the following two forms:

- **Long form**: Spell out the full name of the option prepended by _two_ dashes, e.g. `graphcool deploy --service prod`.
- **Short form**: Take only a single letter of the option's name (most of the time, this is the very first letter) and prepend with only _one_ dash, e.g. `graphcool deploy -s prod`.
