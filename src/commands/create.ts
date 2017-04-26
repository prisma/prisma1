import {Region, Resolver, SchemaInfo, SystemEnvironment} from '../types'
import figures = require('figures')
import generateName = require('sillyname')
import { createProject } from '../api/api'
import * as fs from 'fs'
import * as path from 'path'
import {
  graphcoolProjectFileName,
  creatingProjectMessage,
  createdProjectMessage,
  couldNotCreateProjectMessage,
  projectAlreadyExistsMessage
} from '../utils/constants'
import {writeProjectFile} from '../utils/file'
const debug = require('debug')('graphcool')

interface Props {
  schemaUrl?: string
  name?: string
  alias?: string
  region?: Region // TODO coming soon...
}

export default async(props: Props, env: SystemEnvironment): Promise<void> => {

  const {resolver, out} = env

  if (resolver.exists(graphcoolProjectFileName) && resolver.read(graphcoolProjectFileName).toString().includes('@ project "')) {
    out.write(projectAlreadyExistsMessage)
    process.exit(1)
  }

  const name = props.name || generateName()
  const aliasPart = props.alias ? `alias: "${props.alias}"` : ''

  out.startSpinner(creatingProjectMessage(name))

  try {

    // resolve schema
    const schema = await getSchema(props.schemaUrl, resolver)

    // create project
    const projectInfo = await createProject(name, aliasPart, schema.schema, resolver)
    writeProjectFile(projectInfo, resolver)

    out.stopSpinner()

    const message = createdProjectMessage(name, schema.source, projectInfo.projectId)
    out.write(message)

  } catch(e) {
    debug(`Could not create project: ${e.message}`)
    out.write(couldNotCreateProjectMessage)
    process.exit(1)
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
    const schemaFiles = fs.readdirSync('.').filter(f => f.endsWith('.schema'))
    if (schemaFiles.length === 0) {
      throw new Error('No .schema file found or specified')
    }

    const file = schemaFiles.find(f => f === 'graphcool.schema') || schemaFiles[0]

    return {
      schema: fs.readFileSync(path.resolve(file)).toString(),
      source: schemaFiles[0],
    }
  }
}
