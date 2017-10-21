import {
  AccountInfo,
  AuthenticateCustomerPayload,
  FunctionInfo,
  FunctionLog,
  MigrateProjectPayload,
  MigrationResult,
  PAT,
  Project,
  ProjectDefinition,
  ProjectInfo,
  RemoteProject,
} from '../types/common'

import { GraphQLClient, request } from 'graphql-request'
import { omit, flatMap } from 'lodash'
import { Config } from '../Config'
import { getFastestRegion } from './ping'
import { ProjectDefinitionClass } from '../ProjectDefinition/ProjectDefinition'
import { Environment } from '../Environment'
import { Output } from '../index'
import { Auth } from '../Auth'
import chalk from 'chalk'

const debug = require('debug')('client')

const REMOTE_PROJECT_FRAGMENT = `
  fragment RemoteProject on Project {
    id
    name
    schema
    alias
    region
    isEjected
    projectDefinitionWithFileContent
  }
`

export class Client {
  config: Config
  env: Environment
  out: Output
  auth: Auth
  public mock: (input: { request: any; response: any }) => void

  private mocks: { [request: string]: string } = {}
  private tokenCache: string

  constructor(config: Config, environment: Environment, out: Output) {
    this.config = config
    this.env = environment
    this.out = out
  }

  setAuth(auth: Auth) {
    this.auth = auth
  }

  // always create a new client which points to the latest config for each request
  get client(): GraphQLClient {
    debug('choosing clusterEndpoint', this.env.clusterEndpoint)
    const localClient = new GraphQLClient(this.env.clusterEndpoint, {
      headers: {
        Authorization: `Bearer ${this.env.token}`,
      },
    })
    return {
      request: async (query, variables) => {
        debug('Sending query')
        debug(query)
        debug(variables)
        try {
          return await localClient.request(query, variables)
        } catch (e) {
          if (e.message.includes('No project with id')) {
            const user = await this.getAccount()
            const message = e.response.errors[0].message
            this.out.error(message + ` in account ${user.email}. Please check if you are logged in to the right account.`)
          } else if (e.message.startsWith('No valid session')) {
            await this.auth.ensureAuth(true)
            // try again with new token
            return await this.client.request(query, variables)
          } else if (
            e.message.includes('ECONNREFUSED') &&
            (e.message.includes('localhost') || e.message.includes('127.0.0.1'))
          ) {
            this.out.error(
              `Could not connect to local cluster. Please use ${chalk.bold.green(
                'graphcool local up',
              )} to start your local Graphcool cluster.`,
            )
          } else {
            throw e
          }
        }
      },
    } as any
  }

  async getAccount(): Promise<AccountInfo> {
    const { viewer: { user } } = await this.client.request<{
      viewer: { user: AccountInfo }
    }>(`{
      viewer {
        user {
          email
          name
        }
      }
    }`)

    return user
  }

  async createProject(
    name: string,
    projectDefinition: ProjectDefinition,
    alias?: string,
    region?: string,
  ): Promise<ProjectInfo> {
    const mutation = `\
      mutation addProject($name: String!, $alias: String, $region: Region, $config: String) {
        addProject(input: {
          name: $name,
          alias: $alias,
          region: $region,
          clientMutationId: "static"
          config: $config
        }) {
          project {
            ...RemoteProject
          }
        }
      }
      ${REMOTE_PROJECT_FRAGMENT}
      `

    const newRegion = region || (await getFastestRegion())

    const { addProject: { project } } = await this.client.request<{
      addProject: { project: RemoteProject }
    }>(mutation, {
      name,
      alias,
      region: newRegion,
      config: JSON.stringify(
        ProjectDefinitionClass.sanitizeDefinition(projectDefinition),
      ),
    })

    // TODO set project definition, should be possibility in the addProject mutation

    return this.getProjectDefinition(project)
  }

  async getDeployUrl(projectId: string): Promise<string> {
    const mutation = `
      mutation getUrl($projectId: String!) {
        getTemporaryDeployUrl(
          input: {
          projectId: $projectId
        }
      ) {
          url
        }
      }
    `

    const { getTemporaryDeployUrl: { url } } = await this.client.request<{
      getTemporaryDeployUrl: { url: string }
    }>(mutation, { projectId })
    return url
  }

