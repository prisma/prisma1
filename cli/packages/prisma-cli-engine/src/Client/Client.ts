import {
  AuthenticateCustomerPayload,
  Project,
  SimpleProjectInfo,
  CloudTokenRequestPayload,
} from '../types/common'

import { GraphQLClient } from 'graphql-request'
import { flatten } from 'lodash'
import { Config } from '../Config'
import { Environment, Cluster, FunctionInput, getProxyAgent } from 'prisma-yml'
import { Output } from '../index'
import chalk from 'chalk'
import { introspectionQuery } from './introspectionQuery'
import {
  User,
  Migration,
  DeployPayload,
  Workspace,
  Service,
  AuthenticationPayload,
} from './types'
import * as opn from 'opn'
import { concatName } from 'prisma-yml/dist/PrismaDefinition'
import { IntrospectionQuery } from 'graphql'
import { hasTypeWithField } from '../utils/graphql-schema'
import {
  renderMigrationFragment,
  renderStepFragment,
} from './migrationFragment'

const debug = require('debug')('client')

export class Client {
  config: Config
  env: Environment
  out: Output
  clusterClient: GraphQLClient
  public mock: (input: { request: any; response: any }) => void

  private mocks: { [request: string]: string } = {}
  private tokenCache: string

  constructor(config: Config, environment: Environment, out: Output) {
    this.config = config
    this.env = environment
    this.out = out
  }

  // always create a new client which points to the latest config for each request
  async initClusterClient(
    cluster: Cluster,
    serviceName: string,
    stageName?: string,
    workspaceSlug?: string | undefined | null,
  ) {
    try {
      const token = await cluster.getToken(
        serviceName,
        workspaceSlug || undefined,
        stageName,
      )
      debug(`is local cluster: ${cluster.local}`)
      const authenticationPayload = await this.isAuthenticated()
      if (!cluster.local && authenticationPayload.isAuthenticated && !cluster.shared) {
        // Added a check for login because we can only add a service to cloud when we are logged in
        const serviceCreatedInCloud = await cluster.addServiceToCloudDBIfMissing(
          serviceName,
          workspaceSlug || undefined,
          stageName
        )
        debug({ serviceCreatedInCloud })
      }
      const agent = getProxyAgent(cluster.getDeployEndpoint())
      this.clusterClient = new GraphQLClient(cluster.getDeployEndpoint(), {
        headers: {
          ...(token && {Authorization: `Bearer ${token}`}),
        },
        agent,
      } as any)
    } catch (e) {
      if (e.message.includes('Not authorized')) {
        await this.login()
        if (cluster.shared) {
          cluster.clusterSecret = this.env.cloudSessionKey
        }
        const token = await cluster.getToken(
          serviceName,
          workspaceSlug!,
          stageName,
        )
        this.clusterClient = new GraphQLClient(cluster.getDeployEndpoint(), {
          headers: {
            Authorization: `Bearer ${token}`,
          },
          agent: getProxyAgent(cluster.getDeployEndpoint()),
        } as any)
      } else {
        throw e
      }
    }
  }
  get client(): GraphQLClient {
    if (!this.env.activeCluster) {
      throw new Error(
        `No cluster set. Please set the "cluster" property in your prisma.yml`,
      )
    }
    return {
      request: async (query, variables) => {
        debug(`Sending query to cluster ${this.env.activeCluster.name}`)
        debug(this.env.activeCluster.getDeployEndpoint())
        debug(query)
        debug(variables)
        try {
          const result = await this.clusterClient.request(query, variables)
          debug(result)
          return result
        } catch (e) {
          if (e.response && e.response.errors && e.response.errors[0]) {
            if (
              e.response.errors[0].code === 3015 &&
              e.response.errors[0].message.includes('decoded') &&
              this.env.activeCluster.local
            ) {
              if (!process.env.PRISMA_MANAGEMENT_API_SECRET) {
                throw new Error(
                  `Server at ${chalk.bold(
                    this.env.activeCluster.name,
                  )} requires the Management API secret. Please set the the ${chalk.bold(
                    'PRISMA_MANAGEMENT_API_SECRET',
                  )} environment variable.

                  Learn more about this error in the docs: https://bit.ly/authentication-and-security-docs`,
                )
              } else {
                throw new Error(
                  `Can not authenticate against Prisma server. It seems that your ${chalk.bold(
                    'PRISMA_MANAGEMENT_API_SECRET',
                  )} environment variable is set incorrectly. Please make sure that it matches the value that was used when the Prisma server was deployed.

                  For more info visit: https://bit.ly/authentication-and-security-docs`,
                )
              }
            }

            if (
              e.response.errors[0].code === 3016 &&
              e.response.errors[0].message.includes('management$default')
            ) {
              // TODO: make url mutable in graphql client
              ;(this.clusterClient as any).url = (this
                .clusterClient as any).url.replace(/management$/, 'cluster')

              const result = await this.clusterClient.request(query, variables)
              debug(result)
              return result
            }
          }

          if (
            e.message.includes('HTTP method not allowed') &&
            (this.clusterClient as any).url.endsWith('management')
          ) {
            // TODO: make url mutable in graphql client
            ;(this.clusterClient as any).url = (this
              .clusterClient as any).url.replace(/management$/, 'cluster')

            const result = await this.clusterClient.request(query, variables)
            debug(result)
            return result
          }

          if (e.message.includes('ECONNRESET')) {
            await new Promise(r => setTimeout(r, 5000))
            const result = await this.clusterClient.request(query, variables)
            debug(result)
            return result
          }

          if (
            e.message.includes('ECONNREFUSED') &&
            (e.message.includes('localhost') || e.message.includes('127.0.0.1'))
          ) {
            const localNotice = this.env.activeCluster.local
              ? `Please use ${chalk.bold.green(
                  'docker-compose up -d',
                )} to start your local Prisma cluster.`
              : ''
            throw new Error(
              `Could not connect to cluster ${chalk.bold(
                this.env.activeCluster.name,
              )}. ${localNotice}`,
            )
          } else {
            throw e
          }
        }
      },
    } as any
  }
  get cloudClient(): GraphQLClient {
    const options = {
      headers: {},
      agent: getProxyAgent(this.config.cloudApiEndpoint),
    }
    if (this.env.cloudSessionKey) {
      options.headers = {
        Authorization: `Bearer ${this.env.cloudSessionKey}`,
      }
    }
    const client = new GraphQLClient(this.config.cloudApiEndpoint, options)
    return {
      request: async (query, variables) => {
        debug('Sending query to cloud api')
        debug(this.config.cloudApiEndpoint)
        debug(query)
        debug(variables)
        debug(options)
        return client.request(query, variables)
      },
    } as any
  }

