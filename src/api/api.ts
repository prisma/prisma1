import {Resolver, ProjectInfo, MigrationMessage, MigrationErrorMessage, MigrationResult, APIError} from '../types'
import {graphcoolConfigFilePath, systemAPIEndpoint, contactUsInSlackMessage} from '../utils/constants'
import * as fs from 'fs'
import * as http from 'http'

const debug = require('debug')('graphcool')
import 'isomorphic-fetch'

async function sendGraphQLRequest(
  queryString: string,
  resolver: Resolver,
  variables?: any
): Promise<any> {

  const configContents = resolver.read(graphcoolConfigFilePath)
  const {token} = JSON.parse(configContents)

  const queryVariables = variables || {}
  const payload = {
    query: queryString,
    variables: queryVariables
  }

  const result = await fetch(systemAPIEndpoint, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
  })
  return result
}

export async function createProject(
  name: string,
  schema: string,
  resolver: Resolver,
  alias?: string,
  region?: string
): Promise<ProjectInfo> {

  const mutation = `\
mutation addProject($schema: String!, $name: String!, $alias: String, $region: Region) {
    addProject(input: {
      name: $name,
      schema: $schema,
      alias: $alias,
      region: $region,
      clientMutationId: "static"
    }) {
      project {
        id
        schema
      }
    }
  }
`

  let variables = { name, schema }
  if (alias) {
    variables = {...variables, alias}
  }
  if (region) {
    variables = {...variables, region}
  }

  debug(`Send variables: ${JSON.stringify(variables)}\n`)

  const result = await sendGraphQLRequest(mutation, resolver, variables)
  const json = await result.json()

  debug(`Received JSON: ${JSON.stringify(json)}\n`)

  if (!json.data.addProject) {
    throw json
  }

  const projectId = json.data.addProject.project.id
  const version = '1' // result.addProject.version
  const fullSchema = json.data.addProject.project.schema
  const projectInfo = {projectId, version, schema: fullSchema}

  return projectInfo
}

export async function pushNewSchema(
  newSchema: string,
  isDryRun: boolean,
  resolver: Resolver
): Promise<MigrationResult> {

  const mutation = `\
 mutation($newSchema: String!, $isDryRun: Boolean!) {
  migrateProject(input: {
    newSchema: $newSchema,
    isDryRun: $isDryRun
  }) {
    migrationMessages {
      type
      action
      name
      description
      subDescriptions {
        type
        action
        name
        description
      }
    }
    errors {
      description
      type
      field
    }
    project {
      version
    }
  }
}
`

  const variables = {
    newSchema,
    isDryRun
  }

  const result = await sendGraphQLRequest(mutation, resolver, variables)
  const json = await result.json()

  debug(`Received json for 'push': ${JSON.stringify(json)}`)

  if (!json.data.migrateProject) {
    throw json
  }

  const messages = json.data.migrateProject.migrationMessages as [MigrationMessage]
  const errors = json.data.migrateProject.errors as [MigrationErrorMessage]
  const newVersion = json.data.migrateProject.project.version
  const migrationResult = { messages, errors, newVersion } as MigrationResult

  return migrationResult
}

export async function fetchProjects(resolver: Resolver): Promise<[ProjectInfo]> {

  const query = `\
{
  viewer {
	  user {
      projects {
        edges {
          node {
            id
            name
          }
        }
      }
    }
  }
}
`

  const result = await sendGraphQLRequest(query, resolver)
  const json = await result.json()

  debug(`Received data: ${JSON.stringify(json)}\n`)

  const projects = json.data.viewer.user.projects.edges.map(edge => edge.node)
  const projectInfos = projects.map(project => ({projectId: project.id, name: project.name})) as [ProjectInfo]

  return projectInfos
}

export async function pullProjectInfo(projectId: string, resolver: Resolver): Promise<ProjectInfo> {

  const query = `\
query ($projectId: ID!){
  viewer {
    project(id: $projectId) {
      id
      name
      schema
    }
  }
}`

  const variables = { projectId }
  const result = await sendGraphQLRequest(query, resolver, variables)
  const json = await result.json()

  if (!json.data.viewer.project) {
    throw json
  }

  debug(`${JSON.stringify(json)}`)

  const projectInfo = {
    projectId: json.data.viewer.project.id,
    name: json.data.viewer.project.name,
    schema: json.data.viewer.project.schema,
  } as ProjectInfo
  return projectInfo
}

export async function exportProjectData(projectId: string, resolver: Resolver): Promise<string> {

  const mutation = `\
mutation ($projectId: String!){
  exportData(input:{
    projectId: $projectId,
    clientMutationId: "asd"
  }) {
    url
  }
}`

  const variables = { projectId }
  const result = await sendGraphQLRequest(mutation, resolver, variables)
  const json = await result.json()

  debug(`Received JSON: ${JSON.stringify(json)}`)

  if (!json.data.exportData) {
    throw json
  }

  const url = json.data.exportData.url
  debug(`Download data from url: ${url}`)

  const dataResponse = await fetch(url)
  debug(`Received data response: ${JSON.stringify(dataResponse)}`)

  try {
    const blob = await dataResponse.blob()
    debug(`Blob received: ${blob}`)
  } catch(e) {
    debug(`ERROR: ${JSON.stringify(e)}`)
  }
  // download(url, './test.zip')

  return ''
}

function download(url: string, destination: string) {
  debug(`Download data from url: ${url}`)
  const file = fs.createWriteStream(destination)
  debug(`Created file stream`)
  const request = http.get(url, response => {
    debug(`Received response: ${JSON.stringify(response)}`)
    response.pipe(file)
    file.on('finish', () => file.close())
  }).on('error', error => {
    debug(`An error occured while writing to the file: ${JSON.stringify(error)}`)
    fs.unlink(destination)
  })
}

export function parseErrors(response: any): APIError[] {
  debug(`Parse errors: ${JSON.stringify(response)}`)
  const errors: APIError[] = response.errors.map(error => ({
    message: error.message,
    requestId: error.requestId,
    code: String(error.code)
  }))

  return errors
}

export function generateErrorOutput(apiErrors: APIError[]): string {
  const lines = apiErrors.map(error => `    ${error.message} (Request ID: ${error.requestId})`)
  const output = `  Errors:\n ${lines.join('\n')}\n\n${contactUsInSlackMessage}`
  return output
}














