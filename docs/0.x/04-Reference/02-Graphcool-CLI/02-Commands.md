---
alias: aiteerae6l
description: An overview and explanation of all the commands of the Graphcool Framework CLI. Learn about different options, workflows and examples.
---

# Commands

<InfoBox type=info>

When invoking the CLI, you can use either the full `graphcool-framework` command or the short form `gcf`. All examples on this page are using the longer, explicit version.

</InfoBox>

## Service management

### `graphcool-framework init`

Creates the local file structure for a new Graphcool service:

- `graphcool.yml` which contains the [service definition](!alias-opheidaix3)
- `types.graphql` for your [data model](!alias-eiroozae8u) and other type definitions
- `src` (directory) with a default "Hello World" function

If you provide a directory to the command, all these files will be places inside that directory. 

#### Usage

```sh
graphcool-framework init DIRNAME
```

#### Examples

##### Create file structure for Graphcool service in current directory.

```sh
graphcool-framework init
```

##### Create file structure for Graphcool service in directory called `server`.

```sh
graphcool-framework init server
```

### `graphcool-framework deploy`

Deploys service definition changes. Every time you're making a local change to any file in the service definition on your machine, you need to synchronize these changes with the remote service with this command. 

#### Usage

```sh
graphcool-framework deploy [flags]
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
graphcool-framework deploy
```

##### Deploy local changes to a specific target called `prod`.

```sh
graphcool-framework deploy --target prod
```

Note that in case your `.graphcoolrc` did not yet contain a target called `prod`, a new target with that name will be created.

##### Deploy local changes from default service file accepting potential data loss caused by schema changes.

```sh
graphcool-framework deploy --force
```

### `graphcool-framework info`

Prints meta-data about a specific service. The information contains:

- All clusters to which the service is currently deployed
- API endpoints

#### Usage

```sh
graphcool-framework info
```

#### Examples

##### Print info of current service.

```sh
graphcool-framework info
```



### `graphcool-framework playground`

Open a [Playground](https://github.com/graphcool/graphql-playground) for the current service. The current service is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage

```sh
graphcool-framework playground [flags]
```

#### Flags

```
-t, --target TARGET      Target name
-w, --web                Open browser-based Playground
```



### `graphcool-framework delete`

Delete an existing target from the cluster its deployed to.

#### Usage

```sh
graphcool-framework delete [flags]
```

#### Flags

```
 -f, --force               Force delete, without confirmation
 -t, --target TARGET       Target name
```



### `graphcool-framework add-template`

Add new [template](!alias-zeiv8phail) to current Graphcool service. After invoking this command, you still need to uncomment the added lines in `graphcool.yml` and `types.graphql` and then run `graphcool-framework deploy`.

#### Usage 

```sh
graphcool-framework add-template TEMPLATE
```

#### Examples
      
##### Pull in the officially supported [`email-password` authentication template](https://github.com/graphcool/templates/tree/master/auth/email-password)

```sh    
graphcool-framework add-template graphcool/templates/auth/email-password
```

#### Examples
      
##### Pull in the officially supported [`mailgun` messaging template](https://github.com/graphcool/templates/tree/master/messaging/mailgun)

```sh    
graphcool-framework add-template graphcool/templates/messaging/mailgun
```



### `graphcool-framework root-token`

Print the root token of a specific service. If no concrete token is specified as an option, the command will only list the names of the available tokens.

#### Usage 

```sh
graphcool-framework root-token [flags]
```

#### Flags

```
-t, --token TOKEN        Name of the token
-t, --target TARGET      Target name
```

#### Examples

##### List which root tokens are setup for the current service.

```sh
graphcool-framework root-token
```

##### Fetch a concrete root token.

```sh
graphcool-framework root-token --token my-token
```

Assuming the service has a root token that's called `my-token`. 

> To add a new root token to your service, add the name of the new token to the `rootTokens` section in `graphcool.yml` and run `graphcool-framework deploy`.



### `graphcool-framework logs`

Print the logs of the [functions](!alias-aiw4aimie9) that are setup in the current service.

#### Usage

```sh
graphcool-framework logs [flags]
```

#### Flags

```
-f, --function FUNCTION    (required) Name of the function to get the logs from
-t, --target TARGET      Target name
--tail                   Tail function logs in realtime
```





## Local development (Docker)

### `graphcool-framework local pull`

Download latest (or specific) framework cluster version.

#### Usage 

```sh
graphcool-framework local pull [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```



### `graphcool-framework local stop`

Stop local development cluster.

#### Usage 

```sh
graphcool-framework local pull [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```



### `graphcool-framework local up`

Start local development cluster (Docker required).

#### Usage 

```sh
graphcool-framework local up [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```



### `graphcool-framework local restart`

Restart local development cluster.

#### Usage 

```sh
graphcool-framework local restart [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

### `graphcool-framework local ps`

List Docker containers.

#### Usage 

```sh
graphcool-framework local ps
```


### `graphcool-framework local eject`

Eject from the managed docker runtime.

#### Usage 

```sh
graphcool-framework local eject
```



## Platform

### `graphcool-framework login`

Sign up or login (opens your browser for authentication). The platform token that's received after successful login will be stored in `~/.graphcoolrc`.

#### Usage 

```sh
graphcool-framework login [flags]
```

#### Flags

```
-t, --token TOKEN    System token
```
  
#### Examples
      
##### Authenticate using the browser.

```sh    
graphcool-framework login
```

##### Authenticate using an existing authentication token.

```sh
graphcool-framework login -t <token>
```



### `graphcool-framework console`

Open the console for the current service. The current service is determined by the default environment that's specified in the `.graphcoolrc` of the directory in which you're executing the command.

#### Usage 

```sh
graphcool-framework console [flags]
```

#### Flags

```
-t, --target TARGET      Target name
```

#### Examples
      
##### Open the console for the current service.

```sh    
graphcool-framework console
```

##### Open the console for the `prod` environment.

```sh
graphcool-framework console [flags]
```

Assuming you're executing the command in a directory that contains a `.graphcoolrc` looking similar to this:

```yml
default: dev
environments:
  dev: cj7pyduqj0qyb0136kgf63887
  prod: th4pydulr0vjb049lkgf63951
``` 




## Other

### `graphcool-framework help`

Prints instructions and examples for the usage of a specific command.

#### Usage 

```sh
graphcool-framework help COMMAND
```

#### Examples

##### Instructions and examples for the `init` command.

```sh
graphcool-framework help init
```

##### Overview of all commands.

```sh
graphcool-framework help
```




