import { MigrationMessage, ProjectInfo, MigrationErrorMessage, SystemEnvironment, Out } from '../types'
import { pushNewSchema, parseErrors, generateErrorOutput } from '../api/api'
import figures = require('figures')
import * as chalk from 'chalk'
import {
  graphcoolProjectFileName,
  noProjectFileMessage,
  couldNotMigrateSchemaMessage,
  pushingNewSchemaMessage, noActionRequiredMessage, migrationDryRunMessage, migrationPerformedMessage,
  migrationErrorMessage, invalidProjectFileMessage
} from '../utils/constants'
import {
  writeProjectFile,
  readProjectInfoFromProjectFile
} from '../utils/file'

const debug = require('debug')('graphcool')

interface Props {
  isDryRun: boolean
  projectFilePath: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  if (!resolver.exists(graphcoolProjectFileName) && !resolver.exists(`${props.projectFilePath}/${graphcoolProjectFileName}`)) {
    out.writeError(noProjectFileMessage)
    process.exit(1)
  }

  const projectInfo = readProjectInfoFromProjectFile(resolver, props.projectFilePath)
  if (!projectInfo) {
    out.writeError(invalidProjectFileMessage)
    process.exit(1)
  }

  const {projectId, schema, version} = projectInfo!
  const isDryRun = props.isDryRun

  out.startSpinner(pushingNewSchemaMessage)

  try {
    const schemaWithFrontmatter = `# projectId: ${projectId}\n# version: ${version}\n\n${schema}`
    const migrationResult = await pushNewSchema(schemaWithFrontmatter, isDryRun, resolver)

    out.stopSpinner()

    // no action required
    if (migrationResult.messages.length === 0 && migrationResult.errors.length === 0) {
      out.write(noActionRequiredMessage)
      process.exit(0)
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
