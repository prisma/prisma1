import * as os from 'os'
import * as path from 'path'
import * as chalk from 'chalk'
import figures = require('figures')

/*
 * Networking & URLs
 */
export const authUIEndpoint = process.env.ENV === 'DEV' ? 'https://dev.console.graph.cool/cli/auth' : 'https://console.graph.cool/cli/auth'
export const systemAPIEndpoint = process.env.ENV === 'DEV' ? 'https://dev.api.graph.cool/system' : 'https://api.graph.cool/system'
export const authEndpoint = process.env.ENV === 'DEV' ? 'https://cli-auth-api.graph.cool/dev' : 'https://cli-auth-api.graph.cool/prod'
export const docsEndpoint = process.env.ENV === 'DEV' ? 'https://dev.graph.cool/docs' : 'https://www.graph.cool/docs'
export const statusEndpoint = 'https://crm.graph.cool/prod/status'
export const consoleURL = (token: string, projectName?: string) =>
  `https://console.graph.cool/token?token=${token}${projectName ? `&redirect=/${encodeURIComponent(projectName)}` : ''}`
export const playgroundURL = (token: string, projectName: string) =>
  `https://console.graph.cool/token?token=${token}&redirect=/${encodeURIComponent(projectName)}/playground`
export const sampleSchemaURL = `http://graphqlbin.com/empty.graphql`

/*
 * Sentry
 */
export const sentryDSN = 'https://6ef6eea3afb041f2aca71d08742a36d1:51bdc5643a7648ffbfb3d3017879467c@sentry.io/178603'

/*
 * File paths / names
 */
export const graphcoolProjectFileName = 'project.graphcool'
export const graphcoolCloneProjectFileName = (projectFileName?: string) => projectFileName ?
  `clone-${projectFileName.startsWith(`./`) ? projectFileName.substring(2) : projectFileName}`: `clone-${graphcoolProjectFileName}`
export const graphcoolConfigFilePath = path.join(os.homedir(), '.graphcoolrc')
export const projectFileSuffix = '.graphcool'
export const schemaFileSuffix = '.graphql'
export const exampleSchema = `\
# \`User\` is a system type with special characteristics.
# You can read about it in the documentation:
# https://www.graph.cool/docs/reference/schema/system-artifacts-uhieg2shio/#user-type
type User {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
}

# \`File\` is a system type with special characteristics.
# You can read about it in the documentation:
# https://www.graph.cool/docs/reference/schema/system-artifacts-uhieg2shio/#file-type
type File {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  contentType: String!
  name: String!
  secret: String! @isUnique
  size: Int!
  url: String! @isUnique
}

# You can easily add custom types, here is an example.
# If you want to add the \`Tweet\` type to your schema,
# remove the below comments and run \`graphcool push\`.
# type Tweet {
#   text: String!
# }`

export const blankProjectFileFromExampleSchema = (projectId: string, version: string) => `\
# project: ${projectId}
# version: ${version}


###########################################################################
# \`User\` and \`File\` are generated and have special characteristics.
# You can read about them in the documentation:
# https://www.graph.cool/docs/reference/schema/system-artifacts-uhieg2shio/
###########################################################################

type User {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
}

type File {
  id: ID! @isUnique
  createdAt: DateTime!
  updatedAt: DateTime!
  contentType: String!
  name: String!
  secret: String! @isUnique
  size: Int!
  url: String! @isUnique
}


###########################################################################
# You can easily add custom definitions, \`Tweet\` below is an example.
# If you want to add \`Tweet\` to your schema,
# remove the below comments and run \`graphcool push\`.
###########################################################################

# type Tweet {
#   text: String!
# }`


/*
 * Terminal output: auth
 */
export const openBrowserMessage = `You need to authenticate. Your browser will open shortly...`
export const couldNotRetrieveTokenMessage = `Whoops, something went wrong during authentication.
`

