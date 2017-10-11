---
alias: aiteerae6l
description: An overview and explanation of all the commands of the Graphcool CLI. Learn about different options, workflows and examples.
---

# Commands

## Service management

### `graphcool init`

Creates the local file structure for a new Graphcool service:

- `graphcool.yml` which contains the [service definition](!alias-opheidaix3)
- `types.graphql` for your [data model](!alias-eiroozae8u) and other type definitions
- `src` (directory) with a default "Hello World" function

If you provide a directory to the command, all these files will be places inside that directory. 

#### Usage

```sh
graphcool init [DIRNAME]
```

#### Examples

##### Create file structure for Graphcool service in current directory.

```sh
graphcool init
```

##### Create file structure for Graphcool service in directory called `server`.

```sh
graphcool init server
```



### `graphcool deploy`

Deploys service definition changes. Every time you're making a local change to any file in the service definition on your machine, you need to synchronize these changes with the remote service with this command. 

#### Usage 

```sh
graphcool deploy [flags]
```

#### Flags

```
 -a, --alias ALIAS                                Service alias
 -d, --default                                    Set specified target as default
 -f, --force                                      Accept data loss caused by schema changes
 -i, --interactive                                Force interactive mode to select the cluster
 -n, --new-service NEW-SERVICE                    Name of the new Service
 -c, --new-service-cluster NEW-SERVICE-CLUSTER    Name of the Cluster to deploy to
 -t, --target TARGET                              Local target, ID or alias of service to deploy
 -w, --watch                                      Watch for changes
```
      
#### Examples

##### Deploy local service definition changes to the `default` target.

```sh
graphcool deploy
```

##### Deploy local changes to a specific target called `prod`

```sh
graphcool deploy --target prod
```

Note that in case your `.graphcoolrc` did not yet contain a target called `prod`, a new target with that name will be created.

##### Deploy local changes from default service file accepting potential data loss caused by schema changes

```sh
graphcool deploy --force
```


### `graphcool info`

Prints meta-data about a specific service. The information contains:

- Default environment (specified in `.graphcoolrc`)
- Project ID (specified in `.graphcoolrc`)
- API endpoints (generated based on service ID)

#### Usage

```sh
graphcool info [flags]
```

#### Flags

```
-e, --env ENV    Environment name to set
```

#### Examples

##### Print service info of default environment.


```sh
graphcool info
```

##### Print service of concrete environment.

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


### `graphcool playground`

Open a Playground for the current service. The current service is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage

```sh
graphcool playground --env ENV
```

#### Flags

```
-e, --env ENV              Environment name to set
```



### `graphcool diff`

Displays all the changes between your local service definition and the remote service definition. This command essentially is a "dry-run" for the `graphcool deploy` command. 

#### Usage 

```sh
graphcool diff [flags]
```

#### Flags

```
-e, --env ENV            Project environment to be deployed
-p, --service PROJECT    ID or alias of  service to deploy
```

#### Examples

##### See local changes from graphcool.yml in default service environment.

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



### `graphcool delete`

Delete an existing service.

#### Usage

```sh
graphcool delete [flags]
```

#### Flags

```
 -f, --force            Force delete, without confirmation
 -t, --target TARGET    Target to delete
```



### `graphcool add-template`

Add new [template](!alias-zeiv8phail) to current Graphcool service.

#### Usage 

```sh
graphcool add-template <template>
```

#### Examples
      
##### Pull in the officially supported [`email-password` authentication template](https://github.com/graphcool/templates/tree/master/auth/email-password)

```sh    
graphcool add-template graphcool/templates/messaging/email-password
```

#### Examples
      
##### Pull in the officially supported [`mailgun` messaging template](https://github.com/graphcool/templates/tree/master/messaging/mailgun)

```sh    
graphcool add-template graphcool/templates/messaging/mailgun
```



### `graphcool root-token`

Get the root tokens of a specific service. If no concrete token is specified as an option, the command will only list the names of the available tokens.

#### Usage 

```sh
graphcool get-root-token [flags]
```

#### Flags

```
-e, --env ENV            Environment name to set
-p, --service PROJECT    Project Id to set
-t, --token TOKEN        Name of the token
```

#### Examples

##### List which root tokens are setup for this service.

```sh
graphcool get-root-token
```

##### Fetch a concrete root token.

```sh
graphcool get-root-token --token my-token
```

Assuming the service has a root token that's called `my-token`. 

> To add a new root token to your service, add the name of the new token to the `rootTokens` section in `graphcool.yml` and run `graphcool deploy`.



### `graphcool logs`

Print the logs of the serverless functions that are setup in the current service.

#### Usage

```sh
graphcool logs [flags]
```

#### Flags

```
-f, --function FUNCTION    (required) Name of the function to get the logs from
-e, --env ENV              Environment name to set
-p, --service PROJECT      Project Id to set
-t, --tail                 Tail function logs in realtime
```





## Data workflows

### `graphcool export`

Exports your service data by generating a URL from which you can download a .zip-file that contains all the data from your service.

#### Usage 

```sh
graphcool export [flags]
```

#### Flags

```
-e, --env ENV            Project environment from which to export data
-p, --service PROJECT    ID or alias of service from which to export data
```

#### Examples

##### Export data from default service environment.

```sh
graphcool export
```

#####  Export data from a specific service environment.

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





## Local development (Docker)

### `graphcool local pull`

### `graphcool local stop`

### `graphcool local up`

### `graphcool local restart`

### `graphcool local eject`





## Platform

### `graphcool auth`

Sign up or login (opens your browser for authentication). The authentication token that's received after successful login will be stored in `~/.graphcoolrc`.

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



### `graphcool console`

Open the console for the current service. The current service is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage 

```sh
graphcool console [flags]
```

#### Flags

```
-e, --env ENV    Environment name
```

#### Examples
      
##### Open the console for the current service.

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





















### `graphcool help`

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




