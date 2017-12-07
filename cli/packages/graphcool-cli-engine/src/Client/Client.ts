import {
  AccountInfo,
  AuthenticateCustomerPayload,
  FunctionInfo,
  FunctionLog,
  PAT,
  Project,
  ProjectDefinition,
  ProjectInfo,
  RemoteProject,
  SimpleProjectInfo,
} from '../types/common'

import { GraphQLClient, request } from 'graphql-request'
import { omit, flatMap } from 'lodash'
import { Config } from '../Config'
import { getFastestRegion } from './ping'
import { Environment } from '../Environment'
import { Output } from '../index'
import chalk from 'chalk'
import { Cluster } from '../Cluster'
import { DeployPayload } from './clientTypes'
import { introspectionQuery } from './introspectionQuery'

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

const MIGRATION_FRAGMENT = `
fragment MigrationFragment on Migration {
  revision
  steps {
    type
    __typename
    ... on CreateEnum {
      name
      ce_values: values
    }
    ... on CreateField {
      model
      name
      cf_typeName: typeName
      cf_isRequired: isRequired
      cf_isList: isList
      cf_isUnique: isUnique
      cf_relation: relation
      cf_defaultValue: defaultValue
      cf_enum: enum
    }
    ... on CreateModel {
      name
    }
    ... on CreateRelation {
      name
      leftModel
      rightModel
    }
    ... on DeleteEnum {
      name
    }
    ... on DeleteField {
      model
      name
    }
    ... on DeleteModel {
      name
    }
    ... on DeleteRelation {
      name
    }
    ... on UpdateEnum {
      name
      newName
      values
    }
    ... on UpdateField {
      model
      name
      newName
      typeName
      isRequired
      isList
      isUnique
      isRequired
      relation
      defaultValue
      enum
    }
    ... on UpdateModel {
      name
      um_newName: newName
    }
  }
  hasBeenApplied
}
`

export class Client {
  config: Config
  env: Environment
  out: Output
  public mock: (input: { request: any; response: any }) => void

  private mocks: { [request: string]: string } = {}
  private tokenCache: string

  constructor(config: Config, environment: Environment, out: Output) {
    this.config = config
    this.env = environment
    this.out = out
  }

  // always create a new client which points to the latest config for each request
  get client(): GraphQLClient {
    debug(
      'choosing clusterEndpoint',
      this.env.activeCluster.getDeployEndpoint(),
    )
    const localClient = new GraphQLClient(
      this.env.activeCluster.getDeployEndpoint(),
      {
        headers: {
          Authorization: `Bearer ${this.env.activeCluster.token}`,
        },
      },
    )
    return {
      request: async (query, variables) => {
        debug('Sending query')
        debug(query)
        debug(variables)
        try {
          return await localClient.request(query, variables)
        } catch (e) {
          if (e.message.includes('No service with id')) {
            // const user = await this.getAccount()
            const message = e.response.errors[0].message
            this.out.error(
              message +
                ` Please check if you are logged in to the right account.`,
            )
          } else if (e.message.startsWith('No valid session')) {
            // await this.auth.ensureAuth(true)
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

  // async getAccount(): Promise<AccountInfo> {
  //   const { viewer: { user } } = await this.client.request<{
  //     viewer: { user: AccountInfo }
  //   }>(`{
  //     viewer {
  //       user {
  //         email
  //         name
  //       }
  //     }
  //   }`)

  //   return user
  // }

  async introspect(serviceName: string, stageName: string): Promise<any> {
    return request(
      this.env.activeCluster.getApiEndpoint(serviceName, stageName),
      introspectionQuery,
    )
  }

  async addProject(name: string, stage: string): Promise<SimpleProjectInfo> {
    const mutation = `\
      mutation addProject($name: String! $stage: String!) {
        addProject(input: {
          name: $name,
          stage: $stage
        }) {
          project {
            name
          }
        }
      }
      `

    const { addProject: { project } } = await this.client.request<{
      addProject: { project: SimpleProjectInfo }
    }>(mutation, {
      name,
      stage,
    })

    // TODO set project definition, should be possibility in the addProject mutation

    return project
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

  async deploy(
    name: string,
    stage: string,
    types: string,
    dryRun: boolean,
  ): Promise<any> {
    // TODO add dryRun to query as soon as the backend is ready
    const mutation = `\
      mutation($name: String!, $stage: String! $types: String!) {
        deploy(input: {
          name: $name
          stage: $stage
          types: $types
        }) {
          errors {
            type
            field
            description
          }
          migration {
            ...MigrationFragment
          }
        }
      }
      ${MIGRATION_FRAGMENT}
    `
    const { deploy } = await this.client.request<{
      deploy: DeployPayload
    }>(mutation, {
      name,
      stage,
      types,
    })

    return deploy
  }

  async listProjects(): Promise<Project[]> {
    const { listProjects } = await this.client.request<{
      listProjects: Project[]
    }>(`
      {
        listProjects {
          name
          stage
        }
      }
    `)

    return listProjects
  }

  async resetServiceData(id: string): Promise<void> {
    // dont send any auth information when running the authenticateCustomer mutation
    await this.client.request(
      `mutation ($id: String!) {
          resetProjectData(input: {
            projectId: $id
          }) {
            clientMutationId
          }
        }
      `,
      { id },
    )
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
              __schema {
                directives {
                  description
                }
              }
            }
            `,
        )
        valid = true
      } catch (e) {
        debug(e)
        valid = false
      }

      await new Promise(r => setTimeout(r, 500))
    }
  }

  async waitForMigration(
    name: string,
    stage: string,
    revision: number,
  ): Promise<void> {
    while (true) {
      const { migrationStatus } = await this.client.request<any>(
        `query ($name: String! $stage: String!) {
              migrationStatus(name: $name stage: $stage) {
                revision
                hasBeenApplied
              }
            }
        `,

        {
          stage,
          name,
        },
      )

      if (
        migrationStatus.revision >= revision &&
        migrationStatus.hasBeenApplied
      ) {
        return
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

    const { node } = await this.client.request<FunctionLogsPayload>(
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
      return `${prev}$projectId${index}: String!${
        index < projectIds.length - 1 ? ', ' : ''
      }`
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