export const authenticationSuccessMessage = (email: string) => ` ${chalk.green(figures.tick)} Authenticated user: ${chalk.bold(email)}
`

/*
 * Terminal output: create
 */
export const creatingProjectMessage = (name: string) => `Creating project ${chalk.bold(name)}...`
export const createdProjectMessage = (name: string, projectId: string, projectFileContent: string, projectOutputPath?: string) => `\
 ${chalk.green(figures.tick)} Created project ${chalk.bold(name)} (ID: ${projectId}) successfully.


   ${chalk.bold('Here is what you can do next:')}

   1) Open ${chalk.bold(projectOutputPath || 'project.graphcool')} in your editor to update your schema.
      You can push your changes using ${chalk.cyan('`graphcool push`')}.

   2) Use one of the following endpoints to connect to your GraphQL API:

        Simple API:   https://api.graph.cool/simple/v1/${projectId}
        Relay API:    https://api.graph.cool/relay/v1/${projectId}

   3) Edit your project using the Console. Run ${chalk.cyan('`graphcool console`')} to open it.
`

export const couldNotCreateProjectMessage = `\
Whoops, something went wrong while creating the project.

`

// export const projectAlreadyExistsMessage = `\
// ${graphcoolProjectFileName} already exists for the current project. \
// Looks like you've already setup your backend.
// `

export const projectAlreadyExistsMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project ${projectFiles.length === 1 ? 'file' : 'files'} in the current directory. Looks like you've already setup your backend.
You can still use graphcool init --output file.graphcool to specify a custom output file.
`

export const cantCopyAcrossRegions = `\
A project can't be copied across regions. Please specify the --copy parameter without --region.
`

export const invalidProjectNameMessage = (projectName: string) => `\
'${projectName}' is not a valid project name. It must begin with an uppercase letter.
`

export const howDoYouWantToGetStarted = () => `\

  You are about to create a new Graphcool project.


  ${chalk.bold('How do you want to continue?')}

`

export const invalidSchemaFileMessage = (schemaFileName: string) =>  `\
Your schema file ${chalk.bold(schemaFileName)} is invalid. A schema file must end with ${chalk.bold(schemaFileSuffix)}.
`

/*
 * Terminal output: push
 */

export const remoteSchemaAheadMessage = (remoteVersion: string, localVersion: string) => `\
The local version of your schema (${localVersion}) is behind the remote version (${remoteVersion}). Save your local changes and run ${chalk.cyan(`\`graphcool pull\``)} before retrying.`

export const noProjectFileForPushMessage = `\
Please provide a valid project file (${graphcoolProjectFileName}) for the schema migration.
`

export const invalidProjectFileMessage = `\
The project file (${graphcoolProjectFileName}) that you provided doesn't seem to be valid. Please make sure it contains the ID of your project.
`

export const pushingNewSchemaMessage = `\
Migrating the schema in your project...`


export const noActionRequiredMessage = `\
 ${chalk.green(figures.tick)} Identical schema, no action required.
`

export const migrationPerformedMessage = `\
 ${chalk.green(figures.tick)} Your schema was successfully updated. Here are the changes:
`

export const migrationErrorMessage = `\
There are issues with the new schema:

`

export const potentialDataLossMessage = `\
Your changes might result in data loss.
Review your changes with ${chalk.cyan(`\`graphcool status\``)} or use ${chalk.cyan(`\`graphcool push --force\``)} if you know what you're doing!
`

export const invalidProjectFilePathMessage = (projectFilePath: string) => `\
${projectFilePath} is not a valid project file (must end with ${projectFileSuffix}).
`

export const multipleProjectFilesMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you want to push, e.g.: ${chalk.cyan(`\`graphcool push ${projectFiles[0]}\``)}`

/*
 * Terminal output: projects
 */

export const couldNotFetchProjectsMessage = `\
An error occured while trying to fetch your projects.
`


/*
 * Terminal output: endpoints
 */

export const endpointsMessage = (projectId: string) => `\
 The endpoints for your project are:

   Simple API:         https://api.graph.cool/simple/v1/${projectId}
   Relay API:          https://api.graph.cool/relay/v1/${projectId}
   Subscriptions API:  wss://subscriptions.graph.cool/v1/${projectId}
   File API:           https://api.graph.cool/file/v1/${projectId}
