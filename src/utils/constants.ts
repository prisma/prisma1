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
${chalk.red(figures.cross)}  Please provide a valid project file (${graphcoolProjectFileName}) for the schema migration.
`

export const invalidProjectFileMessage = `\
${chalk.red(figures.cross)}  The project file (${graphcoolProjectFileName}) that you provided doesn't seem to be valid. Please make sure it contains the ID of your project.
`

export const pushingNewSchemaMessage = `\
Migrating the schema in your project...\
`

export const couldNotMigrateSchemaMessage = `
${chalk.red(figures.cross)}  An error occured while trying to migrate the project.
`

export const noActionRequiredMessage = `\
${chalk.red(figures.star)}  The schema you uploaded is identical to the current schema of the project, no action required.
`

export const migrationDryRunMessage = `\
${chalk.red(figures.star)}  This was a dry run of the migration. Here's the list of actions that would need to be done for the schema migration:
`

export const migrationPerformedMessage = `\
${chalk.green(figures.tick)}  Your schema was successfully updated. Here's the list of actions that were performed for the schema migration:\n
`

export const migrationErrorMessage = `\
${chalk.red(figures.cross)}  There are issues with the new schema that your provided. Please make sure to fix the following issues before retrying:\n
`

/*
 * Terminal output: projects
 */
export const projectsMessage = `\
`

export const couldNotFetchProjectsMessage = `\
${chalk.red(figures.cross)}  An error occurded while trying to fetch your projects: 
`

/*
 * Terminal output: pull
 */

export const fetchingProjectDataMessage = `\
Fetching project data ...\
`

export const noProjectIdMessage = `\
${chalk.red(figures.cross)}  Please provide a valid project Id.
`

export const wroteProjectFileMessage = `\
${chalk.green(figures.tick)}  Your project file was successfully updated.
`