  async introspect(
    serviceName: string,
    stageName: string,
    token?: string,
    workspaceSlug?: string,
  ): Promise<any> {
    debug('introspecting', { serviceName, stageName, workspaceSlug })
    const headers: any = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
    const endpoint = this.env.activeCluster.getApiEndpoint(
      serviceName,
      stageName,
      workspaceSlug,
    )
    const client = new GraphQLClient(endpoint, {
      headers,
      agent: getProxyAgent(this.config.cloudApiEndpoint),
    } as any)
    return client.request(introspectionQuery)
  }

  async exec(
    serviceName: string,
    stageName: string,
    query: string,
    token?: string,
    workspaceSlug?: string,
  ): Promise<any> {
    const headers: any = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
    const client = new GraphQLClient(
      this.env.activeCluster.getApiEndpoint(
        serviceName,
        stageName,
        workspaceSlug,
      ),
      {
        headers,
        agent: getProxyAgent(this.config.cloudApiEndpoint),
      } as any,
    )
    return client.request(query)
  }

  async download(
    serviceName: string,
    stage: string,
    exportData: any,
    token?: string,
    workspaceSlug?: string,
  ): Promise<any> {
    const endpoint = this.env.activeCluster.getExportEndpoint(
      serviceName,
      stage,
      workspaceSlug,
    )
    debug(`Downloading from ${endpoint}`)
    debug(exportData)
    const result = await fetch(endpoint, {
      method: 'post',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: exportData,
      agent: getProxyAgent(endpoint),
    } as any)

    const text = await result.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      throw new Error(text)
    }
  }

  async upload(
    serviceName: string,
    stage: string,
    exportData: any,
    token?: string,
    workspaceSlug?: string,
  ): Promise<any> {
    const endpoint = this.env.activeCluster.getImportEndpoint(
      serviceName,
      stage,
      workspaceSlug,
    )
    debug(`Uploading to endpoint ${endpoint}`)
    const result = await fetch(endpoint, {
      method: 'post',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: exportData,
      agent: getProxyAgent(endpoint),
    } as any)

    const text = await result.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      throw new Error(text)
    }
  }

  async reset(
    serviceName: string,
    stage: string,
    token?: string,
    workspaceSlug?: string,
  ): Promise<void> {
    const endpoint =
      this.env.activeCluster.getApiEndpoint(serviceName, stage, workspaceSlug) +
      '/private'
    const result = await fetch(endpoint, {
      method: 'post',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        query: `mutation {
          resetData
        }`,
      }),
      agent: getProxyAgent(endpoint),
    } as any)

    const text = await result.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      throw new Error(text)
    }
  }

  async requestCloudToken(): Promise<string> {
    const mutation = `mutation {
      requestCloudToken {
        secret
      }
    }`

    interface RequestCloudTokenPayload {
      requestCloudToken: {
        secret: string
      }
    }

    const {
      requestCloudToken: { secret },
    } = await this.cloudClient.request<RequestCloudTokenPayload>(mutation)

    return secret
  }

  async cloudTokenRequest(secret: string): Promise<CloudTokenRequestPayload> {
    const query = `query ($secret: String!) {
      cloudTokenRequest(secret: $secret) {
        secret
        token
      }
    }`

    const { cloudTokenRequest } = await this.cloudClient.request<{
      cloudTokenRequest: CloudTokenRequestPayload
    }>(query, { secret })

    return cloudTokenRequest
  }

  async ensureAuth(): Promise<void> {
    const authenticationPayload = await this.isAuthenticated()

    if (!authenticationPayload.isAuthenticated) {
      await this.login()
    }
  }

  async login(key?: string): Promise<void> {
    let token: null | string = null
    this.out.action.start(`Authenticating`)
    if (key) {
      this.env.globalRC.cloudSessionKey = key
    }
    let authenticationPayload = await this.isAuthenticated()
    const authenticated = authenticationPayload.isAuthenticated
    if (authenticated) {
      this.out.action.stop()
      this.out.log(
        `Authenticated with ${authenticationPayload.account!.login[0].email}`,
      )
      this.out.log(key ? 'Successfully signed in' : 'Already signed in')
      if (key) {
        this.env.saveGlobalRC()
      }
      return
    }
    const secret = await this.requestCloudToken()

    const url = `${this.config.consoleEndpoint}/cli-auth?secret=${secret}`

    this.out.log(`Opening ${url} in the browser\n`)

    opn(url).catch(e => {
      console.error(
        `Could not open the authentication link, maybe this is an environment without a browser. Please open this url in your browser to authenticate: ${url}`,
      )
    })

    while (!token) {
      const cloud = await this.cloudTokenRequest(secret)
      if (cloud.token) {
        token = cloud.token
      }
      await new Promise(r => setTimeout(r, 1000))
    }
    this.env.globalRC.cloudSessionKey = token
    this.out.action.stop()

    authenticationPayload = await this.isAuthenticated()
    await this.out.log(
      `Authenticated with ${authenticationPayload.account!.login[0].email}`,
    )

    this.env.saveGlobalRC()
    await this.env.fetchClusters()
  }

  logout(): void {
    delete this.env.globalRC.cloudSessionKey
    this.env.saveGlobalRC()
  }

  async getAccount(): Promise<User | null> {
    const query = `{
      me {
        id
        name
        login {
          email
        }
      }
    }`

    const { me } = await this.cloudClient.request<{
      me: User
    }>(query)

    return me
  }

  async getCloudServices(): Promise<Service[]> {
    const query = `
  {
    me {
      memberships {
        workspace {
          services {
            id
            stage
            name
            cluster {
              name
            }
          }
        }
      }
    }
  }
      `

    const { me } = await this.cloudClient.request<{
      me: {
        memberships: Array<{
          workspace: {
            services: Service[]
          }
        }>
      }
    }>(query)

    return flatten(me.memberships.map(m => m.workspace.services))
  }

  async generateClusterToken(
    workspaceSlug: string,
    clusterName: string,
    serviceName: string,
    stageName: string,
  ): Promise<string> {
    debug('Calling generateClusterToken')
    const query = `
      mutation ($input: GenerateClusterTokenRequest!) {
        generateClusterToken(input: $input) {
          clusterToken
        }
      }
    `

    const {
      generateClusterToken: { clusterToken },
    } = await this.cloudClient.request<{
      generateClusterToken: {
        clusterToken: string
      }
    }>(query, {
      input: {
        workspaceSlug,
        clusterName,
        serviceName,
        stageName,
      },
    })

    return clusterToken
  }

  async isAuthenticated(): Promise<AuthenticationPayload> {
    let authenticated = false
    let account: User | null = null
    try {
      account = await this.getAccount()
      if (account) {
        authenticated = Boolean(account)
      }
    } catch (e) {
      //
    }

    return {
      isAuthenticated: authenticated,
      account,
    }
  }

  async getWorkspaces(): Promise<Workspace[]> {
    const query = `{
      me {
        memberships {
          workspace {
            id
            name
            slug
            clusters {
              id
              name
              connectInfo {
                endpoint
              }
            }
          }
        }
      }
    }`

    const {
      me: { memberships },
    } = await this.cloudClient.request<{
      me: {
        memberships: Array<{ workspace: Workspace }>
      }
    }>(query)

    return memberships.map(m => m.workspace)
  }

  async addProject(
    name: string,
    stage: string,
    secrets: string[] | null,
  ): Promise<SimpleProjectInfo> {
    const mutation = `\
      mutation addProject($name: String! $stage: String! $secrets: [String!]) {
        addProject(input: {
          name: $name,
          stage: $stage
          secrets: $secrets
        }) {
          project {
            name
          }
        }
      }
      `

    const result = await this.client.request<{
      addProject: { project: SimpleProjectInfo }
    }>(mutation, {
      name,
      stage,
      secrets,
    })

    if (!result) {
      throw new Error(`Could not create service ${name}`)
    }

    const {
      addProject: { project },
    } = result

    // TODO set project definition, should be possibility in the addProject mutation

    return project
  }

  async deleteProject(
    name: string,
    stage: string,
    workspaceSlug: string | null,
  ): Promise<void> {
    const cluster = this.env.activeCluster
    if (!this.env.activeCluster.shared && !this.env.activeCluster.isPrivate) {
      const mutation = `\
      mutation ($input: DeleteProjectInput!) {
        deleteProject(input: $input) {
          clientMutationId
        }
      }
      `

      await this.client.request(mutation, {
        input: {
          name: concatName(cluster as any, name, workspaceSlug),
          stage,
        },
      })
    } else {
      const mutation = `\
        mutation ($input: ServiceDeletionInput!) {
          deleteService(input: $input) {
            id
          }
        }
      `

      await this.cloudClient.request(mutation, {
        input: {
          name,
          clusterName: cluster.name,
          workspaceSlug,
          stage,
        },
      })
    }
  }

  async hasStepsApi() {
    const result: IntrospectionQuery = await this.client.request<
      IntrospectionQuery
    >(introspectionQuery)

    return hasTypeWithField(result, 'DeployPayload', 'steps')
  }

  async deploy(
    name: string,
    stage: string,
    types: string,
    dryRun: boolean,
    subscriptions: FunctionInput[],
    secrets: string[] | null,
    force?: boolean,
    noMigration?: boolean,
  ): Promise<DeployPayload> {
    const oldMutation = `\
      mutation($name: String!, $stage: String! $types: String! $dryRun: Boolean $secrets: [String!], $subscriptions: [FunctionInput!]) {
        deploy(input: {
          name: $name
          stage: $stage
          types: $types
          dryRun: $dryRun
          secrets: $secrets
          subscriptions: $subscriptions
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
      ${renderMigrationFragment(false)}
    `

    const introspectionResult: IntrospectionQuery = await this.client.request<
      IntrospectionQuery
    >(introspectionQuery)
    const hasStepsApi = hasTypeWithField(
      introspectionResult,
      'DeployPayload',
      'steps',
    )

    const hasRelationManifestationApi = hasTypeWithField(
      introspectionResult,
      'CreateRelation',
      'after',
    )

    const steps = hasStepsApi
      ? `steps { ${renderStepFragment(hasRelationManifestationApi)} }`
      : ''

    if (
      noMigration &&
      !hasTypeWithField(introspectionResult, 'DeployInput', 'noMigration')
    ) {
      throw new Error(
        `You provided the --no-migrate option, but the Prisma server doesn't support it yet. It's supported in Prisma 1.26 and above.`,
      )
    }
    const noMigrationInput = noMigration ? 'noMigration: true' : ''

    const newMutation = `\
      mutation($name: String!, $stage: String! $types: String! $dryRun: Boolean $secrets: [String!], $subscriptions: [FunctionInput!], $force: Boolean) {
        deploy(input: {
          name: $name
          stage: $stage
          types: $types
          dryRun: $dryRun
          secrets: $secrets
          subscriptions: $subscriptions
          force: $force
          ${noMigrationInput}
        }) {
          errors {
            type
            field
            description
          }
          warnings {
            type
            field
            description
          }
          migration {
            ...MigrationFragment
          }
          
          ${steps}
        }
      }
      ${renderMigrationFragment(hasRelationManifestationApi)}
    `

    try {
      const { deploy } = await this.client.request<{
        deploy: DeployPayload
      }>(newMutation, {
        name,
        stage,
        types,
        dryRun,
        secrets,
        subscriptions,
        force,
      })

      return deploy
    } catch (e) {
      if (
        e.message.includes(`Field 'force' is not defined in the input type`)
      ) {
        const { deploy } = await this.client.request<{
          deploy: DeployPayload
        }>(oldMutation, {
          name,
          stage,
          types,
          dryRun,
          secrets,
          subscriptions,
        })

        return deploy
      } else {
        throw e
      }
    }
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

  async getProject(name: string, stage: string): Promise<Project | null> {
    const { project } = await this.client.request<{
      project: Project | null
    }>(
      `
      query($name: String! $stage: String!) {
        project(name: $name stage: $stage) {
          name
          stage
        }
      }
    `,
      { name, stage },
    )

    return project
  }

  async getCluster(name: string, stage: string): Promise<Cluster | null> {
    const foundClusters: Cluster[] = []
    for (const cluster of this.env.clusters) {
      try {
        const { project } = await this.client.request<{
          project: Project
        }>(
          `
            query ($name: String! $stage: String!) {
              project (name: $name stage: $stage) {
                name
                stage
              }
            }
          `,
          {
            name,
            stage,
          },
        )

        if (project) {
          foundClusters.push(cluster)
        }
      } catch (e) {
        //
      }
    }

    if (foundClusters.length === 1) {
      return foundClusters[0]
    }

    if (foundClusters.length > 1) {
      const clusterNames = foundClusters.map(c => c.name).join(', ')
      throw new Error(
        `The service name / stage combination "${name}@${stage}" is ambigious. It exists in clusters ${clusterNames}`,
      )
    }

    return null
  }

  async getClusterSafe(name: string, stage: string): Promise<Cluster> {
    const cluster = await this.getCluster(name, stage)
    if (!cluster) {
      throw new Error(
        `No cluster for "${name}@${stage}" found. Please make sure to deploy the stage ${stage}`,
      )
    }

    return cluster
  }

  async waitForLocalDocker(endpoint: string): Promise<void> {
    // dont send any auth information when running the authenticateCustomer mutation
    let valid = false
    const client = new GraphQLClient(endpoint, {
      agent: getProxyAgent(endpoint),
    } as any)
    while (!valid) {
      try {
        await client.request(
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

  async getMigration(name: string, stage: string): Promise<Migration> {
    const { migrationStatus } = await this.client.request<{
      migrationStatus: Migration
    }>(
      `query ($name: String! $stage: String!) {
          migrationStatus(name: $name stage: $stage) {
            projectId
            revision
            status
            applied
            rolledBack
            errors
          }
        }
      `,

      {
        stage,
        name,
      },
    )

    return migrationStatus
  }

  async authenticateCustomer(
    endpoint: string,
    token: string,
  ): Promise<AuthenticateCustomerPayload> {
    // dont send any auth information when running the authenticateCustomer mutation
    const client = new GraphQLClient(endpoint, {
      agent: getProxyAgent(endpoint),
    } as any)
    const result = await client.request<{
      authenticateCustomer: AuthenticateCustomerPayload
    }>(
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
