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
graphcool init DIRNAME
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
 -D, --dry-run                                    Perform dry-run of deployment to view changes
 -f, --force                                      Accept data loss caused by schema changes
 -i, --interactive                                Force interactive mode to select the cluster
 -n, --new-service NEW-SERVICE                    Name of the new Service
 -c, --new-service-cluster NEW-SERVICE-CLUSTER    Name of the Cluster to deploy to
 -t, --target TARGET                              Target name
 -w, --watch                                      Watch for changes
```
      
#### Examples

##### Deploy local service definition changes to the `default` target.

```sh
graphcool deploy
```

##### Deploy local changes to a specific target called `prod`.

```sh
graphcool deploy --target prod
```

Note that in case your `.graphcoolrc` did not yet contain a target called `prod`, a new target with that name will be created.

##### Deploy local changes from default service file accepting potential data loss caused by schema changes.

```sh
graphcool deploy --force
```



### `graphcool info`

Prints meta-data about a specific service. The information contains:

- All clusters to which the service is currently deployed
- API endpoints

#### Usage

```sh
graphcool info
```

#### Examples

##### Print info of current service.

```sh
graphcool info
```



### `graphcool playground`

Open a [Playground](https://github.com/graphcool/graphql-playground) for the current service. The current service is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage

```sh
graphcool playground [flags]
```

#### Flags

```
-t, --target TARGET      Target name
-w, --web                Open browser-based Playground
```



### `graphcool delete`

Delete an existing target from the cluster its deployed to.

#### Usage

```sh
graphcool delete [flags]
```

#### Flags

```
 -f, --force               Force delete, without confirmation
 -t, --target TARGET       Target name
```



### `graphcool add-template`

Add new [template](!alias-zeiv8phail) to current Graphcool service. After invoking this command, you still need to uncomment the added lines in `graphcool.yml` and `types.graphql` and then run `grphcool deploy`.

#### Usage 

```sh
graphcool add-template TEMPLATE
```

#### Examples
      
##### Pull in the officially supported [`email-password` authentication template](https://github.com/graphcool/templates/tree/master/auth/email-password)

```sh    
graphcool add-template graphcool/templates/auth/email-password
```

#### Examples
      
##### Pull in the officially supported [`mailgun` messaging template](https://github.com/graphcool/templates/tree/master/messaging/mailgun)

```sh    
graphcool add-template graphcool/templates/messaging/mailgun
```



### `graphcool root-token`

Print the root token of a specific service. If no concrete token is specified as an option, the command will only list the names of the available tokens.

#### Usage 

```sh
graphcool root-token [flags]
```

#### Flags

```
-t, --token TOKEN        Name of the token
-t, --target TARGET      Target name
```

#### Examples

##### List which root tokens are setup for the current service.

```sh
graphcool root-token
```

##### Fetch a concrete root token.

```sh
graphcool root-token --token my-token
```

Assuming the service has a root token that's called `my-token`. 

> To add a new root token to your service, add the name of the new token to the `rootTokens` section in `graphcool.yml` and run `graphcool deploy`.



### `graphcool logs`

Print the logs of the [functions](!alias-aiw4aimie9) that are setup in the current service.

#### Usage

```sh
graphcool logs [flags]
```

#### Flags

```
-f, --function FUNCTION    (required) Name of the function to get the logs from
-t, --target TARGET      Target name
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
-t, --target TARGET      Target name
```

#### Examples

##### Export data from default service environment.

```sh
graphcool export
```

#####  Export data from a specific service environment.

```sh
graphcool export --target prod
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
targets:
  default: dev
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 





## Local development (Docker)

### `graphcool local pull`

Download latest (or specific) framework cluster version.

#### Usage 

```sh
graphcool local pull [flags]
```

#### Flags

```
 -n, --name NAME    Name of the new instance
```



### `graphcool local stop`

Stop local development cluster.

#### Usage 

```sh
graphcool local pull [flags]
```

#### Flags

```
 -n, --name NAME    Name of the new instance
```



### `graphcool local up`

Start local development cluster (Docker required).

#### Usage 

```sh
graphcool local up [flags]
```

#### Flags

```
 -n, --name NAME    Name of the new instance
```



### `graphcool local restart`

Restart local development cluster.

#### Usage 

```sh
graphcool local restart [flags]
```

#### Flags

```
 -n, --name NAME    Name of the new instance
```

### `graphcool local ps`

List Docker containers.

#### Usage 

```sh
graphcool local ps
```


### `graphcool local eject`

Eject from the managed docker runtime.

#### Usage 

```sh
graphcool local eject
```



## Platform

### `graphcool login`

Sign up or login (opens your browser for authentication). The platform token that's received after successful login will be stored in `~/.graphcoolrc`.

#### Usage 

```sh
graphcool login [flags]
```

#### Flags

```
-t, --token TOKEN    System token
```
  
#### Examples
      
##### Authenticate using the browser.

```sh    
graphcool login
```
##### Authenticate using an existing authentication token.

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
-t, --target TARGET      Target name
```

#### Examples
      
##### Open the console for the current service.

```sh    
graphcool console
```

##### Open the console for the `prod` environment.

```sh
graphcool console [flags]
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 




## Other

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




