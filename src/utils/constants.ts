import * as os from 'os'
import * as path from 'path'
import * as chalk from 'chalk'
import figures = require('figures')

/*
 * Networking & URLs
 */
export const systemAPIEndpoint = 'https://api.graph.cool/system'
export const authEndpoint = 'https://cli-auth-api.graph.cool'
export const consoleURL = (token: string, projectName?: string) =>
  `https://console.graph.cool/token/?token=${token}${projectName ? `&redirect=/${encodeURIComponent(projectName)}` : ''}`
export const sampleSchemaURL = `http://graphqlbin.com/empty.graphql`

/*
 * Sentry
 */
// DSN: https:df8b750d27504597af166dc757a890ed//@sentry.io/165377
export const sentryKey = 'df8b750d27504597af166dc757a890ed'
export const sentryId = '165377'


/*
 * File paths / names
 */
export const graphcoolProjectFileName = 'project.graphcool'
export const graphcoolConfigFilePath = path.join(os.homedir(), '.graphcool')
export const projectFileSuffix = '.graphcool'
export const schemaFileSuffix = '.graphql'

export const instagramExampleSchemaUrl = 'http://graphqlbin.com/instagram.graphql'
export const exampleSchema = `\
type User {
  id: ID!
}

# type Tweet {
#   text: String!
# }`

/*
 * Terminal output: auth
 */
export const openBrowserMessage = `You need to authenticate. Your browser will open shortly...`
export const couldNotRetrieveTokenMessage = `\
Whoops, something went wrong during authentication.\n`

export const authenticationSuccessMessage = ` ${chalk.green(figures.tick)}  Authenticated successfully\n`

/*
 * Terminal output: create
 */
export const creatingProjectMessage = (name: string) => `Creating project ${chalk.bold(name)}...`
export const createdProjectMessage = (name: string, schemaSource: string, projectId: string, projectFileContent: string) => `\
 ${chalk.green(figures.tick)} Created project ${chalk.bold(name)} from ${chalk.bold(schemaSource)}. Your endpoints are:
 
   ${chalk.blue(figures.pointer)} Simple API: https://api.graph.cool/simple/v1/${projectId}
   ${chalk.blue(figures.pointer)} Relay API: https://api.graph.cool/relay/v1/${projectId}

  Your project.graphcool file looks like:
  ${projectFileContent}
  `

export const couldNotCreateProjectMessage = `\
Whoops, something went wrong while creating the project.\n`

export const projectAlreadyExistsMessage = `\
${graphcoolProjectFileName} already exists for the current project. \
Looks like you've already setup your backend.\n`

export const howDoYouWantToGetStarted = () => `\

  You are about to create a new Graphcool project. 


  ${chalk.bold('How do you want to continue?')}

`

/*
 * Terminal output: push
 */
export const noProjectFileForPushMessage = `\
Please provide a valid project file (${graphcoolProjectFileName}) for the schema migration.\n`

export const invalidProjectFileMessage = `\
The project file (${graphcoolProjectFileName}) that you provided doesn't seem to be valid. Please make sure it contains the ID of your project.\n`

export const pushingNewSchemaMessage = `\
Migrating the schema in your project...\
`

export const couldNotMigrateSchemaMessage = `
An error occured while trying to migrate the project.\n`

export const noActionRequiredMessage = `\
 ${chalk.green(figures.tick)}  Identical schema, no action required.\n`

export const migrationDryRunMessage = `\
This was a dry run of the migration. Here are the potential changes:\n`

export const migrationPerformedMessage = `\
 ${chalk.green(figures.tick)}  Your schema was successfully updated. Here are the changes:\n`

export const migrationErrorMessage = `\
There are issues with the new schema that you provided:\n`

export const invalidProjectFilePathMessage = (projectFilePath: string) => `\
${projectFilePath} is not a valid project file (must end with ${projectFileSuffix}).\n`

export const multipleProjectFilesMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you want to push by passing it as an argument.
For example: '$ graphcool push -p ${projectFiles[0]}'
`

/*
 * Terminal output: projects
 */
export const projectsMessage = `\
`

export const couldNotFetchProjectsMessage = `\
An error occured while trying to fetch your projects.\n`


/*
 * Terminal output: endpoints
 */

export const endpointsMessage = (projectId: string) => `\
 The endpoints for your project are are:
 
   ${chalk.blue(figures.pointer)} Simple API: https://api.graph.cool/simple/v1/${projectId}
   ${chalk.blue(figures.pointer)} Relay API: https://api.graph.cool/relay/v1/${projectId}\n`


/*
 * Terminal output: pull
 */

export const fetchingProjectDataMessage = `\
Fetching project data ...`

export const noProjectFileForPullMessage = `\
There is no project file (project.graphcool) in the current directory.\n`

export const noProjectIdMessage = `\
Please provide a valid project ID.\n`

export const wroteProjectFileMessage = `\
 ${chalk.green(figures.tick)}  Your project file was successfully updated. Reload it in your editor if needed. `

export const newVersionMessage = (newVersion: string) => `\
The new schema version is ${chalk.bold(newVersion)}.\n`

export const differentProjectIdWarningMessage = (inputProjectId: string, readProjectId: string) => `\
The project ID you provided (${inputProjectId}) is different than the one in the current project file (${readProjectId}). 
This will override the current project file with a different project, do you still want to continue? [y|n]\n`

export const multipleProjectFilesForPullMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you want for which you want to pull the new schema by passing it as an argument.
For example: '$ graphcool pull -p ${projectFiles[0]}\n'
`


/*
 * Terminal output: export
 */
export const exportingDataMessage = `\
Exporting your project data ...`

export const downloadUrlMessage = (url: string) => `\
 ${chalk.green(figures.tick)}  You can download your project data by pasting this URL in a browser:
 
  ${chalk.blue(figures.pointer)} Download URL: ${url}\n`

export const multipleProjectFilesForExportMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you for which you want to export the data by passing it as an argument.
For example: '$ graphcool export -p ${projectFiles[0]}'\n`



/*
 * Terminal output: general
 */
export const contactUsInSlackMessage = `\
Get in touch if you need help: http://slack.graph.cool/`

export const setDebugMessage = `\
You can enable additional output with \`export DEBUG=graphcool\` and rerunning the command.
Open an issue on GitHub: https://github.com/graphcool/graphcool-cli.`

export const noProjectFileOrIdMessage = `\
No project file or project ID provided.\n`

export const notAuthenticatedMessage = `\
You're currently not logged in. You can use the auth command to authenticate with Graphcool: '$ graphcool auth'`

export const projectFileWasUpdatedMessage = `\
Your project file was updated. Reload it in your editor if needed.\n`
