import * as os from 'os'
import * as path from 'path'
import * as chalk from 'chalk'
import figures = require('figures')

/*
 * Networking
 */
export const systemAPIEndpoint = 'https://dev.api.graph.cool/system'
export const authEndpoint = 'https://cli-auth-api.graph.cool'

/*
 * File paths / names
 */
export const graphcoolProjectFileName = 'project.graphcool'
export const graphcoolConfigFilePath = path.join(os.homedir(), '.graphcool')
export const projectFileSuffixes = ['.graphql', '.graphcool', '.schema']

/*
 * Terminal output: auth
 */
export const openBrowserMessage = `Authenticating using your browser...`
export const couldNotRetrieveTokenMessage = `\
${chalk.red(figures.cross)}  Oups, something went wrong during authentication.\n\n`

export const authenticationSuccessMessage = `${chalk.green(figures.tick)}  Authenticated successfully\n\n`

/*
 * Terminal output: create
 */
export const creatingProjectMessage = (name: string) => `Creating project ${chalk.bold(name)}...`
export const createdProjectMessage = (name: string, schemaSource: string, projectId: string) => `\
${chalk.green(figures.tick)}  Created project ${chalk.bold(name)} from ${chalk.bold(schemaSource)}. Your endpoints are:
 
  ${chalk.blue(figures.pointer)} Simple API: https://api.graph.cool/simple/v1/${projectId}
  ${chalk.blue(figures.pointer)} Relay API: https://api.graph.cool/relay/v1/${projectId}
\n`

export const couldNotCreateProjectMessage = `\
${chalk.red(figures.cross)}  Oups, something went wrong while creating the project.\n\n`

export const projectAlreadyExistsMessage = `\
${chalk.red(figures.cross)}  ${graphcoolProjectFileName} already exists for the current project. \
Looks like you've already setup your backend.\n\n`

/*
 * Terminal output: push
 */
export const noProjectFileMessage = `\
${chalk.red(figures.cross)}  Please provide a valid project file (${graphcoolProjectFileName}) for the schema migration.\n\n`

export const invalidProjectFileMessage = `\
${chalk.red(figures.cross)}  The project file (${graphcoolProjectFileName}) that you provided doesn't seem to be valid. Please make sure it contains the ID of your project.\n\n`

export const pushingNewSchemaMessage = `\
Migrating the schema in your project...\
`

export const couldNotMigrateSchemaMessage = `
${chalk.red(figures.cross)}  An error occured while trying to migrate the project.\n\n`

export const noActionRequiredMessage = `\
${chalk.red(figures.star)}  The schema you uploaded is identical to the current schema of the project, no action required.\n\n`

export const migrationDryRunMessage = `\
This was a dry run of the migration. Here's the list of actions that would need to be done for the schema migration:\n\n`

export const migrationPerformedMessage = `\
${chalk.green(figures.tick)}  Your schema was successfully updated. Here's the list of actions that were performed for the schema migration:\n\n`

export const migrationErrorMessage = `\
There are issues with the new schema that your provided. Please make sure to fix the following issues before retrying:\n\n`

/*
 * Terminal output: projects
 */
export const projectsMessage = `\
`

export const couldNotFetchProjectsMessage = `\
${chalk.red(figures.cross)}  An error occured while trying to fetch your projects:\n\n`

/*
 * Terminal output: pull
 */

export const fetchingProjectDataMessage = `\
Fetching project data ...\
`

export const noProjectFileMessageFound = `\
${chalk.red(figures.cross)}  There is no project file (project.graphcool) in the current directory.\n\n`


export const noProjectIdMessage = `\
${chalk.red(figures.cross)}  Please provide a valid project Id.\n\n`

export const wroteProjectFileMessage = `\
${chalk.green(figures.tick)}  Your project file was successfully updated.\n\n`


/*
 * Terminal output: general
 */


export const contactUsInSlackMessage = `\
Please try again or get in touch with us if the problem persists: http://slack.graph.cool/\n\n
`