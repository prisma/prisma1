import {Resolver, ProjectInfo, MigrationMessage, MigrationErrorMessage, MigrationResult} from '../types'
import {graphcoolConfigFilePath, systemAPIEndpoint} from '../utils/constants'
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
  alias: string,
  schema: string,
  resolver: Resolver
): Promise<ProjectInfo> {

  const includeAlias = alias.length > 0

  const mutation = `\
mutation addProject($schema: String!, name: String!) {
    addProject(input: {
      name: $name,
      schema: $schema,
      clientMutationId: "static"
    }) {
      project {
        id
        schema
      }
    }
  }
`
  const variables = { name, schema }

  debug(`Send variables: ${JSON.stringify(variables)}`)

  const result = await sendGraphQLRequest(mutation, resolver, variables)
  const json = await result.json()

  debug(`Received JSON: ${json}`)

  const projectId = json.addProject.project.id
  const version = '1'// result.addProject.version
  const fullSchema = json.addProject.project.schema
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
  }
}
`

  const variables = {
    newSchema,
    isDryRun
  }

  const result = await sendGraphQLRequest(mutation, resolver, variables)
  const json = await result.json()

  // debug(`Received json for 'push': ${JSON.stringify(json)}`)

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

  debug(`Received data: ${JSON.stringify(json)}`)

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

  const projectInfo = {
    projectId: json.data.viewer.project.id,
    name: json.data.viewer.project.name,
    schema: json.data.viewer.project.schema,
  } as ProjectInfo
  return projectInfo
}