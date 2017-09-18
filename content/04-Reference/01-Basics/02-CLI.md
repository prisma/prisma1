---
alias: zboghez5go
description: The Graphcool CLI is the main tool to manage a Graphcool project.
---

# Graphcool CLI

## Overview

The Graphcool is the main tool to manage your Graphcool projects.

## Commands

### `auth`

Sign up or login (opens your browser for authentication). 

Note: Your session token will be store in `~/.graphcool`.

#### Usage 

```sh
graphcool auth [flags]
```

#### Flags

```
-t, --token TOKEN    System token
```
  
#### Examples
      
##### Authenticate using the browser.

```sh    
graphcool auth
```
##### Authenticate using an existing (temporary) authentication token.

```sh
graphcool auth -t <token>
```

### `console`

Open the console for the current project. The current project is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage 

```sh
graphcool console [flags]
```

#### Flags

```
-e, --env ENV    Environment name
```

####authentication-with-email-and-apollo
      
##### Open the console for the current project.

```sh    
graphcool console
```
##### Open the console for the `prod` environment.

```sh
graphcool console --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 

### `deploy`

Deploy project definition changes. Every time you're making a local change to the project on your machine, you need to synchronize these changes with the remote project. This applies to your type definitions, permission rules, serverless functions and any other change you might perform in your `graphcool.yml`-file.

#### Usage 

```sh
graphcool deploy [flags]
```


#### Flags

```
 -e, --env ENV            Project environment to be deployed
 -f, --force              Accept data loss caused by schema changes
 -p, --project PROJECT    ID or alias of  project to deploy
```

####authentication-with-email-and-apollo
      
##### Deploy local changes from graphcool.yml to the default project environment.

```sh
graphcool deploy
```


##### Deploy local changes to a specific environment

```sh
graphcool deploy --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 

##### Deploy local changes from default project file accepting potential data loss caused by schema changes

```sh
graphcool deploy --force --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 

### `diff`

Displays all the changes between your local project definition and the remote project definition. This command essentially is a "dry-run" for the `graphcool deploy` command. 

#### Usage 

```sh
graphcool diff [flags]
```

#### Flags

```
-e, --env ENV            Project environment to be deployed
-p, --project PROJECT    ID or alias of  project to deploy
```

#### Examples

##### See local changes from graphcool.yml in default project environment.

```sh
graphcool diff
```


#####  See local changes from graphcool.yml for a specific environment.

```sh
graphcool diff --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 


### `env`

Manage the local environments for your project. This command provides handy shortcuts to manage the environments that are defined in your `.graphcoolrc` file.

#### Usage

graphcool env <subcommand> [flags]

#### Subcommands

##### `default`

This command sets the default environment by replacing the `default` entry in your local `.graphcoolrc`-file with the environment that is provided.

###### Usage

```sh
graphcool env default [flags]
```

###### Flags

```
-e, --env ENV    (required) Env to set as default
```

##### `remove`

This command removes the specified environment from the list of environments in your local `.graphcoolrc`-file.

###### Usage

```sh
graphcool env remove [flags]
```

###### Flags

```
-e, --env ENV    (required) Env to set as default
```


##### `rename`

This command renames an environment in the list of environments in your local `.graphcoolrc`-file. You need to provide the old and new name of the environment as options for this command.

###### Usage

```sh
graphcool env remove [flags]
```

###### Flags

```
-n, --newName NEWNAME    (required) New name
-o, --oldName OLDNAME    (required) Old name
```


##### `set`

Adds a new environment to the list of environments in your local `.graphcoolrc`-file. 

###### Usage

```sh
graphcool env set [flags]
```

###### Flags

```
-e, --env ENV            (required) Environment Name
-p, --project PROJECT    (required) Project ID
```


### `export`

Exports your project data by generating a URL from which you can download a .zip-file that contains all the data from your project.

#### Usage 

```sh
graphcool export [flags]
```

#### Flags

```
-e, --env ENV            Project environment from which to export data
-p, --project PROJECT    ID or alias of project from which to export data
```

#### Examples

##### Export data from default project environment.

```sh
graphcool export
```


#####  Export data from a specific project environment.

```sh
graphcool export --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 



### `functions`

See a list of all the functions that are currently active.

#### Usage 

```sh
graphcool functions [flags]
```

#### Flags

```
-e, --env ENV            Project environment from which to list active functions
-p, --project PROJECT    ID or alias of project from which to list active functions
```

#### Examples

##### List functions from default project environment.

```sh
graphcool functions
```

##### List functions from a specific project environment.

```sh
graphcool functions --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 

