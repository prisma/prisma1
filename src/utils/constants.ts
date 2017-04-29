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
export const graphcoolConfigFilePath = path.join(os.homedir(), '.graphcool')
export const projectFileSuffixes = ['.graphql', '.graphcool', '.schema']

/*
 * Terminal output: auth
 */
export const openBrowserMessage = `Authenticating using your browser...`
export const couldNotRetrieveTokenMessage = `\
${chalk.red(figures.cross)}  Whoops, something went wrong during authentication.\n`

export const authenticationSuccessMessage = `${chalk.green(figures.tick)}  Authenticated successfully\n`

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
${chalk.red(figures.cross)}  Whoops, something went wrong while creating the project.\n`

export const projectAlreadyExistsMessage = `\
${chalk.red(figures.cross)}  ${graphcoolProjectFileName} already exists for the current project. \
Looks like you've already setup your backend.\n`

/*
 * Terminal output: push
 */
export const noProjectFileMessage = `\
${chalk.red(figures.cross)}  Please provide a valid project file (${graphcoolProjectFileName}) for the schema migration.\n`

export const invalidProjectFileMessage = `\
${chalk.red(figures.cross)}  The project file (${graphcoolProjectFileName}) that you provided doesn't seem to be valid. Please make sure it contains the ID of your project.\n`

export const pushingNewSchemaMessage = `\
Migrating the schema in your project...\
`

export const couldNotMigrateSchemaMessage = `
${chalk.red(figures.cross)}  An error occured while trying to migrate the project.\n`

export const noActionRequiredMessage = `\
${chalk.green(figures.tick)}  Identical schema, no action required.\n`

export const migrationDryRunMessage = `\
This was a dry run of the migration. Here are the potential changes:\n`

export const migrationPerformedMessage = `\
${chalk.green(figures.tick)}  Your schema was successfully updated. Here are the changes:\n`

export const migrationErrorMessage = `\
There are issues with the new schema that you provided:\n`

/*
 * Terminal output: projects
 */
export const projectsMessage = `\
`

export const couldNotFetchProjectsMessage = `\
${chalk.red(figures.cross)}  An error occured while trying to fetch your projects.\n`

/*
 * Terminal output: pull
 */

export const fetchingProjectDataMessage = `\
Fetching project data ...`

export const noProjectFileMessageFound = `\
${chalk.red(figures.cross)}  There is no project file (project.graphcool) in the current directory.\n`

export const noProjectIdMessage = `\
${chalk.red(figures.cross)}  Please provide a valid project ID.\n`

export const wroteProjectFileMessage = `\
${chalk.green(figures.tick)}  Your project file was successfully updated.`

/*
 * Terminal output: export
 */

export const exportingDataMessage = `\
Exporting your project data ...`

export const downloadUrlMessage = (url: string) => `\
${chalk.green(figures.tick)}  You can download your project data by pasting this URL in a browser:
 
  ${chalk.blue(figures.pointer)} Download URL: ${url}
\n`


/*
 * Terminal output: general
 */

export const contactUsInSlackMessage = `\
Get in touch if you need help: http://slack.graph.cool/`