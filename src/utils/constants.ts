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
export const projectFileSuffixes = ['.graphql', '.graphcool', '.schema']

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
 * Terminal output: push
 */
export const noProjectFileMessage = `\
Please provide a valid project (${graphcoolProjectFileName}) file for the schema migration.
`
export const pushingNewSchemaMessage = `\
Migrating the schema in your project...
`

export const couldNotMigrateSchemaMessage = `
An error occured while trying to migrate the project.
`

export const noActionRequiredMessage = `\
The schema you uploaded is identical to the current schema of the project, no action required ${figures.tick}
`

export const migrationDryRunMessage = `\
This was a dry run of the migration. Here's the list of actions that would need to be done for the schema migration:
`

export const migrationPerformedMessage = `\
Your schema was successfully updated. Here's the list of actions that were performed for the schema migration:\n
`

export const migrationErrorMessage = `\
There are issues with the new schema that your provided. Please make sure to fix the following issues before retrying:\n
`