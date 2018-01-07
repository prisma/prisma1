---
alias: eeb1ohr4ec
description: Graphcool CLI Commands
---

TODO: # ` -> ### `


# Commands

## Database Service

# `graphcool init`

Creates the local file structure for a new Graphcool database service:

- `graphcool.yml` contains the [service definition](!alias-opheidaix3)
- `datamodel.graphql` contains the definition of your [data model](!alias-eiroozae8u)
- `.graphqlconfig` is a configuration file following the standardized [`graphql-config`](https://github.com/graphcool/graphql-config) format and is used by various tools, e.g. the [GraphQL Playground](https://github.com/graphcool/graphql-playground)

If you provide a directory name as an argument to the command, all these files will be placed inside a new directory with that name.

#### Usage

```sh
graphcool init DIRNAME
```

#### Examples

##### Create file structure for Graphcool database service in current directory.

```sh
graphcool init
```

##### Create file structure for Graphcool database service in directory called `database`.

```sh
graphcool init database
```

# `graphcool deploy`

Deploys service definition changes. Every time you're making a local change to any file in the service definition on your machine, you need to synchronize these changes with the remote service using this command.

#### Usage

```sh
graphcool deploy [flags]
```

#### Flags

```
-D, --default                                    Set specified stage as default
-d, --dry-run                                    Perform a dry-run of the deployment
-f, --force                                      Accept data loss caused by schema changes
-i, --interactive                                Force interactive mode to select the cluster
-j, --json                                       JSON Output
-c, --new-service-cluster NEW-SERVICE-CLUSTER    Name of the Cluster to deploy to
-s, --stage STAGE                                Local stage to deploy to
-w, --watch                                      Watch for changes
--dotenv DOTENV                                  Path to .env file to inject env vars
--no-seed                                        Disable seed on initial service deploy
```

#### Examples

##### Deploy local service definition changes to the default stage.

```sh
graphcool deploy
```

##### Deploy local changes to a specific stage called `prod`.

```sh
graphcool deploy --stage prod
```

##### Deploy local changes to a specific stage called `prod`, accepting potential data loss caused by schema changes.

```sh
graphcool deploy --stage production --force
```

# `graphcool info`

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

# `graphcool clusters`

Lists all clusters.

#### Usage

```sh
graphcool clusters
```

# `graphcool token`

Create a new service token

#### Usage

```sh
graphcool token [flags]
```

#### Flags

TODO

# `graphcool logs`

Output service logs.

#### Usage

```sh
graphcool logs [flags]
```

# `graphcool list`

List all deployed services.

#### Usage

```sh
graphcool list
```

#### Examples

##### List all deployed services for the currently authenticated user.

```sh
graphcool list
```

# `graphcool delete`

Delete an existing target from the cluster its deployed to.

#### Usage

```sh
graphcool delete [flags]
```

#### Flags

```
 -f, --force               Force delete, without confirmation
 -t, --stage STAGE         Stage name
```

## Data Workflows

# `graphcool playground`

Open service endpoints in [GraphQL Playground](https://github.com/graphcool/graphql-playground).

#### Usage

```sh
graphcool playground [flags]
```

#### Flags

```
-w, --web                Open browser-based Playground
```


# `graphcool import`

Import data into the database of your Graphcool service. The data needs to be formatted according to the [Normalized Data Format](!alias-teroo5uxih). For more info, read the [Data Import](!alias-ol2eoh8xie) chapter.

#### Usage

```sh
graphcool import [flags]
```

#### Flags

```
-d, --data DATA    (required) Path to zip or folder including data to import
```

# `graphcool export`

Exports your service data.

#### Usage

```sh
graphcool export [flags]
```

#### Flags

```
-e, --export-path EXPORT-PATH    Path to export .zip file
```

#### Examples

##### Export data

```sh
graphcool export -e export.zip
```

# `graphcool reset`

Reset the stage data.

#### Usage

```sh
graphcool reset [flags]
```

#### Flags

```
-f, --force    Force reset data without confirmation
```

## Cloud

# `graphcool login`

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
graphcool login -t <token>
```

# `graphcool account`

Print account info

## Local Development

# `graphcool local start`

Start local development cluster (Docker required).

#### Usage

```sh
graphcool local start [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

# `graphcool local stop`

Stop local development cluster.

#### Usage

```sh
graphcool local stop [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

# `graphcool local upgrade`

Upgrades local cluster to the latest version for the current CLI.

#### Usage

```sh
graphcool local pull [flags]
```

#### Flags

```
-n, --name NAME    Name of the cluster instance
```

# `graphcool local nuke`

Hard-reset local development cluster.

#### Usage

```sh
graphcool local nuke [flags]
```

#### Flags

```
-n, --name NAME    Name of the cluster instance
```

## Clusters

TODO

# `graphcool cluster list`

List all clusters.

#### Usage

```sh
graphcool cluster list
```
