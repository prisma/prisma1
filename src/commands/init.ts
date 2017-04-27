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

  if (resolver.exists(graphcoolProjectFileName) && resolver.read(graphcoolProjectFileName).toString().includes('# projectId:')) {
    out.write(projectAlreadyExistsMessage)
    process.exit(1)
  }

  const name = props.name || generateName()
  debug(`Create project: ${name}`)

  out.startSpinner(creatingProjectMessage(name))

  try {

    // resolve schema
    const schema = await getSchema(props.schemaUrl, resolver)
    debug(`Schema: ${JSON.stringify(schema)}`)

    // create project
    const projectInfo = await createProject(name, schema.schema, resolver, props.alias, props.region)
    debug(`Project info: ${JSON.stringify(projectInfo)}`)
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
    debug(`Schema File: ${file}`)

    return {
      schema: fs.readFileSync(path.resolve(file)).toString(),
      source: schemaFiles[0],
    }
  }
}