### `get-root-token`

Get the root tokens of a specific project. If no concrete token is specified as an option, the command will only list the names of the available tokens.

#### Usage 

```sh
graphcool get-root-token [flags]
```

#### Flags

```
-e, --env ENV            Environment name to set
-p, --project PROJECT    Project Id to set
-t, --token TOKEN        Name of the token
```

#### Examples

##### List which root tokens are setup for this project.

```sh
graphcool get-root-token
```

##### Fetch a concrete root token.

```sh
graphcool get-root-token --token my-token
```

Assuming the project has a root token that's called `my-token`. 

> To add a new root token to your project, add the name of the new token to the `rootTokens` section in `graphcool.yml` and run `graphcool deploy`.


### `help`

Prints instructions and examples for the usage of a specific command.

#### Usage 

```sh
graphcool help COMMAND
```

#### Examples

##### Instructions and examples for the `init` command.

```sh
graphcool help init
```

##### Overview of all commands.

```sh
graphcool help
```

### `info`

Prints meta-data about a specific project. The information contains:

- Default environment (specified in `.graphcoolrc`)
- Project ID (specified in `.graphcoolrc`)
- API endpoints (generated based on project ID)

#### Usage

```sh
graphcool info [flags]
```

#### Flags

```
-e, --env ENV    Environment name to set
```

#### Examples

##### Print project info of default environment.


```sh
graphcool info
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 

##### Print project of concrete environment.

```sh
graphcool info --env prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 

### `init`

Creates a new Graphcool project from scratch or based on an existing project. This command will create a new project in your Graphcool account as well as the local project definition and file structure, including:

- `.graphcoolrc` to manage environments
- `graphcool.yml` which contains the project definition
- `types.graphql` for your type definitions
- `code` (directory) with a default "Hello World" function

If you provide a directory to the command, all these files will be places inside that directory. 

#### Usage

```sh
graphcool init [DIRNAME] [flags]
```

#### Flags

```
-a, --alias ALIAS          Project alias
-c, --copy PROJECT         ID or alias of the project to be copied
-e, --env ENV              Local environment name for the new project
-n, --name NAME            Project name
-r, --region REGION        AWS Region of the project (options: US_WEST_2 (default), EU_WEST_1, AP_NORTHEAST_1)
-t, --template TEMPLATE    The template to base the init on. (options: blank, instagram)
```

#### Examples

##### Create new blank project called `MyProject`, with an alias called `myProject`.

```sh
graphcool init --template blank --name MyProject --alias myProject
```


##### Create a clone of an existing project.

```sh
graphcool init --copy myProject
```

Assuming your Graphcool account contains a project with the alias `myProject`.


### `logs`

Print the logs of the serverless functions that are setup in the current project.

#### Usage

```sh
graphcool logs [flags]
```

#### Flags

```
-f, --function FUNCTION    (required) Name of the function to get the logs from
-e, --env ENV              Environment name to set
-p, --project PROJECT      Project Id to set
-t, --tail                 Tail function logs in realtime
```

### `modules`

Manage the Graphcool modules inside your project. Modules are simple Graphcool projects that effectively get "merged" with your current project when you add them.

#### Usage

graphcool modules <subcommand> [flags]

#### Subcommands

##### `add`

Adds a new module to your project. This command will download the Graphcool project definition from the specified module path (usually a GitHub repository or a directory inside a GitHub repository) and put it into the `modules` directory of your project. It also adds the module to the `modules` section inside your project definition `graphcool.yml`.

##### Usage

```sh
graphcool modules add MODULE
```

Notice that `MODULE` is a path to a module on GitHub.

##### Examples

###### Add the `email-password` authentication module to the project.

```sh
`graphcool modules add graphcool/modules/authentication/email-password`
```

The `email-password` module can be found [here](https://github.com/graphcool/modules/tree/master/authentication/email-password).

### `playground`

Open a Playground for the current project. The current project is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage

```sh
graphcool playground --env ENV
```

#### Flags

```
-e, --env ENV              Environment name to set
```


### `projects`

Prints a list of all the projects in your Graphcool account.

#### Usage

```sh
graphcool projects
```

#### Examples

##### Print all the projects in your Graphcool account.

```sh
graphcool projects
```


