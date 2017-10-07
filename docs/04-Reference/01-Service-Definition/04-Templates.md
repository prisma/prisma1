---
alias: zeiv8phail
description: Graphcool templates allow to easily add functionality to your project.
---

# Templates

## Overview

A Graphcool project can include a number of _modules_ that contain additional functionality. A module is nothing but another Graphcool project with its own project definition and file structure.

When adding a module to an existing project, the files that belong to the module need to be placed in the project under a directory called `modules`. Additionally, the module needs to be added to the `modules` list in the project definition.

Modules are primarily are tool for _code organization_ that you can use to improve the structure of your project and make it more modular. 

## Predefined modules

Graphcool provides a number of predefined modules that you can pull into your project. The predefined modules are located in the [`modules`](https://github.com/graphcool/modules) repository of the `graphcool` GitHub organization.

However, any GitHub repository (or any directory inside a repository) that contains a Graphcool project definition and the corresponding files qualifies as a module. 

## Managing modules

Like environments, modules can be managed manually or using the CLI. The CLI offers the `graphcool modules` command for this purpose.

### Adding a module 

Here is an example where the `facebook` authentication module is added to a blank Graphcool project:

```bash
graphcool init --template blank --name MyProject
graphcool modules add graphcool/modules/authentication/facebook
```

Notice that [`graphcool/modules/authentication/facebook `](https://github.com/graphcool/modules/tree/master/authentication/facebook) represents the path to the module on GitHub:

- `graphcool` is the name of the GitHub organization
- `/modules/authentication` is the path to the directory that contains the module
- `/facebook ` is the actual module that contains the Graphcool project definition

After these commands are executed, the modules section in the project definition looks as follows:

```yml
# Graphcool modules
modules: 
  facebook: modules/facebook/graphcool.yml
```

The CLI further created the `modules` directory where it put the contents that it downloaded from GitHub:

```
.
├── modules
│   └── facebook
│       ├── README.md
│       ├── code
│       │   ├── facebook-authentication.graphql
│       │   └── facebook-authentication.js
│       ├── docs
│       │   ├── app-id.png
│       │   └── facebook-login-settings.png
│       ├── graphcool.yml
│       ├── login.html
│       └── types.graphql
...
```

### Deploying a module

After a module was added to your project locally (e.g. using `graphcool modules add <module>`), you still need to sync the local changes with the remote project in your Graphcool account.

For this purpose, you can simply use the `graphcool deploy` like with any other changes you're making locally to your project.

When a module is deployed, the CLI simply _merges_ your current project definition with the one from the module and applies all changes that are introduced by the module.
