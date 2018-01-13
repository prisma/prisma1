---
alias: eeb1ohr4ec
description: Prisma CLI Commands
---

TODO: # ` -> ### `


# Commands

## Database Service

# `prisma init`

Creates the local file structure for a new Prisma database service:

- `prisma.yml` contains the [service definition](!alias-opheidaix3)
- `datamodel.graphql` contains the definition of your [data model](!alias-eiroozae8u)
- `.graphqlconfig` is a configuration file following the standardized [`graphql-config`](https://github.com/graphcool/graphql-config) format and is used by various tools, e.g. the [GraphQL Playground](https://github.com/graphcool/graphql-playground)

If you provide a directory name as an argument to the command, all these files will be placed inside a new directory with that name.

#### Usage

```sh
prisma init DIRNAME
```

#### Examples

##### Create file structure for Prisma database service in current directory.

```sh
prisma init
```

##### Create file structure for Prisma database service in directory called `database`.

```sh
prisma init database
```

# `prisma deploy`

Deploys service definition changes. Every time you're making a local change to any file in the service definition on your machine, you need to synchronize these changes with the remote service using this command.

#### Usage

```sh
prisma deploy [flags]
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
prisma deploy
```

##### Deploy local changes to a specific stage called `prod`.

```sh
prisma deploy --stage prod
```

##### Deploy local changes to a specific stage called `prod`, accepting potential data loss caused by schema changes.

```sh
prisma deploy --stage production --force
```

# `prisma info`

Prints meta-data about a specific service. The information contains:

- All clusters to which the service is currently deployed
- API endpoints

#### Usage

```sh
prisma info
```

#### Examples

##### Print info of current service.

```sh
prisma info
```

# `prisma clusters`

Lists all clusters.

#### Usage

```sh
prisma clusters
```

# `prisma token`

Create a new service token

#### Usage

```sh
prisma token [flags]
```

#### Flags

TODO

# `prisma logs`

Output service logs.

#### Usage

```sh
prisma logs [flags]
```

# `prisma list`

List all deployed services.

#### Usage

```sh
prisma list
```

#### Examples

##### List all deployed services for the currently authenticated user.

```sh
prisma list
```

# `prisma delete`

Delete an existing target from the cluster its deployed to.

#### Usage

```sh
prisma delete [flags]
```

#### Flags

```
 -f, --force               Force delete, without confirmation
 -t, --stage STAGE         Stage name
```

## Data Workflows

# `prisma playground`

Open service endpoints in [GraphQL Playground](https://github.com/graphcool/graphql-playground).

#### Usage

```sh
prisma playground [flags]
```

#### Flags

```
-w, --web                Open browser-based Playground
```


# `prisma import`

Import data into the database of your Prisma service. The data needs to be formatted according to the [Normalized Data Format](!alias-teroo5uxih). For more info, read the [Data Import](!alias-ol2eoh8xie) chapter.

#### Usage

```sh
prisma import [flags]
```

#### Flags

```
-d, --data DATA    (required) Path to zip or folder including data to import
```

# `prisma export`

Exports your service data.

#### Usage

```sh
prisma export [flags]
```

#### Flags

```
-e, --export-path EXPORT-PATH    Path to export .zip file
```

#### Examples

##### Export data

```sh
prisma export -e export.zip
```

# `prisma reset`

Reset the stage data.

#### Usage

```sh
prisma reset [flags]
```

#### Flags

```
-f, --force    Force reset data without confirmation
```

## Cloud

# `prisma login`

Sign up or login (opens your browser for authentication). The platform token that's received after successful login will be stored in `~/.prismarc`.

#### Usage

```sh
prisma login [flags]
```

#### Flags

```
-t, --token TOKEN    System token
```

#### Examples

##### Authenticate using the browser.

```sh
prisma login
```

##### Authenticate using an existing authentication token.

```sh
prisma login -t <token>
```

# `prisma account`

Print account info

## Local Development

# `prisma local start`

Start local development cluster (Docker required).

#### Usage

```sh
prisma local start [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

# `prisma local stop`

Stop local development cluster.

#### Usage

```sh
prisma local stop [flags]
```

#### Flags

```
 -n, --name NAME    Name of the cluster instance
```

# `prisma local upgrade`

Upgrades local cluster to the latest version for the current CLI.

#### Usage

```sh
prisma local pull [flags]
```

#### Flags

```
-n, --name NAME    Name of the cluster instance
```

# `prisma local nuke`

Hard-reset local development cluster.

#### Usage

```sh
prisma local nuke [flags]
```

#### Flags

```
-n, --name NAME    Name of the cluster instance
```

## Clusters

TODO

# `prisma cluster list`

List all clusters.

#### Usage

```sh
prisma cluster list
```
