import {SystemEnvironment,
  Resolver,
  MigrationMessage,
  Out,
  MigrationErrorMessage,
  MigrationActionType} from '../types'
import {
  readProjectIdFromProjectFile,
  isValidProjectFilePath,
  readDataModelFromProjectFile,
  readVersionFromProjectFile
} from '../utils/file'
import {
  noProjectIdMessage,
  invalidProjectFilePathMessage,
  multipleProjectFilesForStatusMessage,
  canNotReadVersionFromProjectFile,
  localSchemaBehindRemoteMessage,
  remoteSchemaBehindLocalMessage,
  everythingUpToDateMessage,
  potentialChangesMessage,
  issuesInSchemaMessage,
  noProjectFileMessage,
  destructiveChangesInStatusMessage,
  usePushToUpdateMessage,
  statusHeaderMessage,
} from '../utils/constants'
import {
  parseErrors,
  generateErrorOutput,
  statusMessage
} from '../api/api'
const debug = require('debug')('graphcool')
import figures = require('figures')
import * as chalk from 'chalk'
import {makePartsEnclodesByCharacterBold} from '../utils/utils'

export interface StatusProps {
  projectFile?: string
}

export default async(props: StatusProps, env: SystemEnvironment): Promise<void> => {
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

    out.write(statusHeaderMessage(projectId, migrationResult.newVersion))

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
      printMigrationMessages(migrationResult.messages, out)
      out.write(usePushToUpdateMessage)
    }

    // issues with local schema that prevent migration
    else if (migrationResult.messages.length === 0 && migrationResult.errors.length > 0) {

      const migrationMessage = potentialChangesMessage
      out.write(`${issuesInSchemaMessage}`)
      printMigrationErrors(migrationResult.errors, out)
    }

    // potentially destructive changes
    else if (migrationResult.errors[0].description.indexOf(`destructive changes`) >= 0) {
      const migrationMessage = potentialChangesMessage

      out.write(`${migrationMessage}`)
      printMigrationMessages(migrationResult.messages, out)
      out.write(destructiveChangesInStatusMessage)
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

function getProjectFilePath(props: StatusProps, resolver: Resolver): string {
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

function printMigrationMessages(migrationMessages: MigrationMessage[], out: Out) {
  migrationMessages.forEach((migrationMessage, index) => {
    const actionType = getMigrationActionType(migrationMessage.description)
    const symbol = getSymbolForMigrationActionType(actionType)
    const description = makePartsEnclodesByCharacterBold(migrationMessage.description, `\``)
    const outputMessage = `${index > 0 ? `  |` : ``}\n  | (${symbol})  ${description}\n`
    out.write(outputMessage)
    migrationMessage.subDescriptions!.forEach(subMessage => {
      const actionType = getMigrationActionType(subMessage.description)
      const symbol = getSymbolForMigrationActionType(actionType)
      const outputMessage = makePartsEnclodesByCharacterBold(subMessage.description, `\``)
      out.write(`  ├── (${symbol})  ${outputMessage}\n`)
    })
  })
}

function getMigrationActionType(message: string): MigrationActionType {
  if (message.indexOf('create') >= 0) {
    return 'create'
  } else if (message.indexOf('update') >= 0) {
    return 'update'
  } else if (message.indexOf('delete') >= 0 || message.indexOf('remove') >= 0) {
    return 'delete'
  }
  return 'unknown'
}

function getSymbolForMigrationActionType(type: MigrationActionType): string {
  switch (type) {
    case 'create': return '+'
    case 'delete': return '-'
    case 'update': return '*'
    case 'unknown': return '?'
  }
}

function printMigrationErrors(errors: [MigrationErrorMessage], out: Out) {
  const indentation = spaces(2)
  errors.forEach(error => {
    const outputMessage = makePartsEnclodesByCharacterBold(error.description, `\``)
    out.write(`${indentation}${chalk.red(figures.cross)} ${outputMessage}\n`)
  })
}

const spaces = (n: number) => Array(n + 1).join(' ')
