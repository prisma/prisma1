---
alias: zboghez5go
description: An overview of the Graphcool CLI.
---

# Overview

The Graphcool CLI is the primary tool to manage your Graphcool services. Generally, the shape and contents of a Graphcool service are determined by the service definition file [`graphcool.yml`](!alias-foatho8aip).

The CLI offers tools to modify the local version of your service definition and file structure as well as syncing these changes with already deployed services. 

## Using command line options

Most of the [CLI commands](!alias-aiteerae6l) accept specific arguments (_options_) that you can provide when invoking the command. 

For each option that you provide, you can use either of the following two forms:

- **Long form**: Spell out the full name of the option prepended by _two_ dashes, e.g. `graphcool deploy --target prod`.
- **Short form**: Take only a single letter of the option's name (most of the time, this is the very first letter) and prepend with only _one_ dash, e.g. `graphcool deploy -t prod`.

## The `GRAPHCOOL_TARGET` environment variable

Almost all CLI commands related to [service management](!alias-aiteerae6l#service-management) accept the `--target` option. If this option is not provided, the `default` target from your local [`.graphcoolrc`](!alias-zoug8seen4) will be used.

It is possible to override the `default` option using an environment variable called `GRAPHCOOL_TARGET`:

```bash
export GRAPHCOOL_TARGET=shared-eu-west-1/cj8vn13me01er0147a280yhwn
graphcool deploy # runs without having a .graphcoolrc or overrides its `default` entry
``` 
