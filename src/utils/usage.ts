import * as chalk from 'chalk'

export const usageRoot = `
  Serverless GraphQL backend for frontend developers (${chalk.underline('https://www.graph.cool')})
  
  ${chalk.dim('Usage:')} ${chalk.bold('graphcool')} [command]

  ${chalk.dim('Commands:')}
    init         Create a new project
    push         Push project file changes
    pull         Pull the latest project file
    export       Export project data
    endpoints    Print GraphQL endpoints
    console      Open Graphcool Console
    playground   Open GraphQL Playground
    projects     List projects
    auth         Sign up or login
    version      Print version
    
  Run 'graphcool COMMAND --help' for more information on a command.
  
  ${chalk.dim('Examples:')}
  
  ${chalk.gray('-')} Initialize a new Graphcool project
    ${chalk.cyan('$ graphcool init')}
  
  ${chalk.gray('-')} Local setup of an existing project
    ${chalk.cyan('$ graphcool pull -p <project-id | alias>')}
    
  ${chalk.gray('-')} Update live project with local changes
    ${chalk.cyan('$ graphcool push')}
    
`

export const usageInit = `
  Usage: graphcool init [options]
  
  Create a new project from scratch or based on an existing GraphQL schema.
  
  Options:
    -s, --schema <path>        Path (local file or remote URL) to a GraphQL schema (ends with .graphql)
    -c, --copy <id | alias>    ID or alias of the project to be copied
    --copy-opts <options>      Include items for copy (options: data, mutation-callbacks, all (default), none)
    -n, --name <name>          Project name
    -a, --alias <alias>        Project alias
    -o, --output <path>        Path to output project file (default: project.graphcool)
    -r, --region <region>      AWS Region (options: us-west-2 (default), eu-west-1, ap-northeast-1)
    -h, --help                 Output usage information
    
  Note: This command will create a ${chalk.bold('project.graphcool')} project file in the current directory.
  
`

export const usagePull = `
  Usage: graphcool pull [options] [<project-file>] 
  
  Pull the latest project file from Graphcool
  
  Options:
    -p, --project <id | alias>  ID or alias of source project (defaults to ID or alias from project file)
    -o, --output <path>         Path to output project file (default: project.graphcool)
    -f, --force                 Override project file
    -h, --help                  Output usage information
   
`

export const usagePush = `
  Usage: graphcool push [options] [<project-file>]  
  
  Push project file changes
  
  Options:
    -f, --force           Accept data loss caused by schema changes
    -h, --help            Output usage information
    
`

export const usageExport = `
  Usage: graphcool export [options] [<project-file>] 
  
  Export project data
  
  Options:
 
    -h, --help            Output usage information
    
`

export const usageStatus = `
  Usage: graphcool status [options] [<project-file>] 
  
  Display difference between local and remote schema versions
  
  Options:
 
    -h, --help            Output usage information
    
`


export const usageEndpoints = `
  Usage: graphcool endpoints [options] [<project-file>] 
  
  Export project data 
  
  Options:
 
    -h, --help            Output usage information
    
`

export const usageConsole = `
  Usage: graphcool console [options] [<project-file>] 
  
  Open current project in Graphcool Console with your browser

  Options: 
    -h, --help             Output usage information
    
`

export const usagePlayground = `
  Usage: graphcool console [options]
  
  Open current project in Graphcool Playground with your browser

  Options: 
    -h, --help             Output usage information
    
`

export const usageProjects = `
  Usage: graphcool projects [options]
  
  List projects
  
  Options:
    -h, --help         Output usage information
    
`

export const usageAuth = `
  Usage: graphcool auth [options]
  
  Sign up or login (opens your browser for authentication)
  
  Options:
    -t, --token <token>    System token
    -h, --help             Output usage information
    
  Note: Your session token will be store at ~/.graphcool
  
`

export const usageVersion = `
  Usage: graphcool version [options]
  
  Print version
  
  Options:
    -h, --help         Output usage information
    
`
