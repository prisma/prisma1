import {Resolver, ProjectInfo, MigrationMessage, MigrationErrorMessage, MigrationResult} from '../types'
import {authConfigFilePath, systemAPIEndpoint} from '../utils/constants'
const debug = require('debug')('graphcool')
import 'isomorphic-fetch'

async function sendGraphQLRequest(
  queryString: string,
  resolver: Resolver,
  variables?: any
): Promise<any> {

  const {token} = JSON.parse(resolver.read(authConfigFilePath))
  // const token = `eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0OTIxMTI1ODgsImNsaWVudElkIjoiY2luYmt5c2d2MDAwMngzaTZxZDR3ZHc1dCJ9.ujuTZXtmiqjdOBX6-beq7EUE9RxgNNSj0UG-acmMcbk`
  debug(`Send GraphQL request with token: ${token}`)

  const queryVariables = variables || {}

  const payload = {
    query: queryString,
    variables: queryVariables
  }

  debug(`Send payload as POST body: ${JSON.stringify(payload)}`)

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
  aliasPart: string,
  schema: string,
  resolver: Resolver
): Promise<ProjectInfo> {

  const result = await sendGraphQLRequest(`mutation addProject($schema: String) {
    addProject(input: {
      name: "${name}"
      ${aliasPart}
      schema: "${schema}"
      clientMutationId: "static"
    }) {
      project {
        id
        schema
      }
    }
  }`, resolver)

  const json = await result.json()
  const projectId = json.addProject.project.id
  const version = '0.1'// result.addProject.version
  const fullSchema = json.addProject.project.schema
  const projectInfo = {projectId, version, schema: fullSchema}

  return projectInfo
}

export async function pushNewSchema(
  projectId: string,
  newSchema: string,
  isDryRun: boolean,
  resolver: Resolver
): Promise<MigrationResult> {

  const mutation = `\
     mutation($id: String!, $newSchema: String!, $isDryRun: Boolean!) {
      migrateProject(input: {
        id: $id,
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
    id: projectId,
    newSchema,
    isDryRun
  }

  const result = await sendGraphQLRequest(mutation, resolver, variables)
  const json = await result.json()

  debug(`Received json for 'push': ${JSON.stringify(json)}`)

  const messages = json.data.migrateProject.migrationMessages as [MigrationMessage]
  const errors = json.data.migrateProject.errors as [MigrationErrorMessage]

  const migrationResult = { messages, errors } as MigrationResult

  return migrationResult
}

export async function fetchProjects(resolver: Resolver): Promise<[ProjectInfo]> {

  const query = `\
viewer{
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
`

  const result = await sendGraphQLRequest(query, resolver)
  const json = await result.json()

  const projects = json.data.viewer.user.projects.edges.map(edge => edge.node)
  const projectInfos = projects.map(project => ({projectId: project.id, name: project.name})) as [ProjectInfo]

  return projectInfos
}