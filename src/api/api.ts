import {Resolver, ProjectInfo, MigrationMessage, MigrationResult} from '../types'
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
): Promise<[MigrationMessage]> {

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

  // const projectInfo = json.migrateProject.project as ProjectInfo
  const migrationMessages = json.data.migrateProject.migrationMessages as [MigrationMessage]
  // const migrationResult = { projectInfo, migrationMessages } as MigrationResult

  return migrationMessages
}