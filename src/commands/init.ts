import { Region, Resolver, SchemaInfo, SystemEnvironment } from '../types'
import figures = require('figures')
import generateName = require('sillyname')
import { createProject, parseErrors, generateErrorOutput } from '../api/api'
import * as fs from 'fs'
import * as path from 'path'
import * as fetch from 'isomorphic-fetch'
import { writeProjectFile } from '../utils/file'
import {
  graphcoolProjectFileName,
  creatingProjectMessage,
  createdProjectMessage,
  couldNotCreateProjectMessage,
  projectAlreadyExistsMessage
} from '../utils/constants'

const debug = require('debug')('graphcool')

interface Props {
  localSchemaFile?: string
  remoteSchemaUrl?: string
  name?: string
  alias?: string
  region?: Region
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  if (resolver.exists(graphcoolProjectFileName) && resolver.read(graphcoolProjectFileName).toString().includes('# projectId:')) {
    out.write(projectAlreadyExistsMessage)
    process.exit(1)
  }

  const name = props.name || generateName()
  debug(`Create project: ${name}`)

  out.startSpinner(creatingProjectMessage(name))

  try {

    // resolve schema
    const schemaUrl = props.localSchemaFile ? props.localSchemaFile : props.remoteSchemaUrl
    const schema = await getSchema(schemaUrl, resolver)

    // create project
    const projectInfo = await createProject(name, schema.schema, resolver, props.alias, props.region)
    debug(`Project info: ${JSON.stringify(projectInfo)}`)
    writeProjectFile(projectInfo, resolver)

    out.stopSpinner()

    const message = createdProjectMessage(name, schema.source, projectInfo.projectId)
    out.write(message)

  } catch (e) {
    out.stopSpinner()
    debug(`Could not create project: ${JSON.stringify(e)}`)
    out.write(`${couldNotCreateProjectMessage}`)

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.write(`${output}`)
    } else {
      throw e
    }
  }

}

async function getSchema(schemaUrl: string | undefined, resolver: Resolver): Promise<SchemaInfo> {

  debug(`Resolving schema: ${schemaUrl}`)

  if (schemaUrl) {
    if (schemaUrl.startsWith('http')) {
      const response = await fetch(schemaUrl)
      const schema = await response.text()
      return {
        schema,
        source: schemaUrl,
      }
    } else {
      return {
        schema: resolver.read(schemaUrl),
        source: schemaUrl,
      }
    }
  } else {
    const schemaFiles = fs.readdirSync('.').filter(f => f.endsWith('.schema'))
    if (schemaFiles.length === 0) {
      throw new Error('No .schema file found or specified')
    }

    const file = schemaFiles.find(f => f === 'graphcool.schema') || schemaFiles[0]
    debug(`Schema File: ${file}`)

    return {
      schema: fs.readFileSync(path.resolve(file)).toString(),
      source: schemaFiles[0],
    }
  }
}
