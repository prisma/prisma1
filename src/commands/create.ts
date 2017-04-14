import { Region } from '../types'
import figures = require('figures')
import generateName = require('sillyname')
import ora = require('ora')
import * as chalk from 'chalk'
import fetch from 'node-fetch'
import * as fs from 'fs'
import * as path from 'path'
// import { writeSchemaFile } from '../utils/file'

interface Props {
  schema?: string
  name?: string
  alias?: string
  region?: Region // TODO coming soon...
}

export default async(props: Props): Promise<void> => {
  if (fs.existsSync('graphcool.schema') && fs.readFileSync('graphcool.schema').toString().includes('# projectId: "')) {
    throw new Error(`graphcool.schema already exists with a projectId. Looks like you've already setup your backend.`)
  }

  const name = props.name || generateName()
  const aliasPart = props.alias ? `alias: "${props.alias}"` : ''

  const spinner = ora(`Creating project ${chalk.bold(name)}...`).start()

  // resolve schema
  const schema = await getSchema(props.schema)

  // create project
  const result = await api().query(`mutation addProject($schema: String) {
    addProject(input: {
      name: "${name}"
      ${aliasPart}
      schema: $schema
      clientMutationId: "static"
    }) {
      project {
        id
        schema
      }
    }
  }`, {schema: schema.schema})

  const projectId = result.addProject.project.id
  const resultSchema = result.addProject.project.schema

  writeSchemaFile(resultSchema, projectId)

  spinner.stop()

  const message = `${chalk.green(figures.tick)}  Created project ${chalk.bold(name)} from ${chalk.bold(schema.source)}. Your endpoints are:
 
  ${chalk.blue(figures.pointer)} Simple API: https://api.graph.cool/simple/v1/${projectId}
  ${chalk.blue(figures.pointer)} Relay API: https://api.graph.cool/relay/v1/${projectId}`

  console.log(message)
}

interface SchemaResult {
  schema: string
  source: string
}

async function getSchema(schemaProp: string | undefined): Promise<SchemaResult> {
  if (schemaProp) {
    if (schemaProp.startsWith('http')) {
      const response = await fetch(schemaProp)
      const schema = await response.text()
      return {
        schema,
        source: schemaProp,
      }
    } else {
      return {
        schema: fs.readFileSync(path.resolve(schemaProp)).toString(),
        source: schemaProp,
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
