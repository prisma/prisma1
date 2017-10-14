---
alias: zeiv8phail
description: Graphcool templates allow to easily add functionality to your project.
---

# Templates

## Overview

Templates are a quick and easy way to pull in predefined functionality into a Graphcool service.

There's a number of templates that are officially supported. You can find them [here](https://github.com/graphcool/templates).

A template is nothing but the definition of another Graphcool service, including a service definition file [`graphcool.yml`](!alias-foatho8aip) and potentially more files for functions or permissions. 

It is important to understand that templates are only a [lightweight and temporary solution](https://github.com/graphcool/graphcool/issues/720) for you to be able to quickly integrate predefined functionality into your Graphcool service. They are not meant to provide an elaborate package/module system like [npm](https://www.npmjs.com/) or other dependency management tools.


## Using a template

There are two ways how you can use a template in your project:

1. Manually download and copy the code from a template directory in the official [templates](https://github.com/graphcool/templates) repository. 
2. Use the `add-template` command from the [CLI](!alias-zboghez5go).


### Manually adding templates

The process of adding a template to a Graphcool service involves several steps. You're basically _merging_ your local service definition with the service definition of the template you want to use. In the end, you end up with only _one_ `graphcool.yml` as well as only _one_ `types.graphql`.

How to add a template manually:

1. Download the folder that contains the service definition of the template you want to use.
2. Copy that folder into the root directory of your Graphcool service.
3. Copy over the contents from the template's `graphcool.yml` into the `graphcool.yml` of your own service. Be sure to adjust any file references, e.g. source files that contain code for [functions](!alias-aiw4aimie9), if necessary.
4. Copy over the contents from the template's `types.graphql` into the `types.graphql` of your own service. 
5. Deploy your changes with `graphcool deploy`.


### Adding templates with the CLI

The `add-template` command in the Graphcool CLI basically automates the process of [manually adding templates](#manually-adding-templates). 

The only option that can be provided to this command is the path to the template on GitHub, e.g. for the [`email-password`](https://github.com/graphcool/modules)-template:

```sh
graphcool add-template graphcool/templates/authentication/email-password
```

When merging the template's `graphcool.yml` and `types.graphql` files with the ones from your local service definition, the CLI will only add the contents from the template files into your local files _as comments_. So you need to explicitly uncomment the parts form the template files that you actually want to use in your project.

The process for using the CLI to add a template thus looks as follows:

1. Use the `add-template <path>` CLI command and specify the `<path>` which points to the template's directory in the [Graphcool GitHub organization](https://github.com/graphcool).
2. Uncomment the lines in `graphcool.yml` and `types.graphql`.
5. Deploy your changes with `graphcool deploy`.

