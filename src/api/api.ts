import {Resolver, ProjectInfo} from '../types'
import {authConfigFilePath, systemAPIEndpoint} from '../utils/constants'
const debug = require('debug')('graphcool-create')
import 'isomorphic-fetch'

async function sendGraphQLRequest(queryString: string, resolver: Resolver): Promise<any> {

  const token = resolver.read(authConfigFilePath)

  const result = await fetch(systemAPIEndpoint, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: queryString
  })

  return result

}

export async function createProject(name: string, aliasPart: string, schema: string, resolver: Resolver): Promise<ProjectInfo> {

  // create project
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