  async push(
    projectId: string,
    force: boolean,
    isDryRun: boolean,
    config: ProjectDefinition,
  ): Promise<MigrationResult> {
    const mutation = `\
      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {
        push(input: {
          projectId: $projectId
          force: $force
          isDryRun: $isDryRun
          config: $config
          version: 1
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
            alias
            projectDefinitionWithFileContent
          }
        }
      }
    `
    debug('\n\nSending project definition:')
    const sanitizedDefinition = ProjectDefinitionClass.sanitizeDefinition(
      config,
    )
    debug(this.out.getStyledJSON(sanitizedDefinition))
    const { push } = await this.client.request<{
      push: MigrateProjectPayload
    }>(mutation, {
      projectId,
      force,
      isDryRun,
      config: JSON.stringify(sanitizedDefinition),
    })

    debug()

    return {
      migrationMessages: push.migrationMessages,
      errors: push.errors,
      newSchema: push.project.schema,
      projectDefinition: this.getProjectDefinition(push.project as any)
        .projectDefinition,
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
                  alias
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

    const { viewer: { project } } = await this.client.request<
      ProjectInfoPayload
    >(
      `
      query ($projectId: ID!){
        viewer {
          project(id: $projectId) {
            ...RemoteProject
          }
        }
      }
      ${REMOTE_PROJECT_FRAGMENT}
      `,
      { projectId },
    )

    return this.getProjectDefinition(project)
  }

  async waitForLocalDocker(endpoint: string): Promise<void> {
    // dont send any auth information when running the authenticateCustomer mutation
    let valid = false
    while (!valid) {
      try {
        debug('requesting', endpoint)
        await request(
          endpoint,
          `
            {
              viewer {
                id
              }
            }
            `,
        )
        valid = true
      } catch (e) {
        valid = false
      }

      await new Promise(r => setTimeout(r, 500))
    }
  }

  async authenticateCustomer(
    endpoint: string,
    token: string,
  ): Promise<AuthenticateCustomerPayload> {
    // dont send any auth information when running the authenticateCustomer mutation
    const result = await request<{
      authenticateCustomer: AuthenticateCustomerPayload
    }>(
      endpoint,
      `
      mutation ($token: String!) {
        authenticateCustomer(input: {
          auth0IdToken: $token
        }) {
          token
          user {
            id
          }
        }
      }
      `,
      { token },
    )

    return result.authenticateCustomer
  }

  async getPats(projectId: string): Promise<PAT[]> {
    interface ProjectPatPayload {
      viewer: {
        project: {
          permanentAuthTokens: {
            edges: Array<{
              node: PAT
            }>
          }
        }
      }
    }
    const {
      viewer: { project: { permanentAuthTokens } },
    } = await this.client.request<ProjectPatPayload>(
      `
      query ($projectId: ID!){
        viewer {
          project(id: $projectId) {
            permanentAuthTokens {
              edges {
                node {
                  id
                  name
                  token
                }
              }
            }
          }
        }
      }
      `,
      { projectId },
    )

    if (!permanentAuthTokens) {
      return []
    }

    return permanentAuthTokens.edges.map(edge => edge.node)
  }

  async getFunctions(projectId: string): Promise<FunctionInfo[]> {
    interface FunctionsPayload {
      viewer: {
        project: {
          functions: {
            edges: Array<{
              node: FunctionInfo
            }>
          }
        }
      }
    }
    const { viewer: { project: { functions } } } = await this.client.request<
      FunctionsPayload
    >(
      `
      query ($projectId: ID!){
        viewer {
          project(id: $projectId) {
            functions {
              edges {
                node {
                  name
                  id
                  type
                  stats {
                    requestCount
                    errorCount
                  }
                  __typename
                }
              }
            }
          }
        }
      }
      `,
      { projectId },
    )

    return functions.edges.map(edge => edge.node)
  }

  async getFunction(
    projectId: string,
    functionName: string,
  ): Promise<FunctionInfo | null> {
    const functions = await this.getFunctions(projectId)
    const normalizedFunctionName = normalizeName(functionName)
    return (
      functions.find(fn => normalizeName(fn.name) === normalizedFunctionName) ||
      null
    )
  }

  async getFunctionLogs(
    functionId: string,
    count: number = 1000,
  ): Promise<FunctionLog[] | null> {
    interface FunctionLogsPayload {
      node: {
        logs: {
          pageInfo: {
            endCursor: string
          }
          edges: Array<{
            node: FunctionLog
          }>
        }
      }
    }

    const { node } = await this.client.request<
      FunctionLogsPayload
    >(
      `query ($id: ID!, $count: Int!) {
      node(id: $id) {
        ... on Function {
          logs(last: $count) {
            pageInfo {
              endCursor
            }
            edges {
              node {
                id
                requestId
                duration
                status
                timestamp
                message
              }
            }
          }
        }
      }
    }`,
      { id: functionId, count },
    )

    return node && node.logs ? node.logs.edges.map(edge => edge.node) : null
  }

  async getAllFunctionLogs(
    projectId: string,
    count: number = 1000,
  ): Promise<FunctionLog[] | null> {
    interface AllFunctionLogsPayload {
      viewer: {
        project: {
          functions: {
            edges: Array<{
              node: {
                logs: {
                  edges: Array<{
                    node: FunctionLog
                  }>
                }
              }
            }>
          }
        }
      }
    }

    const { viewer: { project } } = await this.client.request<
      AllFunctionLogsPayload
    >(
      `
        query ($id: ID!, $count: Int!) {
          viewer {
            project(id: $id) {
              functions {
                edges {
                  node {
                    logs(last: $count) {
                      edges {
                        node {
                          id
                          requestId
                          duration
                          status
                          timestamp
                          message
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        `,
      { id: projectId, count },
    )

    return project && project.functions && project.functions.edges
      ? flatMap(
          project.functions.edges.map(functionEdge =>
            functionEdge.node.logs.edges.map(logEdge => logEdge.node),
          ),
        )
      : null
  }

  async getProjectName(projectId: string): Promise<string> {
    interface ProjectInfoPayload {
      viewer: {
        project: {
          name: string
        }
      }
    }

    const { viewer: { project } } = await this.client.request<
      ProjectInfoPayload
    >(
      `
      query ($projectId: ID!){
        viewer {
          project(id: $projectId) {
            name
          }
        }
      }
      `,
      { projectId },
    )

    return project.name
  }

  async deleteProjects(projectIds: string[]): Promise<string[]> {
    const inputArguments = projectIds.reduce((prev, current, index) => {
      return `${prev}$projectId${index}: String!${index < projectIds.length - 1
        ? ', '
        : ''}`
    }, '')
    const singleMutations = projectIds.map(
      (projectId, index) => `
      ${projectId}: deleteProject(input:{
          projectId: $projectId${index},
          clientMutationId: "asd"
        }) {
          deletedId
      }`,
    )

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

    const { exportData } = await this.client.request<ExportPayload>(
      `
      mutation ($projectId: String!){
        exportData(input:{
          projectId: $projectId,
          clientMutationId: "asd"
        }) {
          url
        }
      }
    `,
      { projectId },
    )

    return exportData.url
  }

  async cloneProject(variables: {
    projectId: string
    name: string
    includeMutationCallbacks: boolean
    includeData: boolean
  }): Promise<ProjectInfo> {
    interface CloneProjectPayload {
      project: RemoteProject
    }

    const { project } = await this.client.request<CloneProjectPayload>(`
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

  async checkStatus(instruction: any): Promise<any> {
    try {
      await fetch(this.config.statusEndpoint, {
        method: 'post',
        headers: {
          Authorization: `Bearer ${this.env.token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(instruction),
      })
    } catch (e) {
      // noop
    }
  }

  private getProjectDefinition(project: RemoteProject): ProjectInfo {
    return {
      ...omit<Project, RemoteProject>(
        project,
        'projectDefinitionWithFileContent',
      ),
      projectDefinition: JSON.parse(
        project.projectDefinitionWithFileContent,
      ) as ProjectDefinition,
    }
  }
}

// only make this available in test mode
// if (process.env.NODE_ENV === 'test') {
//   Client.prototype.mock = function({ request, response }) {
//     if (!this.mocks) {
//       this.mocks = {}
//     }
//     this.mocks[JSON.stringify(request, null, 2)] = response
//   }
// }

function normalizeName(name: string) {
  return name.toLowerCase().trim()
}
