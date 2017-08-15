import {
  Resolver,
  ProjectInfo,
  MigrationMessage,
  MigrationErrorMessage,
  MigrationResult,
  APIError,
  CommandInstruction,
} from '../types'
import {
  graphcoolConfigFilePath,
  systemAPIEndpoint,
  contactUsInSlackMessage,
  statusEndpoint,
} from '../utils/constants'
import 'isomorphic-fetch'
import { getFastestRegion } from '../utils/ping'
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
      name
      schema
      alias
      version
      region
    }
  }
}
`

  let variables: any = {name, schema}
  if (alias) {
    variables = {...variables, alias}
  }
  if (region) {
    variables = {...variables, region: region.toUpperCase()}
  } else {
    const fastestRegion = await getFastestRegion()
    variables = {...variables, region: fastestRegion.toUpperCase()}
  }

  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.addProject) {
    throw json
  }

  const projectId = json.data.addProject.project.id
  const version = json.data.addProject.project.version
  schema = json.data.addProject.project.schema
  const returnedAlias = json.data.addProject.project.alias
  name = json.data.addProject.project.name
  const returnedRegion = json.data.addProject.project.region

  const projectInfo = {projectId, version, alias: returnedAlias, schema, name, region: returnedRegion}

  return projectInfo
}

export async function pushNewSchema(newSchema: string,
                                    force: boolean,
                                    resolver: Resolver): Promise<MigrationResult> {

  const mutation = `\
 mutation($newSchema: String!, $force: Boolean!) {
  migrateProject(input: {
    newSchema: $newSchema,
    force: $force,
    isDryRun: false
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
      id
      name
      schema
      alias
      version
    }
  }
}
`

  const variables = {
    newSchema,
    force
  }

  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.migrateProject) {
    throw json
  }

  const messages = json.data.migrateProject.migrationMessages as [MigrationMessage]
  const errors = json.data.migrateProject.errors as [MigrationErrorMessage]
  const newVersion = json.data.migrateProject.project.version
  newSchema = json.data.migrateProject.project.schema
  const migrationResult = {messages, errors, newVersion, newSchema} as MigrationResult

  return migrationResult
}

export async function statusMessage(newSchema: string, resolver: Resolver): Promise<MigrationResult> {

  const mutation = `\
mutation($newSchema: String!) {
  migrateProject(input: {
    newSchema: $newSchema,
    isDryRun: true
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
        id
        name
        schema
        alias
        version
    }
  }
}`

  const variables = {
    newSchema,
  }

  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.migrateProject) {
    throw json
  }

  const messages = json.data.migrateProject.migrationMessages as [MigrationMessage]
  const errors = json.data.migrateProject.errors as [MigrationErrorMessage]
  const newVersion = json.data.migrateProject.project.version
  newSchema = json.data.migrateProject.project.schema
  const migrationResult = {messages, errors, newVersion, newSchema} as MigrationResult

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
            schema
            alias
            version
            region
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
    alias: p.alias,
    schema: p.schema,
    version: p.version,
    region: p.region,
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
      region
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
    region: json.data.viewer.project.region,
  }
  return projectInfo
}

export async function deleteProject(projectIds: string[], resolver: Resolver): Promise<string[]> {

  const inputArguments = projectIds.reduce((prev, current, index) => {
    return `${prev}$projectId${index}: String!${index < (projectIds.length - 1) ? ', ' : ''}`
  }, '')
  const singleMutations = projectIds.map((projectId, index) => `\
  ${projectId}: deleteProject(input:{
      projectId: $projectId${index},
      clientMutationId: "asd"
    }) {
      deletedId
  }`)

  const header = `mutation (${inputArguments}) `
  const body = singleMutations.join('\n')
  const mutation = `${header} { \n${body} \n}`
  const variables = projectIds.reduce((prev, current, index) => {
    prev[`projectId${index}`] = current
    return prev
  }, {})

  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (json.errors) {
    throw json
  }

  const deletedIds: string[] = Object.keys(json.data).map(key => json.data[key])
  return deletedIds
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

export async function cloneProject(
  projectId: String,
  clonedProjectName: string,
  includeMutationCallbacks: boolean,
  includeData: boolean,
  resolver: Resolver): Promise<ProjectInfo> {
  const mutation = `\
mutation ($projectId: String!, $name: String!, $includeMutationCallbacks: Boolean!, $includeData: Boolean!){
  cloneProject(input:{
    name: $name,
    projectId: $projectId,
    includeData: $includeData,
    includeMutationCallbacks: $includeMutationCallbacks,
    clientMutationId: "asd"
  }) {
    project {
      id
      name
      alias
      schema
      version
      region
    }
  }
}`

  const variables = { projectId, name: clonedProjectName, includeMutationCallbacks, includeData }
  const json = await sendGraphQLRequest(mutation, resolver, variables)

  if (!json.data.cloneProject) {
    throw json
  }

  const projectInfo: ProjectInfo = {
    projectId: json.data.cloneProject.project.id,
    name: json.data.cloneProject.project.name,
    schema: json.data.cloneProject.project.schema,
    alias: json.data.cloneProject.project.alias,
    version: String(json.data.cloneProject.project.version),
    region: json.data.cloneProject.project.region,
  }

  return projectInfo
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

export async function checkStatus(
  instruction: CommandInstruction,
  resolver: Resolver,
): Promise<any> {
  try {
    const configContents = resolver.read(graphcoolConfigFilePath)
    if (!configContents) {
      return
    }
    const {token} = JSON.parse(configContents)

    await fetch(statusEndpoint, {
      method: 'post',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(instruction),
    })
  } catch (e) {
    // no op
  }
}
