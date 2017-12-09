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
