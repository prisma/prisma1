---
alias: zoug8seen4
description: Graphcool allows to deploy a project to multiple environments.
---

# Targets

When working with Graphcool, you can configure different _environments_ that you can deploy your project to. You can use this for example to separate a _development_ from a _production_ environment.

> Notice that the concept of an environment only exists in the _frontend_ (CLI). The backend is not aware of environments and only cares about _projects_.

## `.graphcoolrc`

Environments are managed in the `.graphcoolrc` file that's generated for you when you're creating a new project with `graphcool init`.

### Example

```yml
default: dev
environments:
  dev:
    projectId: cj7n3mp0f0000rayvphelgz7d
    envVars:
      WEBHOOK_URL: https://dev.example.com/webhook
  prod:
    projectId: cj7n3ob1y0001rayvdhegkcu2
    envVars:
      WEBHOOK_URL: https://prod.example.com/webhook
```

### Structure

`.graphcoolrc` is written in YAML and has the following root properties:

| Root Property | Type | Description | 
| --------- | ------------------ | --------------- | 
| `default`| `string` | Specifies the name of the default environment. This environment will be used for all commands you're invoking with the CLI. If you want to run a command against a different environment than the default one, you need to specify the `--env` option for the command. |
| `environments` | `[string:string]` | A list of environments that are available. |


## The `default` environment

Environments are relevant when using the CLI to manage your project. Almost every CLI command takes the `--env <env>` option for you to specify to which environment this command should be applied, for example: `graphcool deploy --env prod`.

When not passing the `--env <env>` option to a CLI command, the CLI checks `.graphcoolrc` for the `default` environment and runs the specified command against it.

## Managing environments

Environments can be managed by manually changing the contents of `.graphcoolrc` or by using the CLI and the `graphcool env` command. `graphcool env` has four dedicated subcommands that you can use to alter the contents of `.graphcoolrc`:

- `graphcool env default <env>`: Sets the `default` environment
- `graphcool env set <env>`: Adds an environment to the `environments` list
- `graphcool env remove <env>`: Removes an environment from the `environments` list
- `grahcool env rename <oldname> <newname>`: Renames an environment from the `environments` list
