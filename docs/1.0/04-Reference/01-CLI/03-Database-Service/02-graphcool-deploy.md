---
alias: kee1iedaov
description: Deploys service definition changes
---

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
