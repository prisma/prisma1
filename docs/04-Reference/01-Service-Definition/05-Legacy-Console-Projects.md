---
alias: aemieb1aev
description: Graphcool is currently transitioning from a pure Backend as a Service to a general Application Development Framework. If you are an existing Graphcool customer this document explains everything you need to know about the transition. If you are new to Graphcool or about to craete a new application, you should use the new CLI as described in the [readme](https://github.com/graphcool/graphcool).
---

# Migration Guide for Legacy Console Projects

## Terminology

When creating a backend with Graphcool, we used to refer to it as a _project_. In the future we will refer to this as a _service_. During the transition we will use _legacy Console project_ to refer to old projects and _Graphcool Service_ to refer to services created using the new CLI that was introduced with the [Framework Preview](https://blog.graph.cool/graphcool-framework-preview-ff42081b1333).

If you have recently started using Graphcool and are unsure what version you are using, this is how you can find out:

- **Legacy Console Project**: Projects created with any CLI version lower than 0.4 or in the Console. Legacy Console projects are not using the [`graphcool.yml`](!alias-foatho8aip) service definition file, but simply pull the current data model from the old project configuration file `project.graphcool`. If you go to the console you will be able to edit your schema, functions etc.
- **Graphcool Service**: Graphcool services that were created with any CLI version greater or equal to 0.4. If you go to the console you will not be able to edit your schema, functions etc.


## Can I continue to use my legacy console project?

Yes!

While most new features added to the Graphcool Framework will require you to upgrade to a Graphcool _service_, you can continue to use your _legacy Console project_ without change. 

Legacy Console projects can only use CLI versions lower than 0.4 and are primarily managed through the Console. Particularly, managing _functions_ and _permissions_ can only be done in the Console.

Managing the GraphQL type definitions can still be done both in the Console (in the _Schema Editor_) and the old CLI (using `graphcool pull` and `graphcool push`).


## Upgrading a legacy Console project to a Graphcool service

In most cases upgrading a _legacy Console project_ to a _Graphcool service_ is a simple process. 

> We recommend that you first create a new service using the [CLI](!alias-zboghez5go) to familiarize yourself with the new workflows.

It is important to understand that **once a project is upgraded to a Graphcool service, it can not be converted back to a legacy Console project again**.

To upgrade a _legacy Console project_ to a _Graphcool service_, you need to navigate to the **Project Settings** in the Console, select the **General**-tab and click the **Upgrade Project**-button:

![](https://i.imgur.com/dCp8HPH.png)

<InfoBox type=warning>

Upgrading a project is a one-way migration. If you have any concerns about the process, please ask a question in the fForum](https://www.graph.cool/forum/).

</InfoBox>

## Deprecated features

A few features are being deprecated as part of the Framework release. If your cegacy Console roject is currently using any of these featuers you will have to replace the functionality before you can upgrade. The following features have been deprecated:

- **Integrations**: If you are currently using the Algolia integration or any of the Auth Providers that can be configured in the console, you should transition to one of the many [templates](!alias-zeiv8phail) available on [Github](https://github.com/graphcool/templates). 
- **Request Pipeline functions with a `PRE_WRITE` step**: Depending on your use case, you can either move the code to `TRANSFORM_ARGUMENT` or to a `Server-side Subscription`.
