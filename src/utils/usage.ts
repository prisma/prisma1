import * as chalk from 'chalk'

export const usageRoot = () => `
  Serverless GraphQL backend for frontend developers (${chalk.underline('https://www.graph.cool')})

  ${chalk.dim('Usage:')} ${chalk.bold('graphcool')} [command]

  ${chalk.dim('Commands:')}
    init          Create a new project
    env           Manage project environments
    push          Push project file changes
    pull          Download the latest project file
    import        Import project data
    export        Export project data
    logs          View logs
    info          Print project info (environments, endpoints, ...)
    playground    Open GraphQL Playground
    console       Open Graphcool Console in browser
    projects      List all projects
    delete        Delete one or more projects
    auth          Create account or login
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

export const usageEnvironment = `
  Usage: graphcool env COMMAND
  
  Manage project environment.
  
  ${chalk.dim('Commands:')}
    set         Sets a project environment
    rename      Renames a project environment
    remove      Removes a project environment
    default     Set the default environment
  
  ${chalk.dim('Options:')}
    -h, --help  Output usage information

`

export const usageSetEnvironment = `
  Usage: graphcool env set <environment-name> <project-id>
  
  Adds a project environment based on a project id.

`

export const usageRenameEnvironment = `
  Usage: graphcool env rename <old-name> <new-name>
  
  Renames a project environment

`

export const usageRemoveEnvironment = `
  Usage: graphcool env remove <environment-name>

  Removes a project environment

`

export const usageDefaultEnvironment = `
  Usage: graphcool env default <environment-name>

  Set the default environment

`

export const usageInit = `
  Usage: graphcool init [options]
  
  Create a new project definition and environment from scratch or based on an existing GraphQL schema.
  
  Options:
    -n, --name <name>          Project name
    -a, --alias <alias>        Project alias
    -e, --env <environment>    Local environment name for the project (e.g. "dev" or "production")
    -r, --region <region>      AWS Region (options: us-west-2 (default), eu-west-1, ap-northeast-1)
    -h, --help                 Output usage information
      
  ${chalk.dim('Examples:')}
  
  ${chalk.gray('-')} Initialize a new Graphcool project
    ${chalk.cyan('$ graphcool init')}
    
  ${chalk.gray('-')} Create a new project based on a schema
    ${chalk.cyan('$ graphcool init')}

`

export const usagePull = `
  Usage: graphcool pull [options]
  
  Pull the latest project definition from Graphcool
  
  Options:
    -p, --project <id | alias>  ID or alias of  project to delete (deprecated, use -e in the future)
    -e, --env <environment>     Name of the project environment to pull from
    -f, --force                 Override project file
    -h, --help                  Output usage information
     
  ${chalk.dim('Examples:')}
  
  ${chalk.gray('-')} Download latest project file for default project and the default environment (written to graphcool.yml)
    ${chalk.cyan('$ graphcool pull')}
  
  ${chalk.gray('-')} Download latest definition for specific project environment (written to graphcool.yml)
    ${chalk.cyan('$ graphcool pull --env <environment-name>')}
    
`

export const usagePush = `
  Usage: graphcool push [options]
  
  Push project definition changes
  
  Options:
    -p, --project <id | alias>  ID or alias of  project to delete (deprecated, use -e in the future)
    -e, --env <environment>     Project environment to be pushed to
    -f, --force                 Accept data loss caused by schema changes
    -h, --help                  Output usage information
     
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Push local changes from graphcool.yml to the default project environment.
    ${chalk.cyan('$ graphcool push')}
  
  ${chalk.gray('-')} Push local changes to a specific environment
    ${chalk.cyan('$ graphcool push --env production')}
      
  ${chalk.gray('-')} Push local changes from default project file accepting potential data loss caused by schema changes
    ${chalk.cyan('$ graphcool push --force --env production')}
    
`

export const usageExport = `
  Usage: graphcool export [options]
  
  Export project data
  
  Options:
 
    -e, --env                   Project environment
    -p, --project <id | alias>  ID or alias of  project to delete
    -h, --help                  Output usage information
     
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Export data from default project environment
    ${chalk.cyan('$ graphcool export')}
  
  ${chalk.gray('-')} Export data from specific project environment
    ${chalk.cyan('$ graphcool export --env dev')}

`

export const usageLogs = `
  Usage: graphcool logs COMMAND

  View logs.

  ${chalk.dim('Commands:')}
    function <function-name>   View function execution logs
    requests                   View request logs (COMING SOON)

  ${chalk.dim('Options:')}
    -h, --help  Output usage information

`

export const usageLogsFunction = `
  Usage: graphcool logs function <function-name>

  View function execution logs.

  Options:
 
    --no-color            Produce monochrome output
    -f, --follow          Follow log output

`

export const usageDelete = `
  Usage: graphcool delete [options] 
  
  Delete a Graphcool project
  
  Options:
 
    -e, --env                   Project environment
    -p, --project <id | alias>  ID or alias of  project to delete
    -h, --help                  Output usage information
     
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Delete project with ID <project-id>
    ${chalk.cyan('$ graphcool delete -p <project-id>')}
  
  ${chalk.gray('-')} Select which projects to delete (interactive)
    ${chalk.cyan('$ graphcool delete ')}
`



export const usageInfo = `
  Usage: graphcool info [options]
  
  Print project info (environments, endpoints, ...) 
  
  Options:
    -e, --env             Project environment to get the endpoints from.
    -h, --help            Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Print project info for the default project environment
    ${chalk.cyan('$ graphcool info')}
  
  ${chalk.gray('-')} Print project info for a specific project environment
    ${chalk.cyan('$ graphcool info -e dev')}

`

export const usageConsole = `
  Usage: graphcool console [options]
  
  Open default project in Graphcool Console with your browser

  Options: 
    -e, --env              Project environment to open the console for.
    -h, --help             Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Open console for the default project environment
    ${chalk.cyan('$ graphcool console')}
  
  ${chalk.gray('-')} Open console for specific project environment
    ${chalk.cyan('$ graphcool console -e dev')}
    
`

export const usagePlayground = `
  Usage: graphcool console [options]
  
  Open default project in Graphcool Playground with your browser

  Options: 
    -e, --env              Project environment to open the console for.
    -h, --help             Output usage information
    
  ${chalk.dim('Examples:')}
      
  ${chalk.gray('-')} Open playground for the default project environment
    ${chalk.cyan('$ graphcool playground')}
  
  ${chalk.gray('-')} Open playground for a specific project environment
    ${chalk.cyan('$ graphcool playground -e dev')}    
    
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