`

export const multipleProjectFilesForEndpointsMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you for which you want to display the endpoints, e.g.: ${chalk.cyan(`\`graphcool endpoints ${projectFiles[0]}\``)}`




/*
 * Terminal output: pull
 */

export const fetchingProjectDataMessage = `\
Fetching project data ...`

export const noProjectIdMessage = `\
Please provide a valid project ID.
`

export const wroteProjectFileMessage = (projectFile: string) => `\
 ${chalk.green(figures.tick)} Your project file (${chalk.bold(projectFile)}) was successfully updated. Reload it in your editor if needed.
`

export const pulledInitialProjectFileMessage = (projectFile: string) => `\
 ${chalk.green(figures.tick)} Your project file was written to ${chalk.bold(projectFile)}
`

export const newVersionMessage = (newVersion: string) => `\
The new schema version is ${chalk.bold(newVersion)}.
`

export const differentProjectIdWarningMessage = (inputProjectId: string, readProjectId: string) => `\
The project ID you provided (${inputProjectId}) is different than the one in the current project file (${readProjectId}).
This will override the current project file with a different project, do you still want to continue? [y|N]
`

export const warnOverrideProjectFileMessage = (projectFile: string) => `\
You are about to override the local project file ${chalk.bold(projectFile)}. Make sure to save local changes that you want to preserve.
Do you want to continue? [y|N]
`

export const multipleProjectFilesForPullMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you want for which you want to pull the new schema, e.g.: ${chalk.cyan(`\`graphcool pull ${projectFiles[0]}\``)}
`


/*
 * Terminal output: export
 */
export const exportingDataMessage = `\
Exporting your project data ...`

export const downloadUrlMessage = (url: string) => `\
 ${chalk.green(figures.tick)} You can download your project data by pasting this URL in a browser:

   ${chalk.blue(figures.pointer)} Download URL: ${url}
`

export const multipleProjectFilesForExportMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you for which you want to export the data, e.g.: ${chalk.cyan(`\`graphcool export ${projectFiles[0]}\``)}`


/*
 * Terminal output: console
 */
export const openedConsoleMessage = (projectName?: string) => projectName ? `\
The Console for project ${chalk.bold(projectName)} was opened in your browser.
` : `\
The Console was opened in your browser.
`



/*
 * Terminal output: clone
 */
export const multipleProjectFilesForCloneMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one for which you want to clone the project, e.g.: ${chalk.cyan(`\`graphcool clone ${projectFiles[0]}\``)}
`

export const cloningProjectMessage = `\
Cloning your project ...`

export const clonedProjectMessage = (clonedProjectName: string, outputPath: string, projectId: string) => `\
 ${chalk.green(figures.tick)} Cloned your project as ${chalk.bold(clonedProjectName)}. The project file was written to ${chalk.bold(outputPath)}.

   Here are your endpoints:

        Simple API:   https://api.graph.cool/simple/v1/${projectId}
        Relay API:    https://api.graph.cool/relay/v1/${projectId}
