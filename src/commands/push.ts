import {Region, Resolver, SchemaInfo, MigrationMessage, ProjectInfo} from '../types'
import {
  graphcoolProjectFileName,
  noProjectFileMessage,
  couldNotMigrateSchemaMessage,
  pushingNewSchemaMessage, noActionRequiredMessage, migrationDryRunMessage, migrationPerformedMessage
} from '../utils/constants'
import * as fs from 'fs'
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
  // debug(`Execute 'push'\nProps: ${JSON.stringify(props)}`)
  if (!fs.existsSync(graphcoolProjectFileName) && !fs.existsSync(`${props.projectFilePath}/${graphcoolProjectFileName}`)) {
    process.stdout.write(noProjectFileMessage)
    process.exit(1)
  }

  const projectId = readProjectIdFromProjectFile(resolver, props.projectFilePath)
  const newSchema = readDataModelFromProjectFile(resolver, props.projectFilePath)
  const isDryRun = props.isDryRun || true

  // debug(`Push new schema: ${projectId} (dry: ${isDryRun})\n\n${newSchema}`)
  // TODO: check against remote schema to see if there are any changes

  const spinner = ora(pushingNewSchemaMessage).start()

  try {
    const migrationMessages = await pushNewSchema(projectId, newSchema, isDryRun, resolver)

    spinner.stop()

    if (migrationMessages.length === 0) {
      process.stdout.write(noActionRequiredMessage)
      process.exit(0)
    }

    const migrationMessage = isDryRun ? migrationDryRunMessage : migrationPerformedMessage
    process.stdout.write(migrationMessage)

    printMessages(migrationMessages, 0)

    // update project file if necessary
    if (!isDryRun) {
      const projectInfo = {
        projectId,
        schema: newSchema,
        version: '1.0'
      } as ProjectInfo
      writeProjectFile(projectInfo, resolver)
    }

  } catch(e) {
    debug(`Could not push new schema: ${e.message}`)
    process.stdout.write(couldNotMigrateSchemaMessage)
    process.exit(1)
  }

}

function printMessages(migrationMessages: [MigrationMessage], indentationLevel: number) {
  migrationMessages.forEach(migrationMessage => {
    const indentation = spaces(indentationLevel * 2)
    process.stdout.write(`${indentation}${figures.play} ${migrationMessage.description}\n`)
    // debug(`Sub Descriptions: ${migrationMessage.subDescriptions}`)
    if (migrationMessage.subDescriptions) {
      printMessages(migrationMessage.subDescriptions, indentationLevel + 1)
    }
  })
}

function spaces(n: number) {
  return Array(n+1).join(' ')
}