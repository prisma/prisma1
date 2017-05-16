# graphcool-cli

## Quickstart

```sh
# Create a new GraphQL backend
graphcool init

# Edit `project.graphcool` to change schema and push updates using...
graphcool push
```

## Install

```sh
npm install -g graphcool
```

## Usage

```sh
  Serverless GraphQL backend for frontend developers (https://www.graph.cool)
  
  Usage: graphcool [command]

  Commands:
    quickstart    Tutorial to get started with Graphcool
    init          Create a new project
    push          Push project file changes
    pull          Download the latest project file
    export        Export project data
    endpoints     Print GraphQL endpoints
    console       Open Graphcool Console
    playground    Open GraphQL Playground
    projects      List projects
    auth          Sign up or login
    version       Print version
    
  Run 'graphcool COMMAND --help' for more information on a command.
  
  Examples:
  
  - Initialize a new Graphcool project
    $ graphcool init
  
  - Local setup of an existing project
    $ graphcool pull -p <project-id | alias>
    
  - Update live project with local changes
    $ graphcool push
```

