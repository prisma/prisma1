import {MigrationMessage, ProjectInfo, MigrationErrorMessage, SystemEnvironment, Out, Resolver} from '../types'
import { pushNewSchema, parseErrors, generateErrorOutput } from '../api/api'
import figures = require('figures')
import * as chalk from 'chalk'
import {
  graphcoolProjectFileName,
  noProjectFileMessage,
  couldNotMigrateSchemaMessage,
  pushingNewSchemaMessage, noActionRequiredMessage, migrationDryRunMessage, migrationPerformedMessage,
  migrationErrorMessage, invalidProjectFileMessage, invalidProjectFilePathMessage, multipleProjectFilesMessage
} from '../utils/constants'
import {
  writeProjectFile,
  readProjectInfoFromProjectFile, isValidProjectFilePath
} from '../utils/file'

const debug = require('debug')('graphcool')

interface Props {
  isDryRun: boolean
  projectFile?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env
  const projectFilePath = getProjectFilePath(props, env)

  const projectInfo = readProjectInfoFromProjectFile(resolver, projectFilePath)
  if (!projectInfo) {
    throw new Error(invalidProjectFileMessage)

  }

  const {projectId, schema, version} = projectInfo!
  const isDryRun = props.isDryRun

  out.startSpinner(pushingNewSchemaMessage)

  try {
    const schemaWithFrontmatter = `# project: ${projectId}\n# version: ${version}\n\n${schema}`
    const migrationResult = await pushNewSchema(schemaWithFrontmatter, isDryRun, resolver)

    out.stopSpinner()

    // no action required
    if (migrationResult.messages.length === 0 && migrationResult.errors.length === 0) {
      out.write(noActionRequiredMessage)
      return
    }

    // migration successful
    else if (migrationResult.messages.length > 0 && migrationResult.errors.length === 0) {
      const migrationMessage = isDryRun ? migrationDryRunMessage : migrationPerformedMessage

      out.write(`${migrationMessage}`)
      printMigrationMessages(migrationResult.messages, 1, out)

      // update project file if necessary
      if (!isDryRun) {
        const projectInfo = {
          projectId,
          schema,
          version: migrationResult.newVersion
        } as ProjectInfo
        writeProjectFile(projectInfo, resolver)
      }
    }

    // can't do migration because of issues with schema
    else if (migrationResult.messages.length === 0 && migrationResult.errors.length > 0) {
      out.write(`\n${migrationErrorMessage}`)
      printMigrationErrors(migrationResult.errors, out)
    }

  } catch (e) {
    out.stopSpinner()
    out.writeError(couldNotMigrateSchemaMessage)

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }
  }

}

function getProjectFilePath(props: Props, env: SystemEnvironment): string {
  const {resolver, out} = env

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
    throw new Error(multipleProjectFilesMessage(projectFiles))
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
