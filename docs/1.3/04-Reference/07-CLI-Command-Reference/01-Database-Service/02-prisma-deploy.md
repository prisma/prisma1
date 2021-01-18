---
alias: kee1iedaov
description: Deploys service definition changes
---

# `prisma deploy`

Deploys service definition changes. Every time you're making a local change to any file in the [service configuration](!alias-ieshoo5ohm) (including the [data model](!alias-eiroozae8u)), you need to synchronize these changes with the remote service using this command.

Upon the very first deploy of a service, the command will perform initial data seeding if the [`seed`](!alias-ufeshusai8#seed-optional) property in `prisma.yml` is provided. You can prevent this by passing the `--no-seed` option.

If no [`cluster`](!alias-ufeshusai8#cluster-optional) property is specifed in `prisma.yml`, the command will prompt you to interactively select a [cluster](!alias-eu2ood0she) as a deployment target. After you selected the cluster, it will write it to `prisma.yml` as the default deployment target for future deploys. To bring up the interactive prompt again, simply remove the `cluster` property from `prisma.yml` manually.

<!-- If no [`cluster`](!alias-ufeshusai8#cluster-optional) property is specifed in `prisma.yml`, the command will prompt you to interactively select a [cluster](!alias-eu2ood0she) as a deployment target. After you selected the cluster, it will write it to `prisma.yml` as the default deployment target for future deploys. To bring up the interactive prompt again, either remove the `cluster` property manually or pass the `--interactive` option to the command. -->

#### Usage

```sh
prisma deploy [flags]
```

#### Flags

```
-d, --dry-run              Perform a dry-run of the deployment
-e, --env-file ENV-FILE    Path to .env file to inject env vars
-f, --force                Accept data loss caused by schema changes
-j, --json                 JSON Output
-w, --watch                Watch for changes
--no-seed                  Disable seed on initial service deploy
```

<!--

```
-d, --dry-run              Perform a dry-run of the deployment
-e, --env-file ENV-FILE    Path to .env file to inject env vars
-f, --force                Accept data loss caused by schema changes
-i, --interactive          Force interactive mode to select the cluster
-j, --json                 JSON Output
-w, --watch                Watch for changes
--no-seed                  Disable seed on initial service deploy
```

-->

#### Examples

##### Deploy service.

```sh
prisma deploy
```

##### Deploy service and interactively select a cluster as deployment target.

```sh
prisma deploy --interactive
```

##### Deploy service with environment variables specified in `.env.prod`.

```sh
prisma deploy --env-file .env.prod
```
