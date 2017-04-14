import * as os from 'os'
import * as path from 'path'
import * as chalk from 'chalk'
import figures = require('figures')

/*
 * Networking
 */
export const systemAPIEndpoint = 'https://api.graph.cool/system'
export const authEndpoint = 'https://cli-auth-api.graph.cool'

/*
 * File paths / names
 */
export const graphcoolProjectFileName = 'project.graphcool'
export const authConfigFilePath = path.join(os.homedir(), '.graphcool')

/*
 * Terminal output: auth
 */
export const openBrowserMessage = `Authenticating using your browser...`
export const couldNotRetrieveTokenMessage = `\
${chalk.red(figures.cross)} Oups, something went wrong during authentication.

Please try again or get in touch with us if the problem persists: http://slack.graph.cool/\n`
export const authenticationSuccessMessage = `${chalk.green(figures.tick)}  Authenticated successfully\n`

/*
 * Terminal output: create
 */
export const creatingProjectMessage = (name: string) => `Creating project ${chalk.bold(name)}...`
export const createdProjectMessage = (name: string, schemaSource: string, projectId: string) => `\
${chalk.green(figures.tick)}  Created project ${chalk.bold(name)} from ${chalk.bold(schemaSource)}. Your endpoints are:
 
  ${chalk.blue(figures.pointer)} Simple API: https://api.graph.cool/simple/v1/${projectId}
  ${chalk.blue(figures.pointer)} Relay API: https://api.graph.cool/relay/v1/${projectId}
`
export const couldNotCreateProjectMessage = `\
${chalk.red(figures.cross)}  Oups, something went wrong while creating the project.

Please try again or get in touch with us if the problem persists: http://slack.graph.cool/\n`

export const projectAlreadyExistsMessage = `\
${chalk.red(figures.cross)}  ${graphcoolProjectFileName} already exists for the current project. \
Looks like you've already setup your backend.\n`

/*
 * Testing
 */
export const testToken = 'abcdefghijklmnopqrstuvwxyz'

export const mockFullSchema = `\
  type Tweet {
    id: ID!
    createdAt: DateTime!
    updatedAt: DateTime!
    text: String!
  }`

export const mockSchema = `\
  type Tweet {
    text: String!
  }`

export const mockedCreateProjectResponse = `\
{
  "addProject": {
    "project": {
      "id": "abcdefghi",
      "schema": "type Tweet {\\n  id: ID!\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n  text: String!\\n}"
    }
  }
}`