`


/*
 * Terminal output: playground
 */
export const tooManyProjectFilesForPlaygroundMessage = (projectFiles: string[]) => `\
Found ${projectFiles.length} project files. You can specify the one you for which you want open the Playground, e.g.: ${chalk.cyan(`\`graphcool playground ${projectFiles[0]}\``)}
`

export const openedPlaygroundMessage = (projectName: string) => `\
The Playground for project ${chalk.bold(projectName)} was opened in your browser.
`

/*
 * Terminal output: quickstart
 */
export const openedQuickstartMessage = `\
The Quickstart tutorial was opened in your browser.
`

/*
 * Terminal output: status
 */
export const multipleProjectFilesForStatusMessage = (projectFiles: string[]) =>  `\
Found ${projectFiles.length} project files. You can specify the one you for which you want display the status, e.g.: ${chalk.cyan(`\`graphcool status ${projectFiles[0]}\``)}
`

export const localSchemaBehindRemoteMessage = (remoteVersion: string, localVersion: string) => `\
The local version of your schema (${localVersion}) is behind the remote version (${remoteVersion}).
Save your local changes and execute ${chalk.cyan(`\`graphcool pull\``)} before a schema migration.
`

export const remoteSchemaBehindLocalMessage = (remoteVersion: string, localVersion: string) => `\
The remote version (${remoteVersion}) is behind the local version (${localVersion}). Please don't make manual changes to the project file's header.
`

export const statusHeaderMessage = (projectId: string, version: string) => `\
Project ID: ${projectId}
Remote Schema Version: ${version}

`

export const everythingUpToDateMessage = `\
Everything up-to-date.
`

export const potentialChangesMessage = `\
Here are all the local changes:
`

export const usePushToUpdateMessage = `
Use ${chalk.cyan(`\`graphcool push\``)} to apply your schema changes.
`

export const issuesInSchemaMessage = `\
The current version of your schema contains some issues:

`

export const destructiveChangesInStatusMessage = `\

Pushing the current version of your schema can result in data loss.
Use ${chalk.cyan(`\`graphcool push --force\``)} if you know what you're doing!
`

/*
 * Terminal output: delete
 */

export const deletingProjectMessage = (projectId?: string) => projectId ? `\
Deleting project with Id ${projectId} ...` : `\
Deleting projects ...`

export const deletingProjectsMessage = (projectIds: string[]) => `\
Deleting ${projectIds.length} projects ...`

export const deletedProjectMessage = (projectIds: string[]) => projectIds.length > 1 ?  `\
${chalk.green(figures.tick)} Successfully deleted ${projectIds.length} projects.` : `\
${chalk.green(figures.tick)} Your project was successfully deleted.`

export const deletingProjectWarningMessage = `\
Are you absolutely sure you want to delete these projects? ${chalk.bold(`This operation can not be undone!`)} [y|N]`

/*
 * Terminal output: general
 */

export const contactUsInSlackMessage = `\
 * Get in touch if you need help: http://slack.graph.cool/`

export const setDebugMessage = `\
 * You can enable additional output by setting \`export DEBUG=graphcool\` and rerunning the command.
 * Open an issue on GitHub: https://github.com/graphcool/graphcool-cli.`

export const noProjectFileOrIdMessage = `\
No project file or project ID provided.
`

export const noProjectFileMessage = `\
No project file found.
`

export const notAuthenticatedMessage = `\
You're currently not logged in. You can use the auth command to authenticate with ${chalk.cyan(`\`graphcool auth\``)}
`

export const projectFileWasUpdatedMessage = (projectFile: string) => `\
Your project file ${chalk.bold(projectFile)} was updated. Reload it in your editor if needed.
`

export const canNotReadVersionFromProjectFile = (projectFile: string) => `\
No schema version specified in ${chalk.bold(projectFile)}.
`

export const canNotReadProjectIdFromProjectFile = `\
Could not read the project's ID from project file.
`

export const unknownOptionsWarning = (command: string, unknownOptions: string[]) => unknownOptions.length > 1 ? `\
${chalk.bold('Error:')} The following options are not recognized: ${chalk.red(`${unknownOptions.map(a => a)}`)}
Use ${chalk.cyan(`\`graphcool ${command} --help\``)} to see a list of all possible options.
` : `\
${chalk.bold('Error:')} The following option is not recognized: ${chalk.red(`${unknownOptions[0]}`)}
Use ${chalk.cyan(`\`graphcool ${command} --help\``)} to see a list of all possible options.
`
