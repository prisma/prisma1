import {SystemEnvironment, Resolver, MigrationMessage, Out, MigrationErrorMessage} from '../types'
import {
  readProjectIdFromProjectFile,
  isValidProjectFilePath,
  readDataModelFromProjectFile, readVersionFromProjectFile
} from '../utils/file'
import {
  noProjectIdMessage,
  noProjectFileOrIdMessage,
  invalidProjectFilePathMessage,
  multipleProjectFilesForStatusMessage, canNotReadVersionFromProjectFile, localSchemaBehindRemoteMessage,
  remoteSchemaBehindLocalMessage, everythingUpToDateMessage, potentialChangesMessage, issuesInSchemaMessage,
  noProjectFileMessage,
} from '../utils/constants'
import {
  parseErrors,
  generateErrorOutput,
  statusMessage
} from '../api/api'
const debug = require('debug')('graphcool')
import figures = require('figures')
import * as chalk from 'chalk'

interface Props {
  projectFile?: string
}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {

    const projectFile = getProjectFilePath(props, resolver)

    const projectId = readProjectIdFromProjectFile(resolver, projectFile)
    if (!projectId) {
      throw new Error(noProjectIdMessage)
    }

    const localVersion = readVersionFromProjectFile(resolver, projectFile)
    if (!localVersion || !parseInt(localVersion)) {
      throw new Error(canNotReadVersionFromProjectFile(projectFile))
    }

    const localSchema = readDataModelFromProjectFile(resolver, projectFile)
    const schemaWithFrontmatter = `# project: ${projectId}\n# version: ${localVersion}\n\n${localSchema}`
    const migrationResult = await statusMessage(schemaWithFrontmatter, resolver)

    if (parseInt(localVersion) < parseInt(migrationResult.newVersion)) {
      out.write(localSchemaBehindRemoteMessage(migrationResult.newVersion, localVersion))
      return
    } else if (parseInt(localVersion) > parseInt(migrationResult.newVersion)) {
      throw new Error(remoteSchemaBehindLocalMessage(migrationResult.newVersion, localVersion))
    }

    // no action required
    if (migrationResult.messages.length === 0 && migrationResult.errors.length === 0) {
      out.write(everythingUpToDateMessage)
      return
    }

    // changes to be displayed
    else if (migrationResult.messages.length > 0 && migrationResult.errors.length === 0) {
      const migrationMessage = potentialChangesMessage

      out.write(`${migrationMessage}`)
      printMigrationMessages(migrationResult.messages, 1, out)
    }

    // issues with local schema that prevent migration
    else if (migrationResult.messages.length === 0 && migrationResult.errors.length > 0) {
      out.write(`${issuesInSchemaMessage}`)
      printMigrationErrors(migrationResult.errors, out)
    }

  } catch(e) {
    out.stopSpinner()
    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }
  }

}

function getProjectFilePath(props: Props, resolver: Resolver): string {
  // check if provided file is valid (ends with correct suffix)
  if (props.projectFile && isValidProjectFilePath(props.projectFile)) {
    return props.projectFile
  } else if (props.projectFile && !isValidProjectFilePath(props.projectFile)) {
    throw new Error(invalidProjectFilePathMessage(props.projectFile))
  }

  // no project file provided, search for one in current dir
  const projectFiles = resolver.projectFiles('.')
  if (projectFiles.length === 0) {
    throw new Error(noProjectFileMessage)
  } else if (projectFiles.length > 1) {
    throw new Error(multipleProjectFilesForStatusMessage(projectFiles))
  }

  return projectFiles[0]
}

function printMigrationMessages(migrationMessages: MigrationMessage[], indentationLevel: number, out: Out) {
  migrationMessages.forEach(migrationMessage => {
    const indentation = spaces(indentationLevel * 4)
    out.write(`${indentation}${chalk.green(figures.play)} ${migrationMessage.description}\n`)

    if (migrationMessage.subDescriptions) {
      printMigrationMessages(migrationMessage.subDescriptions, indentationLevel + 1, out)
    }
  })
}

function printMigrationErrors(errors: [MigrationErrorMessage], out: Out) {
  const indentation = spaces(4)
  errors.forEach(error => {
    out.write(`${indentation}${chalk.red(figures.cross)} ${error.description}\n`)
  })
}

const spaces = (n: number) => Array(n + 1).join(' ')
