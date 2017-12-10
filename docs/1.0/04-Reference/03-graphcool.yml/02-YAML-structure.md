---
alias: ufeshusai8
description: YAML Structure
---

# YAML Structure

## Overview

The service definition file `graphcool.yml` has the following root properties:

- [`types`](#types): References your type definition file(s).
- [`functions`](#functions): Defines all the [functions](!alias-aiw4aimie9)  you're using in your service.
- [`permissions`](#permissions): Defines all the permission rules for your service.
- [`rootTokens`](#root-tokens): Lists all the [root token](!alias-eip7ahqu5o#root-tokens) you've configured for your service.

> The exact structure of `graphcool.yml` is defined with [JSON schema](http://json-schema.org/). You can find the corresponding schema definition [here](https://raw.githubusercontent.com/graphcool/graphcool-json-schema/master/src/schema.json).
