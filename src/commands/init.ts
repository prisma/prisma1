import { Region, Resolver, SchemaInfo, SystemEnvironment } from '../types'
import figures = require('figures')
import generateName = require('sillyname')
import { createProject, parseErrors, generateErrorOutput } from '../api/api'
import * as fs from 'fs'
import * as path from 'path'
import { writeProjectFile } from '../utils/file'
import 'isomorphic-fetch'
import {
  graphcoolProjectFileName,
  creatingProjectMessage,
  createdProjectMessage,
  couldNotCreateProjectMessage,
  projectAlreadyExistsMessage, projectFileSuffix
} from '../utils/constants'
const debug = require('debug')('graphcool')

interface Props {
  localSchemaFile?: string
  remoteSchemaUrl?: string
  name?: string
  alias?: string
  region?: Region
  outputPath?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  if (resolver.exists(graphcoolProjectFileName) && resolver.read(graphcoolProjectFileName).toString().includes('# project:')) {
    throw new Error(projectAlreadyExistsMessage)
  }

  const name = props.name || generateName()

  out.startSpinner(creatingProjectMessage(name))

  try {

    // resolve schema
    const schemaUrl = props.localSchemaFile ? props.localSchemaFile : props.remoteSchemaUrl
    const schema = await getSchema(schemaUrl, resolver)

    // create project
    const projectInfo = await createProject(name, schema.schema, resolver, props.alias, props.region)
    writeProjectFile(projectInfo, resolver, props.outputPath)

    out.stopSpinner()

    const message = createdProjectMessage(name, schema.source, projectInfo.projectId)
    out.write(message)

  } catch (e) {
    out.stopSpinner()
    out.writeError(`${couldNotCreateProjectMessage}`)

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }
  }

}

async function getSchema(schemaUrl: string | undefined, resolver: Resolver): Promise<SchemaInfo> {
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
    const schemaFiles = resolver.readDirectory('.').filter(f => f.endsWith(projectFileSuffix))
    if (schemaFiles.length === 0) {
      throw new Error(`No ${projectFileSuffix} file found or specified`)
    }

    const file = schemaFiles.find(f => f === graphcoolProjectFileName) || schemaFiles[0]
    return {
      schema: fs.readFileSync(path.resolve(file)).toString(),
      source: schemaFiles[0],
    }
  }
}

