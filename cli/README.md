# graphcool-cli [![npm version](https://badge.fury.io/js/graphcool.svg)](https://badge.fury.io/js/graphcool) [![CircleCI](https://circleci.com/gh/graphcool/graphcool-cli.svg?style=svg)](https://circleci.com/gh/graphcool/graphcool-cli)

📟 Manage your GraphQL backend from your terminal

## Features

* Instantly setup a production-ready GraphQL backend
* **Edit your schema locally** and version control changes via [GraphQL SDL](https://www.graph.cool/docs/faq/graphql-sdl-schema-definition-language-kr84dktnp0/)
* Supports [multi-stage](https://www.graph.cool/docs/tutorials/cli-multi-staging-workflow-ex4wo4zaep/) environment workflows

## Quickstart

Watch this [video tutorial](https://www.youtube.com/watch?v=sf0ZkyalSTg) 🎥 to get started with the CLI and follow the frontend [quickstart guide](https://www.graph.cool/docs/quickstart/).

```sh
# Create a new GraphQL backend
graphcool init

# Edit `project.graphcool` to change schema and push updates using...
graphcool push
```

Check out the CLI in action:

![](http://imgur.com/PABWSL7.gif)

## Install

Installs the global `graphcool` command via NPM (or Yarn).

```sh
npm install -g graphcool
```

## Usage

```sh

  Serverless GraphQL backend for frontend developers (https://www.graph.cool)
  
  Usage: graphcool [command]

  Commands:
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

## Resources

* [Quickstart](https://www.graph.cool/docs/quickstart/): Using GraphQL with React, VueJS, Apollo, Relay & more
* [Exploring Graphcool: The Serverless GraphQL Backend](https://www.graph.cool/docs/tutorials/graphcool-features-overview-ped6wohw0o/)
* [Using the Graphcool CLI for a multi-stage development workflow](https://www.graph.cool/docs/tutorials/cli-multi-staging-workflow-ex4wo4zaep/)


## Help & Community [![Slack Status](https://slack.graph.cool/badge.svg)](https://slack.graph.cool)

Join our [Slack community](http://slack.graph.cool/) if you run into issues or have questions. We love talking to you!

![](http://i.imgur.com/5RHR6Ku.png)
