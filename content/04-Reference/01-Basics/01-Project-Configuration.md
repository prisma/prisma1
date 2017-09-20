---
alias: opheidaix3
description: An overview of the project definition, file structure and overall configuration of Graphcool projects.
---

# Project Configuration

## Overview

Every Graphcool project consists of several components that developers can provide:

- **Database model**: Determines the types that are to be persisted in the database. These types typically represent the entities from the application domain. Read more in the [Database](!alias-viuf8uus7o) chapter.
- **Permission rules**: Define which users are allowed to perform what operations in the API. Read more in the [Authorization](!alias-iegoo0heez) chapter.
- **Serverless functions**: Used to implement data validation and transformation, GraphQL resolvers functions and other business logic. Read more in the [Functions](!alias-aiw4aimie9) chapter.
- **Modules**: Other Graphcool projects that provide additional functionality to the current project. Read more in the [Modules](#modules) section below.
- **Root tokens**: The authentication tokens that provide access to all API operations. Read more in the [Authentication](!alias-bee4oodood) chapter. 

To manage each of these components in a coherent way, Graphcool uses a custom configuration format written in [YAML](https://en.wikipedia.org/wiki/YAML). The file can be altered manually or through dedicated commands of the [CLI](!alias-zboghez5go).

## Project definition 

### Example

Here is what a simple example of a project definition file looks like:

```yml
# Type Definitions
types: ./types.graphql


# Serverless Functions
functions:
  authenticateCustomer:
    handler:
      code:
        src: ./code/authenticate.js
    type: resolver
    schema: ./code/authenticate.graphql
  sendSlackMessage:
    handler:
      webhook:
        url: http://example.org/sendSlackMessage
        headers:
            Content-Type: application/json
            Authorization: Bearer saiquiegh1wohb7shie9phai
    type: subscription
    query: ./code/sendSlackMessage/newMessage.graphql


# Permission Rules
permissions:
- operation: Message.read
- operation: Message.create
  authenticated: true
  query: ./permissions/message.graphql


# Modules
modules:
- ./modules/facebookLogin/fb.yml


# Root tokens
rootTokens:
- authenticate
```

This project definition expects the following project structure:

```
.
├── code
│   ├── authenticate.graphql
│   └── authenticate.js
│   └── sendSlackMessage
│       └── newMessage.graphql
├── modules
│   └── facebookLogin
│       ├── ...
│       └── fb.yml
└── permissions
    └── message.graphql
```


### YAML structure

The YAML configuration file has the following _root properties_:

| Root Property | Type | Description | 
| --------- | ------------------ | --------------- | 
| `types`| `string`<br>`[string]` | Type defintions ([SDL]()) for database models, relations, enums and other types. |
| `functions` | `[string:function]` | All serverless functions that belong to the current project. The key of each element in the dictionary is a unique name for the function, the value specifies details about the function to be invoked. See the `function` type below for more info on the structure. |
| `permissions` | `[permission]` | All permissions rules that belong to the current project. See the `permission` type below for more info on the structure. |
| `modules` | `[string]` | A list of filenames that refer to configuration files of other Graphcool projects which are used in the current project (_modules_). |

This is what the additional YAML types look like that are used in the file:

#### Type: `function`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `isEnabled` | `boolean` | `true`<br>`false` | No (`true`) | The function will only be executed if set to true | `true`|
| `handler` | `string`<br>`[string:string]`<br>`[string:webhook]` | any<br>`["webhook":any]`<br>`["webhook":any]` | Yes | Defines how this function should be invoked (i.e. where it lives). Either as a webhook or a local Graphcool function. | 
| `type` | `string` | `subscription`<br>`resolver`<br>`operationBefore`<br>`operationAfter` | Yes | Defines what kind of serverless function this is. |
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `operationBefore` or `operationAfter` | If the function is set up in the context of an operation, this specifies the concrete operation. |
| `query` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `subscription` | If the function is set up as a subscription, this specifies the subscription query (which determines the input type of the function). |
| `schema` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Only if `type` is `resolver` | If the function is set up as a resolver, this specifies the necessary extensions on the `Query` or `Mutation` type (and potentially additional types that represent the input or return types of the new field).


#### Type: `permission`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `operation` | `string` | `<Model>.<operation>`<br>`<Relation>.<operation>` | Yes | Specifies the operation for which this permission rule should apply. |
| `authenticated` | `boolean` | `true`<br>`false` | No (`false)` | Specifies any HTTP headers for the request that invokes the function. | Defines whether a request needs to be authenticated to perform the corresponding operation (if true, the `id` of the authenticated node is available as an argument inside the permission query. |
| `query` | `string` | any | No | Specifies the permission query that belongs to this permission rule. |
| `fields` | `[string]` | any | all fields of the model type | Specifies to which fields this permission rule should apply to.

 
#### Type: `webhook`

| Property  | Type | Possible Values | Required (default value) | Description|
| --------- | ------------------ | --------------- | --------- | ------------------ | --------------- | --------------- | 
| `url` | `string` | any | Yes | Specifies the URL where the function can be invoked. |
| `headers` | `[string:string]` | any | No | Specifies any HTTP headers for the request that invokes the function. |


## Applying project changes

After making a change to the project definition or any file inside the file structure of your local project (e.g. making changes to the implementation of a serverless function), you need to sync these changes to the remote project that lives in your Graphcool account.

The only way of doing this is by running the `graphcool deploy` command. 

In case your local changes might result in data loss, you need to add the `--force` option to the command in order to signal to the CLI that you know what you're doing: `graphcool deploy --force`.

Sometimes your changes require _migration values_ to be added for the deployment. For example, when a you're adding a non-nullable field to a model type for which there are already existing nodes in the database.

Notice that `graphcool diff` will print all changes between your local project and the remote project in your Graphcool account.

## Environments

When working with Graphcool, you can configure different _environments_ that you can deploy your project to. You can use this for example to separate a _development_ from a _production_ environment.

> Notice that the concept of an environment only exists in the _frontend_ (CLI). The backend is not aware of environments and only cares about _projects_.

### `.graphcoolrc`

Environments are managed in the `.graphcoolrc` file that's generated for you when you're creating a new project with `graphcool init`.

#### Example

```yml
default: dev
environments:
  dev:
    projectId: cj7n3mp0f0000rayvphelgz7d
    envVars:
      WEBHOOK_URL: https://dev.example.com/webhook
  prod:
    projectId: cj7n3ob1y0001rayvdhegkcu2
    envVars:
      WEBHOOK_URL: https://prod.example.com/webhook
```

#### Structure

`.graphcoolrc` is written in YAML and has the following root properties:

| Root Property | Type | Description | 
| --------- | ------------------ | --------------- | 
| `default`| `string` | Specifies the name of the default environment. This environment will be used for all commands you're invoking with the CLI. If you want to run a command against a different environment than the default one, you need to specify the `--env` option for the command. |
| `environments` | `[string:string]` | A list of environments that are available. |


### The `default` environment

Environments are relevant when using the CLI to manage your project. Almost every CLI command takes the `--env <env>` option for you to specify to which environment this command should be applied, for example: `graphcool deploy --env prod`.

When not passing the `--env <env>` option to a CLI command, the CLI checks `.graphcoolrc` for the `default` environment and runs the specified command against it.

### Managing environments

Environments can be managed by manually changing the contents of `.graphcoolrc` or by using the CLI and the `graphcool env` command. `graphcool env` has four dedicated subcommands that you can use to alter the contents of `.graphcoolrc`:

- `graphcool env default <env>`: Sets the `default` environment
- `graphcool env set <env>`: Adds an environment to the `environments` list
- `graphcool env reove <env>`: Removes an environment from the `environments` list
- `grahcool env rename <oldname> <newname>`: Renames an environment from the `environments` list


## Modules

A Graphcool project can include a number of _modules_ that contain additional functionality. A module is nothing but another Graphcool project with its own project definition and file structure.

When adding a module to an existing project, the files that belong to the module need to be placed in the project under a directory called `modules`. Additionally, the module needs to be added to the `modules` list in the project definition.

Modules are primarily are tool for _code organization_ that you can use to improve the structure of your project and make it more modular. 

### Predefined modules

Graphcool provides a number of predefined modules that you can pull into your project. The predefined modules are located in the [`modules`](https://github.com/graphcool/modules) repository of the `graphcool` GitHub organization.

However, any GitHub repository (or any directory inside a repository) that contains a Graphcool project definition and the corresponding files qualifies as a module. 

### Managing modules

Like environments, modules can be managed manually or using the CLI. The CLI offers the `graphcool modules` command for this purpose.

#### Adding a module 

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

#### Deploying a module

After a module was added to your project locally (e.g. using `graphcool modules add <module>`), you still need to sync the local changes with the remote project in your Graphcool account.

For this purpose, you can simply use the `graphcool deploy` like with any other changes you're making locally to your project.

When a module is deployed, the CLI simply _merges_ your current project definition with the one from the module and applies all changes that are introduced by the module.


## Ejecting a project

A remote Graphcool project (i.e. a project in your Graphcool account) can be in either of two modes:

- _Ejected_: Projects that are or have been created with any CLI version lower than 1.4 or in the Console are _non-ejected_ by default.
- _Not ejected_: Projects that are created with any CLI version greater or equal to 1.4 are _ejected_ by default. 

> These two modes exist due to the recent changes to the Graphcool platform and its new focus towards managing projects through the CLI rather than using the web-based Console. 

To eject a project, you can use the `graphcool eject` command of the CLI. If you want to manage your project locally with a CLI version greater or equal to 1.4, it needs to be ejected.

<InfoBox type=warning>

Once a project was ejected, it can't be converted back to the non-ejected state any more!

</InfoBox>

### Non-ejected projects

Non-ejected projects can only use CLI versions lower than 1.4 and are primarily managed through the Console. Particularly, managing _functions_ and _permissions_ can only be done in the Console with non-ejected projects.

Managing the GraphQL type definitions can still be done both in the Console (in the _Schema Editor_) and the CLI (using `graphcool pull` and `graphcool push`).

### Ejected projects

Ejected projects can only use CLI versions greater or equal to 1.4. Non-ejected projects can only be modified with the CLI, all the features that are offered for non-ejected projects in the Console are _read-only_.




