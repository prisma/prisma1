import * as chalk from 'chalk'

export const usageRoot = `  Usage: ${chalk.bold('graphcool')} [command]
  
    ${chalk.bold('Serverless GraphQL backend for frontend developers')}
    Read more at https://www.graph.cool/docs/cli

  ${chalk.dim('Commands:')}
    init           Create a new project
    pull           Pull the latest project config
    push           Push project config changes
    export         Export project data
    endpoints      Print GraphQL endpoints
    console        Open Graphcool Console
    projects       List projects
    auth           Sign up or login
    version        Print version
    
  Run 'graphcool COMMAND --help' for more information on a command.
  
  ${chalk.dim('Examples:')}
  
  ${chalk.gray('-')} Initialize a new Graphcool project
    ${chalk.cyan('$ graphcool init')}
  
  ${chalk.gray('-')} Local setup of an existing project
    ${chalk.cyan('$ graphcool pull -p <project-id | alias>')}
    
  ${chalk.gray('-')} Update live project with local changes
    ${chalk.cyan('$ graphcool push')}
`

export const usageInit = `  Usage: graphcool init [options]
  
  Create a new project from scratch or based on an existing GraphQL schema.
  
  Options:
    -u, --url <schema-url>    Url to a GraphQL schema
    -f, --file <schema-file>  Local GraphQL schema file
    -n, --name <name>         Project name
    -a, --alias <alias>       Project alias
    -r, --region <region>     AWS Region (default: us-west-2)
    -h, --help                Output usage information
    
  Note: This command will create a ${chalk.bold('project2.graphcool')} config file in the current directory.
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
    -d, --dry-run      Simulate command
    -c, --config       Config file (default: project.graphcool)
    -h, --help         Output usage information
`

export const usageExport = `  Usage: graphcool export [options]
  
  Export project data
  
  Options:
    -c, --config       Config file (default: project.graphcool)
    -h, --help         Output usage information
`

export const usageConsole = `  Usage: graphcool console [options]
  
  Open current project in Graphcool Console with your browser

  Options:
    -h, --help         Output usage information
`

export const usageProjects = `  Usage: graphcool projects [options]
  
  List projects
  
  Options:
    -h, --help         Output usage information
`

export const usageAuth = `  Usage: graphcool auth [options]
  
  Sign up or login (opens your browser for authentication)
  
  Options:
    -t, --token <token>    System token
    -h, --help             Output usage information
    
  Note: Your session token will be store at ~/.graphcool
`

export const usageVersion = `  Usage: graphcool version [options]
  
  Print version
  
  Options:
    -h, --help         Output usage information
`
