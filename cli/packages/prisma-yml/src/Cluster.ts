const debug = require('debug')('environment')
import 'isomorphic-fetch'
import * as jwt from 'jsonwebtoken'
import { cloudApiEndpoint } from './constants'
import { GraphQLClient } from 'graphql-request'
import chalk from 'chalk'

export class Cluster {
  name: string
  baseUrl: string
  local: boolean
  shared: boolean
  clusterSecret?: string
  requiresAuth: boolean
  private cachedToken?: string
  constructor(
    name: string,
    baseUrl: string,
    clusterSecret?: string,
    local: boolean = true,
    shared: boolean = true,
  ) {
    this.name = name
    this.baseUrl = baseUrl
    this.clusterSecret = clusterSecret
    this.local = local
    this.shared = shared
  }

  async getToken(
    serviceName: string,
    workspaceSlug?: string,
    stageName?: string,
  ): Promise<string> {
    // public clusters just take the token
    if (this.name === 'shared-public-demo') {
      return ''
    }
    if (this.shared) {
      return this.generateClusterToken(serviceName, workspaceSlug, stageName)
    } else {
      return this.getLocalToken()
    }
  }

  getLocalToken() {
    if (!this.clusterSecret || this.clusterSecret === '') {
      throw new Error(`Property '${chalk.bold(
        'clusterSecret',
      )}' of cluster ${chalk.bold(this.name)} in ~/.prisma/config.yml is empty.
Please either provide a clusterSecret or run ${chalk.green.bold(
        'prisma local start',
      )} to generate a new one. Read more here https://bit.ly/prisma-graphql-config-yml`)
    }
    if (!this.cachedToken) {
      const grants = [{ target: `*/*`, action: '*' }]

      try {
        this.cachedToken = jwt.sign({ grants }, this.clusterSecret, {
          expiresIn: '10 minutes',
          algorithm: 'RS256',
        })
      } catch (e) {
        throw new Error(
          `Could not generate token for local cluster ${chalk.bold(
            this.name,
          )}. ${e.message}`,
        )
      }
    }

    return this.cachedToken!
  }

  get cloudClient() {
    return new GraphQLClient(cloudApiEndpoint, {
      headers: {
        Authorization: `Bearer ${this.clusterSecret}`,
      },
    })
  }

  async generateClusterToken(
    serviceName: string,
    workspaceSlug: string = '*',
    stageName?: string,
  ): Promise<string> {
    const query = `
      mutation ($input: GenerateClusterTokenRequest!) {
        generateClusterToken(input: $input) {
          clusterToken
        }
      }
    `

    debug('generateClusterToken', cloudApiEndpoint)

    const {
      generateClusterToken: { clusterToken },
    } = await this.cloudClient.request<{
      generateClusterToken: {
        clusterToken: string
      }
    }>(query, {
      input: {
        workspaceSlug,
        clusterName: this.name,
        serviceName,
        stageName,
      },
    })

    debug('generated cluster token')

    return clusterToken
  }

  getApiEndpoint(serviceName: string, stage: string, workspaceSlug?: string) {
    const workspaceString = workspaceSlug ? `${workspaceSlug}/` : ''
    return `${this.baseUrl}/${workspaceString}${serviceName}/${stage}`
  }

  getWSEndpoint(serviceName: string, stage: string, workspaceSlug?: string) {
    const replacedUrl = this.baseUrl.replace('http', 'ws')
    const workspaceString = workspaceSlug ? `${workspaceSlug}/` : ''
    return `${replacedUrl}/${workspaceString}${serviceName}/${stage}`
  }

  getImportEndpoint(
    serviceName: string,
    stage: string,
    workspaceSlug?: string,
  ) {
    const workspaceString = workspaceSlug ? `${workspaceSlug}/` : ''
    return `${this.baseUrl}/${workspaceString}${serviceName}/${stage}/import`
  }

  getExportEndpoint(
    serviceName: string,
    stage: string,
    workspaceSlug?: string,
  ) {
    const workspaceString = workspaceSlug ? `${workspaceSlug}/` : ''
    return `${this.baseUrl}/${workspaceString}${serviceName}/${stage}/export`
  }

  getDeployEndpoint() {
    return `${this.baseUrl}/cluster`
  }

  async isOnline(): Promise<boolean> {
    const version = await this.getVersion()
    return Boolean(version)
  }

  async getVersion(): Promise<string | null> {
    try {
      const result = await fetch(this.getDeployEndpoint(), {
        method: 'post',
        headers: {
          'Content-Type': 'application/json',
        } as any,
        body: JSON.stringify({
          query: `{
            clusterInfo {
              version
            }
          }`,
        }),
      })

      const { data } = await result.json()
      return data.clusterInfo.version
    } catch (e) {
      debug(e)
    }

    return null
  }
}
