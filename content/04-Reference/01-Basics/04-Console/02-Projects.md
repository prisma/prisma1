---
alias: yahph3foch
path: /docs/reference/console/projects
layout: REFERENCE
description: A GraphQL projects exposes different API endpoints, and offers different settings like setting an alias.
tags:
  - console
related:
  further:
    - ow2yei7mew
  more:
---

# Projects

Apps you are building with Graphcool are organized into different projects. A project consists of a [data schema](!alias-ahwoh2fohj), the data itself and other components like [integrations](!alias-seimeish6e) or [permissions](!alias-iegoo0heez).

More options to manage your project can be found in the [project settings](!alias-aechi6iequ).

## Project Endpoints

A project offers different [API](!alias-heshoov3ai) endpoints that all contain the project id.

![](./endpoints.png?width=600)

## Managing projects

By clicking the arrow next to the active project's name, you can expand the project list to get an overview of all your existing projects.

![](./project-list.png?width=200)

Click the plus button to create new projects. The schema of a new project will be populated with the existing [system types](!alias-uhieg2shio).

## Project names and aliases

Project names can contain **alphanumeric characters and spaces** and need to start with an uppercase letter. They can contain **maximally 64 characters**.

*Project names are unique on an account level.*

Project aliases can contain **lowercase letters and dashes**.

*Project aliaes are globally unique.*

#### Examples

Valid project names:

* `My Project`
* `MyProject2`
* `MY PROJECT`

Valid project aliases:

* `my-project`
* `my-project-dev`
* `myproject`
