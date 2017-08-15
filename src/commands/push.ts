import {
  MigrationMessage,
  ProjectInfo,
  MigrationErrorMessage,
  SystemEnvironment,
  Out,
  MigrationActionType,
  Resolver
} from '../types'
import {pushNewSchema,
  parseErrors,
  generateErrorOutput,
  pullProjectInfo} from '../api/api'
import figures = require('figures')
import * as chalk from 'chalk'
import {
  noProjectFileForPushMessage,
  pushingNewSchemaMessage,
  noActionRequiredMessage,
  migrationPerformedMessage,
  migrationErrorMessage,
  invalidProjectFileMessage,
  invalidProjectFilePathMessage,
  multipleProjectFilesMessage,
  projectFileWasUpdatedMessage,
  remoteSchemaAheadMessage,
  potentialDataLossMessage
} from '../utils/constants'
import {
  writeProjectFile,
  readProjectInfoFromProjectFile,
  isValidProjectFilePath
} from '../utils/file'
import {makePartsEnclodesByCharacterBold} from '../utils/utils'

const debug = require('debug')('graphcool')

export interface PushProps {
  force: boolean
  projectFile?: string
}

export default async (props: PushProps, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env
  const projectFilePath = getProjectFilePath(props, resolver)

  const projectInfo = readProjectInfoFromProjectFile(resolver, projectFilePath)
  if (!projectInfo) {
    throw new Error(invalidProjectFileMessage)
  }

  const {projectId, schema, version} = projectInfo!
  const force = props.force

  out.startSpinner(pushingNewSchemaMessage)

  try {

    // first compare local and remote versions and fail if remote is ahead
    const remoteProjectInfo = await pullProjectInfo(projectInfo.projectId, resolver)
    if (parseInt(projectInfo.version) > parseInt(remoteProjectInfo.version)) {
      out.stopSpinner()
      throw new Error(remoteSchemaAheadMessage(remoteProjectInfo.version, projectInfo.version))
    }

    const schemaWithFrontmatter = `# project: ${projectId}\n# version: ${version}\n\n${schema}`
    const migrationResult = await pushNewSchema(schemaWithFrontmatter, force, resolver)

    out.stopSpinner()

    // no action required
    if (migrationResult.messages.length === 0 && migrationResult.errors.length === 0) {
      out.write(noActionRequiredMessage)
      return
    }

    // migration successful
    else if (migrationResult.messages.length > 0 && migrationResult.errors.length === 0) {
      const migrationMessage = migrationPerformedMessage

      out.write(`${migrationMessage}`)
      printMigrationMessages(migrationResult.messages, out)
      out.write(`\n`)

      // update project file if necessary
      const projectInfo = {
        projectId,
        schema: migrationResult.newSchema,
        version: migrationResult.newVersion
      } as ProjectInfo
      writeProjectFile(projectInfo, resolver, projectFilePath)
      out.write(projectFileWasUpdatedMessage(projectFilePath))
    }

    // can't do migration because of issues with schema
    else if (migrationResult.messages.length === 0 && migrationResult.errors.length > 0) {
      out.write(`${migrationErrorMessage}`)
      printMigrationErrors(migrationResult.errors, out)
      out.write(`\n`)
    }

    // potentially destructive changes
    else if (migrationResult.errors[0].description.indexOf(`destructive changes`) >= 0) {
      out.write(potentialDataLossMessage)
    }

  } catch (e) {
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

function getProjectFilePath(props: PushProps, resolver: Resolver): string {

  // check if provided file is valid (ends with correct suffix)
  if (props.projectFile && isValidProjectFilePath(props.projectFile)) {
    return props.projectFile
  } else if (props.projectFile && !isValidProjectFilePath(props.projectFile)) {
    throw new Error(invalidProjectFilePathMessage(props.projectFile))
  }

  // no project file provided, search for one in current dir
  const projectFiles = resolver.projectFiles('.')
  if (projectFiles.length === 0) {
    throw new Error(noProjectFileForPushMessage)
  } else if (projectFiles.length > 1) {
    throw new Error(multipleProjectFilesMessage(projectFiles))
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
