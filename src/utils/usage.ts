import * as chalk from 'chalk'

export const usageRoot = (showQuickstart: boolean) => `
  Serverless GraphQL backend for frontend developers (${chalk.underline('https://www.graph.cool')})
  
  ${chalk.dim('Usage:')} ${chalk.bold('graphcool')} [command]

  ${chalk.dim('Commands:')}${showQuickstart ? `
    quickstart    Open Graphcool Quickstart examples`: ''}
    init          Create a new project
    push          Push project file changes
    pull          Download the latest project file
    export        Export project data
    endpoints     Print GraphQL endpoints
    console       Open Graphcool Console
    playground    Open GraphQL Playground
    projects      List projects
    delete        Delete existing projects
    auth          Sign up or login
    version       Print version
    
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
    -s, --schema <path | url>  Path / URL to a GraphQL schema (ends with .graphql)
    -c, --copy <id | alias>    ID or alias of the project to be copied
    --copy-opts <options>      Include items for copy (options: data, mutation-callbacks, all (default), none)
    -n, --name <name>          Project name
    -a, --alias <alias>        Project alias
    -o, --output <path>        Path to output project file (default: project.graphcool)
    -r, --region <region>      AWS Region (options: us-west-2 (default), eu-west-1, ap-northeast-1)
    -h, --help                 Output usage information
      
  ${chalk.dim('Examples:')}
  
  ${chalk.gray('-')} Initialize a new Graphcool project
    ${chalk.cyan('$ graphcool init')}
    
  ${chalk.gray('-')} Create a new project based on a schema
    ${chalk.cyan('$ graphcool init --schema <path | url>')}
  
  ${chalk.gray('-')} Create a copy of an existing project
    ${chalk.cyan('$ graphcool init --copy <project-id | alias>')}

`

export const usagePull = `
  Usage: graphcool pull [options] [<project-file>] 
  
  Pull the latest project file from Graphcool
  
  Options:
    -p, --project <id | alias>  ID or alias of source project (defaults to ID or alias from project file)
    -o, --output <path>         Path to output project file (default: project.graphcool)
    -f, --force                 Override project file
    -h, --help                  Output usage information
     
  ${chalk.dim('Examples:')}
  
  ${chalk.gray('-')} Download latest project file for current project ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool pull')}
  
  ${chalk.gray('-')} Download latest project for specific project (written to project.graphcool)
    ${chalk.cyan('$ graphcool pull --project <project-id | alias>')}
    
  ${chalk.gray('-')} Download latest project for specific project (written to example.graphcool)
    ${chalk.cyan('$ graphcool pull --project <project-id | alias> --output example.graphcool')}
    
`

export const usagePush = `
  Usage: graphcool push [options] [<project-file>]  
  
  Push project file changes
  
  Options:
    -f, --force           Accept data loss caused by schema changes
    -h, --help            Output usage information    
     
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Push local changes from current project file ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool push')}
  
  ${chalk.gray('-')} Push local changes from specific project file 
    ${chalk.cyan('$ graphcool push <project-file>')}
      
  ${chalk.gray('-')} Push local changes from current project file accepting potential data loss caused by schema changes
    ${chalk.cyan('$ graphcool push --force <project-file>')}
    
`

export const usageExport = `
  Usage: graphcool export [options] [<project-file>] 
  
  Export project data
  
  Options:
 
    -h, --help            Output usage information
     
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Export data from current project ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool export')}
  
  ${chalk.gray('-')} Export data from specific project file 
    ${chalk.cyan('$ graphcool export <project-file>')}

`

export const usageDelete = `
  Usage: graphcool delete [options] 
  
  Delete a Graphcool project
  
  Options:
 
    -p, --project <id | alias>  ID or alias of  project to delete
    -h, --help                  Output usage information
     
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Delete project with ID <project-id>
    ${chalk.cyan('$ graphcool delete -p <project-id>')}
  
  ${chalk.gray('-')} Select which projects to delete (interactive)
    ${chalk.cyan('$ graphcool delete ')}
`


export const usageStatus = `
  Usage: graphcool status [options] [<project-file>] 
  
  Display difference between local and remote schema versions
  
  Options:
 
    -h, --help            Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Status for current project ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool status')}
  
  ${chalk.gray('-')} Status for specific project
    ${chalk.cyan('$ graphcool status <project-file>')}

`


export const usageEndpoints = `
  Usage: graphcool endpoints [options] [<project-file>] 
  
  Export project data 
  
  Options:
 
    -h, --help            Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Display API endpoints for current project ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool endpoints')}
  
  ${chalk.gray('-')} Display API endpoints for specific project
    ${chalk.cyan('$ graphcool endpoints <project-file>')}

`

export const usageConsole = `
  Usage: graphcool console [options] [<project-file>] 
  
  Open current project in Graphcool Console with your browser

  Options: 
    -h, --help             Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Open console for current project ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool console')}
  
  ${chalk.gray('-')} Open console for specific project
    ${chalk.cyan('$ graphcool console <project-file>')}
    
`

export const usagePlayground = `
  Usage: graphcool console [options] [<project-file>]
  
  Open current project in Graphcool Playground with your browser

  Options: 
    -h, --help             Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Open playground for current project ([<project-file>] defaults to project.graphcool)
    ${chalk.cyan('$ graphcool playground')}
  
  ${chalk.gray('-')} Open playground for specific project
    ${chalk.cyan('$ graphcool playground <project-file>')}    
    
`

export const usageProjects = `
  Usage: graphcool projects [options]
  
  List projects
  
  Options:
    -h, --help         Output usage information
    
  ${chalk.dim('Example:')}
      
  ${chalk.gray('-')} Display all projects in your account
    ${chalk.cyan('$ graphcool projects')}
        
`

export const usageAuth = `
  Usage: graphcool auth [options]
  
  Sign up or login (opens your browser for authentication)
  
  Options:
    -t, --token <token>    System token
    -h, --help             Output usage information
    
  Note: Your session token will be store at ~/.graphcool
  
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Authenticate using the browser
    ${chalk.cyan('$ graphcool auth')}
  
  ${chalk.gray('-')} Authenticate using an existing token
    ${chalk.cyan('$ graphcool auth -t <token>')}    
  
`

export const usageQuickstart = `
  Usage: graphcool quickstart [options]
  
  Print version of Graphcool CLI
  
  Options:
    -h, --help         Output usage information
            
`

export const usageVersion = `
  Usage: graphcool version [options]
  
  Tutorial to get started with Graphcool
  
  Options:
    -h, --help         Output usage information
            
`
