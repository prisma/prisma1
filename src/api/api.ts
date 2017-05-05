import { Resolver, ProjectInfo, MigrationMessage, MigrationErrorMessage, MigrationResult, APIError } from '../types'
import { graphcoolConfigFilePath, systemAPIEndpoint, contactUsInSlackMessage } from '../utils/constants'
import 'isomorphic-fetch'
const debug = require('debug')('graphcool')

async function sendGraphQLRequest(queryString: string,
                                  resolver: Resolver,
                                  variables?: any): Promise<any> {

  const configContents = resolver.read(graphcoolConfigFilePath)
  const {token} = JSON.parse(configContents)

  const queryVariables = variables || {}
  const payload = {
    query: queryString,
    variables: queryVariables
  }

  debug(`Send request to ${systemAPIEndpoint}\nPayload: \n${JSON.stringify(payload)}`)

  const result = await fetch(systemAPIEndpoint, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload),
  })
  const json = await result.json()

  if (process.env.DEBUG === 'graphcool') {
    debug(`Received JSON response: \n${JSON.stringify(json)}`)
  }

  return json
}

export async function createProject(name: string,
                                    schema: string,
                                    resolver: Resolver,
                                    alias?: string,
                                    region?: string): Promise<ProjectInfo> {

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
        alias
      }
    }
  }
`

  let variables: any = {name, schema}
  if (alias) {
    variables = {...variables, alias}
  }
  if (region) {
    variables = {...variables, region}
  }

  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.addProject) {
    throw json
  }

  const projectId = json.data.addProject.project.id
  const version = '1' // result.addProject.version
  schema = json.data.addProject.project.schema
  const projectInfo = {projectId, version, alias, schema}

  return projectInfo
}

export async function pushNewSchema(newSchema: string,
                                    isDryRun: boolean,
                                    resolver: Resolver): Promise<MigrationResult> {

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

  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.migrateProject) {
    throw json
  }

  const messages = json.data.migrateProject.migrationMessages as [MigrationMessage]
  const errors = json.data.migrateProject.errors as [MigrationErrorMessage]
  const newVersion = json.data.migrateProject.project.version
  const migrationResult = {messages, errors, newVersion} as MigrationResult

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
            alias
          }
        }
      }
    }
  }
}`

  const json = await sendGraphQLRequest(query, resolver) // await fetch(systemAPIEndpoint, {

  const projects = json.data.viewer.user.projects.edges.map(edge => edge.node)
  const projectInfos: [ProjectInfo] = projects.map(p => ({
    projectId: p.id,
    name: p.name,
    alias: p.alias
  }))

  return projectInfos
}

export async function pullProjectInfo(projectId: string, resolver: Resolver): Promise<ProjectInfo> {

  const query = `\
query ($projectId: ID!){
  viewer {
    project(id: $projectId) {
      id
      name
      alias
      schema
      version
    }
  }
}`

  const variables = {projectId}

  const json = await sendGraphQLRequest(query, resolver, variables)

  if (!json.data.viewer.project) {
    throw json
  }

  const projectInfo: ProjectInfo = {
    projectId: json.data.viewer.project.id,
    name: json.data.viewer.project.name,
    schema: json.data.viewer.project.schema,
    alias: json.data.viewer.project.alias,
    version: String(json.data.viewer.project.version),
  }
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
  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.exportData) {
    throw json
  }

  return json.data.exportData.url
}

export function parseErrors(response: any): APIError[] {
  const errors: APIError[] = response.errors.map(error => ({
    message: error.message,
    requestId: error.requestId,
    code: String(error.code)
  }))

  return errors
}

export function generateErrorOutput(apiErrors: APIError[]): string {
  const lines = apiErrors.map(error => `${error.message} (Request ID: ${error.requestId})`)
  const output = `\n${lines.join('\n')}\n\n${contactUsInSlackMessage}`
  return output
}
