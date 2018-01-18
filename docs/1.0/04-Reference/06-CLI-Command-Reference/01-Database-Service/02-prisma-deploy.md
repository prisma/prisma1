---
alias: kee1iedaov
description: Deploys service definition changes
---

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
--dotenv DOTENV                                  Path to .env file to inject env vars
--no-seed                                        Disable seed on initial service deploy
```

#### Examples

##### Deploy service

```sh
prisma deploy
```

##### Deploy service with specific environment variables

```sh
prisma deploy --dotenv .env.prod
```
