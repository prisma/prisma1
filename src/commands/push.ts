import {Resolver, MigrationMessage, ProjectInfo, MigrationErrorMessage} from '../types'
import {
  graphcoolProjectFileName,
  noProjectFileMessage,
  couldNotMigrateSchemaMessage,
  pushingNewSchemaMessage, noActionRequiredMessage, migrationDryRunMessage, migrationPerformedMessage,
  migrationErrorMessage
} from '../utils/constants'
import {readProjectIdFromProjectFile, readDataModelFromProjectFile, writeProjectFile} from '../utils/file'
import {pushNewSchema} from '../api/api'
import ora = require('ora')
import figures = require('figures')
const debug = require('debug')('graphcool')

interface Props {
  isDryRun?: boolean
  projectFilePath?: string
}

export default async(props: Props, resolver: Resolver): Promise<void> => {

  if (!resolver.exists(graphcoolProjectFileName) && !resolver.exists(`${props.projectFilePath}/${graphcoolProjectFileName}`)) {
    process.stdout.write(noProjectFileMessage)
    process.exit(1)
  }

  const projectId = readProjectIdFromProjectFile(resolver, props.projectFilePath)
  const newSchema = readDataModelFromProjectFile(resolver, props.projectFilePath)
  const version = '1'
  const isDryRun = props.isDryRun || true

  const spinner = ora(pushingNewSchemaMessage).start()

  try {

    const schemaWithFrontmatter = `# projectId: ${projectId}\n# version: ${version}\n\n${newSchema}`
    debug(`Schema with frontmatter: \n${schemaWithFrontmatter}`)

    const migrationResult = await pushNewSchema(schemaWithFrontmatter, isDryRun, resolver)
    spinner.stop()

    // no action required
    if (migrationResult.messages.length === 0 && migrationResult.errors.length === 0) {
      process.stdout.write(noActionRequiredMessage)
      process.exit(0)
    }

    // migration successful
    else if (migrationResult.messages.length > 0 && migrationResult.errors.length === 0) {

      const migrationMessage = isDryRun ? migrationDryRunMessage : migrationPerformedMessage

      process.stdout.write(`${migrationMessage}`)
      printMigrationMessages(migrationResult.messages, 0)

      // update project file if necessary
      if (!isDryRun) {
        const projectInfo = {
          projectId,
          schema: newSchema,
          version: '1.0'
        } as ProjectInfo
        writeProjectFile(projectInfo, resolver)
      }
    }

    // something went wrong
    else if (migrationResult.messages.length === 0 && migrationResult.errors.length > 0) {
      process.stdout.write(`\n\n${migrationErrorMessage}`)
      printMigrationErrors(migrationResult.errors)
    }

  } catch(e) {
    debug(`Could not push new schema: ${e.message}`)
    process.stdout.write(couldNotMigrateSchemaMessage)
    process.exit(1)
  }

}

function printMigrationMessages(migrationMessages: [MigrationMessage], indentationLevel: number) {
  migrationMessages.forEach(migrationMessage => {
    const indentation = spaces(indentationLevel * 2)
    process.stdout.write(`${indentation}${figures.play} ${migrationMessage.description}\n`)
    if (migrationMessage.subDescriptions) {
      printMigrationMessages(migrationMessage.subDescriptions, indentationLevel + 1)
    }
  })
}

function printMigrationErrors(errors: [MigrationErrorMessage]) {
  errors.forEach(error => {
    process.stdout.write(`${figures.cross} ${error.description}\n`)
  })
}

function spaces(n: number) {
  return Array(n+1).join(' ')
}