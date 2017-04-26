import * as chalk from 'chalk'

export const usageRoot = `  Usage: graphcool [command]
  
    ${chalk.bold('Serverless GraphQL database for frontend developers')}
    Read more at https://www.graph.cool/docs/cli

  Commands:
    init         Create a new project
    pull         Pull the latest project config
    push         Push project config changes
    export       Export database
    projects     List projects
    auth         Sign up or login
    version      Print version

  Run 'graphcool COMMAND --help' for more information on a command.
`

export const usageInit = `  Usage: graphcool init [options]
  
  Create a new project from scratch or based on an existing GraphQL schema.
  
  Options:
    -u, --url <schema-url>    Url to a GraphQL schema
    -f, --file <schema-file>  Local GraphQL schema file
    -n, --name <name>         Project name
    -a, --alias <alias>       Project alias
    -r, --region <region>     AWS Region (default: us-west-1)
    -h, --help                Output usage information
    
  Note: This command will create a ${chalk.bold('project.graphcool')} config file in the current directory.
`

export const usagePull = `  Usage: graphcool pull [options]
  
  Pull the latest project config from Graphcool
  
  Options:
    -p, --project      ID or alias of source project (defaults to project from config file)
    -c, --config       Config file (default: project.graphcool)
    -h, --help         Output usage information
`

export const usagePush = `  Usage: graphcool push [options]
  
  Push project config changes
  
  Options:
    -c, --config       Config file (default: project.graphcool)
    -d, --dry-run      Simulate command
    -f, --force        Don't prompt for confirmation
    -h, --help         Output usage information
`

export const usageAuth = `  Usage: graphcool auth [options]
  
  Sign up or login (opens your browser for authentication)
  
  Options:
    -t, --token <token>    System token
    -h, --help             Output usage information
    
  Note: Your session token will be store at ~/.graphcool
`

export const usageProjects = `  Usage: graphcool projects [options]
  
  List projects
  
  Options:
    -h, --help         Output usage information
`
