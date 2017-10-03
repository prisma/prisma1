---
alias: ioqu7arooj
description: Learn how to synchronize your local changes with the remote Graphcool project.
---

# Applying project changes

After making a change to the project definition or any file inside the file structure of your local project (e.g. making changes to the implementation of a serverless function), you need to sync these changes to the remote project that lives in your Graphcool account.

The only way of doing this is by running the `graphcool deploy` command. 

In case your local changes might result in data loss, you need to add the `--force` option to the command in order to signal to the CLI that you know what you're doing: `graphcool deploy --force`.

Sometimes your changes require _migration values_ to be added for the deployment. For example, when a you're adding a non-nullable field to a model type for which there are already existing nodes in the database.

Notice that `graphcool diff` will print all changes between your local project and the remote project in your Graphcool account.