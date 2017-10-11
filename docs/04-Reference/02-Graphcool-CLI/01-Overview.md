---
alias: zboghez5go
description: An overview of the Graphcool CLI.
---

# Overview

The Graphcool CLI is the primary tool to manage your Graphcool services. Generally, the shape and contents of a Graphcool service are determined by the service definition file [`graphcool.yml`](!alias-foatho8aip).

The CLI offers tools to modify the local version of your service definition and file structure as well as syncing these changes with already deployed services. 

## Using command line options

Most of the [CLI commands](!alias-aiteerae6l) accept specific options that you can provide. 

For each option that you provide, you can use either of the following two forms:

- **Long form**: Spell out the full name of the option prepended by _two_ dashes, e.g. `graphcool deploy --stage prod`.
- **Short form**: Take only a single letter of the option's name (most of the time, this is the very first letter) and prepend with only _one_ dash, e.g. `graphcool deploy -s prod`.