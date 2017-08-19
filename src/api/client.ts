import {GraphQLClient} from 'graphql-request'
import { statusEndpoint, systemAPIEndpoint } from '../utils/constants'
import {
  CommandInstruction,
  MigrateProjectPayload, MigrationResult, Project, ProjectDefinition, ProjectInfo,
  RemoteProject
} from '../types'
import { getFastestRegion } from '../utils/ping'
import {omit} from 'lodash'
import config from '../utils/config'

const REMOTE_PROJECT_FRAGMENT = `
  fragment RemoteProject on Project {
    id
    name
    schema
    alias
    version
    region
    projectDefinitionWithFileContent
  }
`

class Client {
  client: GraphQLClient
  constructor() {
    this.updateClient()
  }

  updateClient() {
    this.client = new GraphQLClient(systemAPIEndpoint, {
      headers: {
        Authorization: `Bearer ${config.token}`
      }
    })
  }

  async createProject(name: string, schema: string, alias?: string, region?: string): Promise<ProjectInfo> {
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
            ...RemoteProject
          }
        }
      }
      ${REMOTE_PROJECT_FRAGMENT}
      `

    // gather variables
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

    const {addProject: {project}} = await this.client.request<{addProject: {project: RemoteProject}}>(mutation, variables)

    return this.getProjectDefinition(project)
  }

  private getProjectDefinition(project: RemoteProject): ProjectInfo {
    return {
      ...omit<Project, RemoteProject>(project, 'projectDefinitionWithFileContent'),
      projectDefinition: JSON.parse(project.projectDefinitionWithFileContent) as ProjectDefinition,
    }
  }

  async migrateProject(newSchema: string, force: boolean, isDryRun: boolean): Promise<MigrationResult> {
    const mutation = `\
      mutation($newSchema: String!, $force: Boolean!, $isDryRun: Boolean!) {
        migrateProject(input: {
          newSchema: $newSchema,
          force: $force,
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
            id
            name
            schema
            alias
            version
          }
        }
      }
    `
    const {migrateProject} = await this.client.request<{migrateProject: MigrateProjectPayload}>(mutation, {newSchema, force, isDryRun})

    return {
      migrationMessages: migrateProject.migrationMessages,
      errors: migrateProject.errors,
      newVersion: migrateProject.project.version,
      newSchema: migrateProject.project.schema,
    }
  }

  async fetchProjects(): Promise<Project[]> {
    interface ProjectsPayload {
      viewer: {
        user: {
          projects: {
            edges: Array<{
              node: ProjectInfo
            }>
          }
        }
      }
    }
    const result = await this.client.request<ProjectsPayload>(`
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
      }`)

    return result.viewer.user.projects.edges.map(edge => edge.node)
  }

  async fetchProjectInfo(projectId: string): Promise<ProjectInfo> {
    interface ProjectInfoPayload {
      viewer: {
        project: RemoteProject
      }
    }

    const {viewer: {project}} = await this.client.request<ProjectInfoPayload>(`
      query ($projectId: ID!){
        viewer {
          project(id: $projectId) {
            ...RemoteProject
          }
        }
      }
      ${REMOTE_PROJECT_FRAGMENT}
      `, {projectId})

    return this.getProjectDefinition(project)
  }

  async deleteProjects(projectIds: string[]): Promise<string[]> {
    const inputArguments = projectIds.reduce((prev, current, index) => {
      return `${prev}$projectId${index}: String!${index < (projectIds.length - 1) ? ', ' : ''}`
    }, '')
    const singleMutations = projectIds.map((projectId, index) => `
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

    interface DeletePayload {
      [projectId: string]: {
        deletedId: string
      }
    }

    const result = await this.client.request<DeletePayload>(mutation, variables)

    return Object.keys(result).map(projectId => result[projectId].deletedId)
  }

  async exportProjectData(projectId: string): Promise<string> {
    interface ExportPayload {
      exportData: {
        url: string
      }
    }

    const {exportData} = await this.client.request<ExportPayload>(`
      mutation ($projectId: String!){
        exportData(input:{
          projectId: $projectId,
          clientMutationId: "asd"
        }) {
          url
        }
      }
    `, {projectId})

    return exportData.url
  }

  async cloneProject(variables: {
                       projectId: string,
                       name: string,
                       includeMutationCallbacks: boolean,
                       includeData: boolean,
                     }): Promise<ProjectInfo> {

    interface CloneProjectPayload {
      project: RemoteProject
    }

    const {project} = await this.client.request<CloneProjectPayload>(`
      mutation ($projectId: String!, $name: String!, $includeMutationCallbacks: Boolean!, $includeData: Boolean!){
        cloneProject(input:{
          name: $name,
          projectId: $projectId,
          includeData: $includeData,
          includeMutationCallbacks: $includeMutationCallbacks,
          clientMutationId: "asd"
        }) {
          project {
            ...RemoteProject
          }
        }
      }
      ${REMOTE_PROJECT_FRAGMENT}
    `)

    return this.getProjectDefinition(project)
  }

  async checkStatus(instruction: CommandInstruction): Promise<any> {
    try {
      await fetch(statusEndpoint, {
        method: 'post',
        headers: {
          'Authorization': `Bearer ${config.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(instruction),
      })
    } catch (e) {
      // noop
    }
  }
}

const client = new Client()

export default client
