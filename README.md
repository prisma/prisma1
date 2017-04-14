# graphcool-cli

## Usage

```sh
  Usage: graphcool [command]
  
    Serverless GraphQL database for frontend developers
    Read more at https://www.graph.cool/docs/cli

  Commands:
    auth         Sign up or login
    create       Create a new project
    eject        Eject config from Schema format to YAML
    export       Export database
    fetch        Fetch newest config for project
    import       Import data
    update       Apply config changes to project
    version      Print version

  Run 'graphcool COMMAND --help' for more information on a command.

```

### `create`

```sh
  Usage: graphcool create [options]
  
  Create a new project (uses graphcool.schema as template if exists)
  
  Options:
    -n, --name <name>      Project name
    -a, --alias <alias>    Project alias (https://api.graph.cool/ENDPOINT/ALIAS)
    -r, --region <region>  AWS Region (default: us-west-1)
    -h, --help             Output usage information
  
```

### `update`

```sh
  Usage: graphcool update [options]
  
  Apply config changes to project
  
  Options:
    -d, --dry-run               Perform a dry-run
    -c, --config <path>         Config file (default: graphcool.schema or graphcool.yaml)
    -h, --help                  Output usage information
  
```


### `fetch`

```sh
  Usage: graphcool fetch
  
  Fetch newest config for project 
  
  Options:
    -p, --project-id   Project id to fetch (defaults to project from config)
    -h, --help         Output usage information
  
```


