---
alias: ioqu7arooj
description: Learn how to synchronize your local changes with the remote Graphcool project.
---

# Deploying a Graphcool service

## Applying changes

After making a change to `graphcool.yml` or any file inside the your local service definition (for example adding a new type or updating the implementation of a managed function), you need to deploy these changes to a specified [_target_](!alias-zoug8seen4).

The only way of doing this is by running the following CLI command:

```sh
graphcool deploy
``` 

> Notice that `graphcool diff` will print all changes between your local service definition and the already deployed service.

## Using the `--force` option

In case your local changes might result in data loss, for example when you're deleting model type, you need to add the `--force` option to the command in order to signal to the CLI that you know what you're doing: `graphcool deploy --force` (or `graphcool deploy -f`).


## Providing migration values

Sometimes your changes require _migration values_ to be added for the deployment. For example, when a you're adding a non-nullable field to a model type for which there are already existing nodes in the database. In these cases, you need to add the `@migrationValue` directory to the corresponding field. You can read more about migration values [here](!alias-paesahku9t).

