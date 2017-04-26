import * as chalk from 'chalk'

export const usageRoot = `  Usage: graphcool [command]
  
    ${chalk.bold('Serverless GraphQL database for frontend developers')}
    Read more at https://www.graph.cool/docs/cli

  Commands:
    auth         Sign up or login
    create       Create a new project
    eject        Eject config from Schema format to YAML
    export       Export database
    fetch        Fetch newest config for project
    import       Import data
    projects     List projects
    migrate      Apply config changes to project
    version      Print version

  Run 'graphcool COMMAND --help' for more information on a command.
`

export const usageAuth = `  Usage: graphcool auth [options]
  
  Sign up or login
  
  Options:
    -t, --token <token>    System token
    -h, --help             Output usage information
`

export const usageCreate = `  Usage: graphcool create [options] [schema]
  
  Create a new project (uses graphcool.schema as template if exists)
  
  The [schema] argument is optional but can point to any kind of .schema file or URL.
  
  Options:
    -n, --name <name>      Project name
    -a, --alias <alias>    Project alias (https://api.graph.cool/ENDPOINT/ALIAS)
    -r, --region <region>  AWS Region (default: us-west-1)
    -h, --help             Output usage information
`

export const usagePull = `  Usage: graphcool pull
  
  Fetch newest config for project 
  
  Options:
    -p, --project-id   Project id to fetch (defaults to project from config in current directory)
    -h, --help         Output usage information
`

export const usageProjects = `  Usage: graphcool projects
  
  List projects
  
  Options:
    -h, --help         Output usage information
`
